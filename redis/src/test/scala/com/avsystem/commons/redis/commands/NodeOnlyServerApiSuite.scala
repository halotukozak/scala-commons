package com.avsystem.commons
package redis.commands

import com.avsystem.commons.redis.RedisNodeCommandsSuite

import scala.concurrent.duration.*

class NodeOnlyServerApiSuite extends RedisNodeCommandsSuite {

  // ignored because of spurious failures
  /*
  import RedisApi.Batches.StringTyped._

  apiTest("CLIENT KILL") {
    val clients: Seq[ClientInfo] = waitFor(clientList.exec)(_.size >= 3, 100.millis).futureValue
    clientKill(clients.head.addr).get
    clientKill(clients(1).addr, Skipme(false)).assertEquals(1)
    clientKill(clients(2).id, Skipme(false)).assertEquals(1)
    clientKill(ClientType.Master).assertEquals(0)
  }
   */
}
