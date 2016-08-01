package com.avsystem.commons
package redis

import akka.util.{ByteString, ByteStringBuilder}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Author: ghik
  * Created: 14/04/16.
  */
trait ByteStringInterpolation {
  implicit class bsInterpolation(sc: StringContext) {
    def bs = this

    def apply(): ByteString = {
      val bsb = new ByteStringBuilder
      sc.parts.foreach(p => bsb.append(ByteString(p)))
      bsb.result()
    }
  }
}

trait CommandsSuite extends FunSuite with ScalaFutures with BeforeAndAfterAll with ByteStringInterpolation {
  type Api <: ApiSubset

  def executor: RedisExecutor[Api#CmdScope]
  def setupCommands: RedisBatch[Any, Api#CmdScope] = RedisBatch.success(())
  val commands: Api {type Result[+A, -S] = Future[A]}
}

trait RedisNodeCommandsSuite extends FunSuite with UsesRedisNodeClient with CommandsSuite {
  type Api = RedisNodeAsyncCommands
  def executor = redisClient.toExecutor
  lazy val commands = RedisNodeAsyncCommands(executor)

  override def nodeConfig = super.nodeConfig.copy(initOp = setupCommands.operation)

  override protected def afterAll() = {
    Await.result(commands.flushall, Duration.Inf)
    super.afterAll()
  }
}

trait RedisConnectionCommandsSuite extends FunSuite with UsesRedisConnectionClient with CommandsSuite {
  type Api = RedisConnectionAsyncCommands
  def executor = redisClient.toExecutor
  lazy val commands = RedisConnectionAsyncCommands(executor)

  override def connectionConfig = super.connectionConfig.copy(initCommands = setupCommands)

  override protected def afterAll() = {
    Await.result(commands.flushall, Duration.Inf)
    super.afterAll()
  }
}