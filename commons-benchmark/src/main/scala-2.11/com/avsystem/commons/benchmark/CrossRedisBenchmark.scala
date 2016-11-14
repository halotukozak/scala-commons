package com.avsystem.commons
package benchmark

import _root_.redis._
import com.avsystem.commons.redis.RedisClientBenchmark
import com.avsystem.commons.redis.RedisClientBenchmark._
import org.openjdk.jmh.annotations.Benchmark

import scala.concurrent.Future

trait CrossRedisBenchmark { this: RedisClientBenchmark =>
  lazy val rediscalaClient = RedisClient()
  lazy val rediscalaClientPool = RedisClientPool(Seq.fill(PoolSize)(RedisServer()))

  lazy val scredisClient = scredis.Redis.withActorSystem()

  def rediscalaCommandFuture(client: RedisCommands, i: Int): Future[Any] =
    client.set(s"$KeyBase$i", "v")

  def scredisCommandFuture(client: scredis.Redis, i: Int): Future[Any] =
    client.set(s"$KeyBase$i", "v")

  def scredisOperationFuture(client: scredis.Redis, i: Int): Future[Any] = {
    client.withTransaction { b =>
      for (j <- 0 until (BatchSize - 1)) {
        b.set(s"$KeyBase$i$j", "v")
      }
      b.set(s"$KeyBase$i${BatchSize - 1}", "v")
    }
  }

  @Benchmark
  def scredisCommandBenchmark() =
    redisClientBenchmark(ConcurrentCommands, scredisCommandFuture(scredisClient, _))

  @Benchmark
  def scredisOpBenchmark() =
    redisClientBenchmark(ConcurrentBatches, scredisOperationFuture(scredisClient, _))

  @Benchmark
  def rediscalaCommandBenchmark() =
    redisClientBenchmark(ConcurrentCommands, rediscalaCommandFuture(rediscalaClient, _))

  @Benchmark
  def rediscalaPoolCommandBenchmark() =
    redisClientBenchmark(ConcurrentCommands, rediscalaCommandFuture(rediscalaClientPool, _))
}
