package com.avsystem.commons
package redis.actor

import org.apache.pekko.actor.{Actor, ActorRef, Props}
import com.avsystem.commons.redis.NodeAddress
import com.avsystem.commons.redis.actor.ConnectionPoolActor.*
import com.avsystem.commons.redis.config.{ConnectionConfig, NodeConfig}
import com.avsystem.commons.redis.exception.RedisException
import com.typesafe.scalalogging.LazyLogging

import scala.annotation.tailrec

class ConnectionPoolActor(address: NodeAddress, config: NodeConfig, queue: JDeque[QueuedConn])
    extends Actor
    with LazyLogging {

  import context.*

  private val connections = new MHashSet[ActorRef]

  if config.maxBlockingIdleTime.isFinite then {
    val interval = config.blockingCleanupInterval
    system.scheduler.scheduleWithFixedDelay(interval, interval, self, Cleanup)
  }

  def receive: Receive = {
    case CreateNewConnection if connections.size < config.maxBlockingPoolSize =>
      logger.info(s"Creating new blocking connection to $address")
      val connConfig: ConnectionConfig = config.blockingConnectionConfigs(connections.size)
      val props = Props(new RedisConnectionActor(address, connConfig))
      val connection = connConfig.actorName.fold(actorOf(props))(actorOf(props, _))
      connections += connection
      connection ! RedisConnectionActor.Open(mustInitiallyConnect = false, Promise[Unit]())
      sender() ! NewConnection(connection)
    case CreateNewConnection =>
      sender() ! Full
    case Cleanup =>
      cleanup(System.nanoTime(), config.maxBlockingIdleTime.toNanos)
    case Close(cause, stopSelf) =>
      connections.foreach(_ ! RedisConnectionActor.Close(cause, stopSelf))
      if stopSelf then {
        stop(self)
      }
  }

  private def cleanup(nowNanos: Long, maxIdleNanos: Long): Unit = {
    @tailrec def loop(dequeue: Boolean): Unit = {
      val last = if dequeue then queue.pollLast() else queue.peekLast()
      last match {
        case QueuedConn(conn, enqueuedAt) =>
          val stale = (nowNanos - enqueuedAt) > maxIdleNanos
          if !dequeue && stale then {
            loop(dequeue = true)
          } else if dequeue && stale then {
            conn ! RedisConnectionActor.Close(new RedisException("Idle blocking connection closed"), stop = true)
            context.stop(conn)
            connections.remove(conn)
            loop(dequeue = false)
          } else if dequeue && !stale then {
            // unlikely situation where we dequeued something else than we peeked before
            queue.addLast(last)
          }
        case null =>
      }
    }
    loop(dequeue = false)
  }
}
object ConnectionPoolActor {
  final case class QueuedConn(conn: ActorRef, enqueuedAt: Long)

  object CreateNewConnection
  final case class Close(cause: Throwable, stop: Boolean)
  object Cleanup

  final case class NewConnection(connection: ActorRef)
  object Full
}
