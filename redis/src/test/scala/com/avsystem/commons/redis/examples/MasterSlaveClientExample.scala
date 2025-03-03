package com.avsystem.commons
package redis.examples

import org.apache.pekko.actor.ActorSystem
import com.avsystem.commons.redis.*

// Global execution context is used for the sake of simplicity of this example,
// think well if this is what you actually want.
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Basic example showing how to execute simple command on [[RedisMasterSlaveClient]].
 */
object MasterSlaveClientExample extends App {
  // The driver is implemented using Pekko IO, so we need actor system
  implicit val actorSystem: ActorSystem = ActorSystem()
  // The client is the object that actually talks to Redis, but does not expose Redis API
  // Assuming Redis Sentinel is working locally on its default port, 26379
  val client = new RedisMasterSlaveClient("mymaster")
  // API object exposes API to access individual Redis commands. The API variant we're using here is:
  // - node level: commands specific to single Redis connection are excluded (e.g. CLIENT SETNAME)
  // - asynchronous: API object uses the client to actually execute commands and returns results as Futures
  // - stringly-typed: Redis keys, hash keys and values used in method signatures are typed as Strings
  val api = RedisApi.Node.Async.StringTyped(client)

  // execute GET method and register callback
  api.get("key").onComplete {
    case Success(Opt(value)) => println(s"Got value $value")
    case Success(Opt.Empty) => println(s"Got no value")
    case Failure(t) => t.printStackTrace()
  }
}
