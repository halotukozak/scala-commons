package com.avsystem.commons
package redis.actor

import org.apache.pekko.actor.{Actor, ActorRef, Cancellable, Props}
import com.avsystem.commons.redis.*
import com.avsystem.commons.redis.actor.RedisConnectionActor.PacksResult
import com.avsystem.commons.redis.commands.{NodeInfo, SlotRange, SlotRangeMapping}
import com.avsystem.commons.redis.config.ClusterConfig
import com.avsystem.commons.redis.exception.{ClusterInitializationException, ErrorReplyException}
import com.avsystem.commons.redis.monitoring.ClusterStateObserver
import com.avsystem.commons.redis.util.{ActorLazyLogging, SingletonSeq}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.*
import scala.util.Random

final class ClusterMonitoringActor(
  seedNodes: Seq[NodeAddress],
  config: ClusterConfig,
  onClusterInitFailure: Throwable => Any,
  onNewClusterState: ClusterState => Any,
  onTemporaryClient: RedisNodeClient => Any,
  clusterStateObserver: OptArg[ClusterStateObserver] = OptArg.Empty,
) extends Actor
    with ActorLazyLogging {

  import ClusterMonitoringActor.*
  import context.*

  private def createConnection(addr: NodeAddress): ActorRef =
    actorOf(Props(new RedisConnectionActor(addr, config.monitoringConnectionConfigs(addr), clusterStateObserver)))

  private def getConnection(addr: NodeAddress, seed: Boolean): ActorRef =
    connections.getOrElse(addr, openConnection(addr, seed))

  private def openConnection(addr: NodeAddress, seed: Boolean): ActorRef = {
    val connection = connections.getOrElseUpdate(addr, createConnection(addr))
    connection ! RedisConnectionActor.Open(seed, Promise[Unit]())
    connection
  }

  private def createClient(addr: NodeAddress, clusterNode: Boolean = true) =
    new RedisNodeClient(addr, config.nodeConfigs(addr), clusterNode)

  private val random = new Random
  private var masters = MLinkedHashSet.empty[NodeAddress]
  private val connections = new MHashMap[NodeAddress, ActorRef]
  private val clients = new MHashMap[NodeAddress, RedisNodeClient]
  private var state = Opt.empty[ClusterState]
  private var lastEpoch: Long = 0
  private var suspendUntil = Deadline(Duration.Zero)
  private var fallbackToSeedsAfter = Deadline(Duration.Zero)
  private var scheduledRefresh = Opt.empty[Cancellable]
  private val seedFailures = new ArrayBuffer[Throwable]

  self ! Refresh(Opt.Empty)

  private def randomMasters(): Seq[NodeAddress] = {
    val pool = masters.toArray
    val count = config.nodesToQueryForState(pool.length)
    var i = 0
    while i < count do {
      val idx = i + random.nextInt(pool.length - i)
      val node = pool(idx)
      pool(idx) = pool(i)
      pool(i) = node
      i += 1
    }
    IArraySeq.unsafeWrapArray(pool.slice(0, count))
  }

  def receive: Receive = {
    case Refresh(nodeOpt) =>
      if suspendUntil.isOverdue() then {
        val addresses = nodeOpt.map(new SingletonSeq(_)).getOrElse {
          if fallbackToSeedsAfter.isOverdue() then {
            if state.isDefined then {
              lastEpoch = 0
              log.warning(s"Could not fetch cluster state from current masters, using seed nodes")
            }
            seedNodes ++ randomMasters().filterNot(seedNodes.contains)
          } else randomMasters()
        }
        log.debug(s"Asking ${addresses.mkString(",")} for cluster state")
        addresses.foreach { node =>
          getConnection(node, state.isEmpty) ! StateRefresh
        }
        suspendUntil = config.minRefreshInterval.fromNow
      }

    case pr: PacksResult =>
      Try(StateRefresh.decodeReplies(pr)) match {
        case Success((slotRangeMapping, NodeInfosWithMyself(nodeInfos, thisNodeInfo)))
            if thisNodeInfo.configEpoch >= lastEpoch =>
          clusterStateObserver.foreach(_.onClusterRefresh())

          lastEpoch = thisNodeInfo.configEpoch

          val newMapping = {
            val res = slotRangeMapping.iterator.map { srm =>
              (srm.range, clients.getOrElseUpdate(srm.master, createClient(srm.master)))
            }.toArray
            java.util.Arrays.sort(res, MappingComparator)
            IArraySeq.unsafeWrapArray(res)
          }

          masters = nodeInfos.iterator
            .filter(n => n.flags.master && !n.flags.fail)
            .map(_.address)
            .to(mutable.LinkedHashSet)
          masters.foreach { addr =>
            openConnection(addr, seed = false)
          }

          val mappedMasters = slotRangeMapping.iterator.map(_.master).to(mutable.LinkedHashSet)

          if state.forall(_.mapping != newMapping) then {
            log.info(s"New cluster slot mapping received:\n${slotRangeMapping.mkString("\n")}")
            val newState = ClusterState(newMapping, mappedMasters.iterator.map(m => (m, clients(m))).toMap)
            state = newState.opt
            onNewClusterState(newState)
          }

          if scheduledRefresh.isEmpty then {
            val refreshInterval = config.autoRefreshInterval
            scheduledRefresh =
              system.scheduler.scheduleWithFixedDelay(refreshInterval, refreshInterval, self, Refresh(Opt.Empty)).opt
          }
          fallbackToSeedsAfter = config.refreshUsingSeedNodesAfter.fromNow

          (connections.keySet diff masters).foreach { addr =>
            connections.remove(addr).foreach(context.stop)
          }
          (clients.keySet diff mappedMasters).foreach { addr =>
            clients.remove(addr).foreach { client =>
              client.nodeRemoved()
              context.system.scheduler.scheduleOnce(config.nodeClientCloseDelay)(client.close())
            }
          }

        case Success(_) =>
        // obsolete cluster state, ignore

        case Failure(err: ErrorReplyException)
            if state.isEmpty && seedNodes.size == 1 && config.fallbackToSingleNode &&
              err.errorStr == "ERR This instance has cluster support disabled" =>
          val addr = seedNodes.head
          log.info(s"$addr is a non-clustered node, falling back to regular node client")

          val client = clients.getOrElseUpdate(addr, createClient(addr, clusterNode = false))
          val newState = ClusterState.nonClustered(client)
          state = newState.opt
          onNewClusterState(newState)

          // we don't need monitoring connection for non-clustered node
          connections.values.foreach(context.stop)
          connections.clear()

        case Failure(cause) =>
          log.error("Failed to refresh cluster state", cause)
          clusterStateObserver.foreach(_.onClusterRefreshFailure())
          if state.isEmpty then {
            seedFailures += cause
            if seedFailures.size == seedNodes.size then {
              val failure = new ClusterInitializationException(seedNodes)
              seedFailures.foreach(failure.addSuppressed)
              onClusterInitFailure(failure)
            }
          }
      }

    case GetClient(addr) =>
      val client = clients.getOrElseUpdate(
        addr, {
          val tempClient = createClient(addr)
          onTemporaryClient(tempClient)
          tempClient
        },
      )
      sender() ! GetClientResponse(client)
  }

  override def postStop(): Unit = {
    scheduledRefresh.foreach(_.cancel())
    clients.values.foreach(_.close())
  }
}

object ClusterMonitoringActor {
  val StateRefresh: RedisBatch[(Seq[SlotRangeMapping], Seq[NodeInfo])] = {
    val api = RedisApi.Batches.BinaryTyped
    api.clusterSlots zip api.clusterNodes
  }
  val MappingComparator: Ordering[(SlotRange, RedisNodeClient)] =
    Ordering.by[(SlotRange, RedisNodeClient), Int](_._1.start)

  final case class Refresh(node: Opt[NodeAddress])
  final case class GetClient(addr: NodeAddress)
  final case class GetClientResponse(client: RedisNodeClient)

  private object NodeInfosWithMyself {
    def unapply(nodeInfos: Seq[NodeInfo]): Opt[(Seq[NodeInfo], NodeInfo)] =
      nodeInfos.findOpt(_.flags.myself).map(tni => (nodeInfos, tni))
  }
}
