package com.avsystem.commons
package redis

import com.avsystem.commons.redis.RawCommand.Level
import com.avsystem.commons.redis.RedisBatch.Index
import com.avsystem.commons.redis.commands.{Exec, Multi}
import com.avsystem.commons.redis.exception.{OptimisticLockException, RedisException, UnexpectedReplyException}
import com.avsystem.commons.redis.protocol.*

import scala.annotation.tailrec

final class Transaction[+A](batch: RedisBatch[A]) extends SinglePackBatch[A] {

  override def maxBlockingMillis: Int =
    batch.rawCommandPacks.maxBlockingMillis

  def rawCommands(inTransaction: Boolean): RawCommands = new RawCommands {
    def emitCommands(consumer: RawCommand => Unit): Unit = {
      if !inTransaction then {
        consumer(Multi)
      }
      batch.rawCommandPacks.emitCommandPacks(_.rawCommands(inTransaction = true).emitCommands(consumer))
      if !inTransaction then {
        consumer(Exec)
      }
    }
  }

  def checkLevel(minAllowed: Level, clientType: String): Unit =
    batch.rawCommandPacks.emitCommandPacks(
      _.rawCommands(inTransaction = true)
        .emitCommands(_.checkLevel(minAllowed, clientType)),
    )

  def createPreprocessor(replyCount: Int): ReplyPreprocessor = new ReplyPreprocessor {
    private var singleError: Opt[FailureReply] = Opt.Empty
    private var errors: Opt[Array[ErrorMsg]] = Opt.Empty
    private var normalResult: Opt[IndexedSeq[RedisMsg]] = Opt.Empty
    private var ctr = 0

    private def setSingleError(exception: => RedisException): Unit =
      if singleError.isEmpty then {
        singleError = FailureReply(exception).opt
      }

    @tailrec private def errorsBuffer: Array[ErrorMsg] =
      errors match {
        case Opt(arr) => arr
        case Opt.Empty =>
          errors = Array.fill[ErrorMsg](replyCount - 2)(null).opt
          errorsBuffer
      }

    private def setDefaultError(fillWith: ErrorMsg): Unit = {
      val buf = errorsBuffer
      var i = 0
      while i < buf.length do {
        if buf(i) == null then {
          buf(i) = fillWith
        }
        i += 1
      }
    }

    def preprocess(message: RedisMsg, state: WatchState): Opt[RedisReply] = {
      val LastIndex = replyCount - 1
      val c = ctr
      ctr += 1
      c match {
        case 0 =>
          message match {
            case RedisMsg.Ok =>
            case _ => setSingleError(new UnexpectedReplyException(s"Unexpected reply for MULTI: $message"))
          }
          Opt.Empty
        case LastIndex =>
          Exec.updateWatchState(message, state)
          message match {
            case ArrayMsg(elements) => normalResult = elements.opt
            case NullArrayMsg => setSingleError(new OptimisticLockException)
            case errorMsg: ErrorMsg => setDefaultError(errorMsg)
            case _ => setSingleError(new UnexpectedReplyException(s"Unexpected reply for EXEC: $message"))
          }
          singleError orElse
            errors.map(a => TransactionReply(IArraySeq.unsafeWrapArray(a))) orElse
            normalResult.map(TransactionReply.apply)
        case i =>
          message match {
            case RedisMsg.Queued =>
            case errorMsg: ErrorMsg =>
              errorsBuffer(i - 1) = errorMsg
            case _ =>
              setSingleError(new UnexpectedReplyException(s"Unexpected reply: expected QUEUED, got $message"))
          }
          Opt.Empty
      }
    }
  }

  def decodeReplies(replies: Int => RedisReply, index: Index, inTransaction: Boolean): A =
    if inTransaction then batch.decodeReplies(replies, index, inTransaction)
    else
      replies(index.inc()) match {
        case TransactionReply(elements) =>
          batch.decodeReplies(elements, new Index, inTransaction = true)
        case fr: FailureReply =>
          batch.decodeReplies(_ => fr, new Index, inTransaction = true)
        case msg =>
          val failure = FailureReply(new UnexpectedReplyException(s"Unexpected reply for transaction: $msg"))
          batch.decodeReplies(_ => failure, new Index, inTransaction = true)
      }

  override def transaction: Transaction[A] = this
}
