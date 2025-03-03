package com.avsystem.commons
package redis.protocol

import org.apache.pekko.util.{ByteString, ByteStringBuilder}
import com.avsystem.commons.misc.Sam
import com.avsystem.commons.redis.exception.{InvalidDataException, RedisException}
import com.avsystem.commons.redis.util.SizedArraySeqBuilder

import java.nio.ByteBuffer
import scala.annotation.tailrec
import scala.collection.immutable.VectorBuilder

/**
 * Raw result of executing a single [[com.avsystem.commons.redis.RawCommandPack]]. It may be a Redis protocol message
 * ([[RedisMsg]]) or an object that aggregates transaction results or an object that indicates failure.
 */
sealed trait RedisReply
final case class TransactionReply(elements: IndexedSeq[RedisMsg]) extends RedisReply
trait FailureReply extends RedisReply {
  def exception: RedisException
}
object FailureReply {
  def apply(createException: => RedisException): FailureReply =
    Sam[FailureReply](createException)
}

/**
 * Redis protocol message. It can be sent over network from or to Redis instance.
 */
sealed trait RedisMsg extends RedisReply
sealed trait ValidRedisMsg extends RedisMsg
case class SimpleStringMsg(string: ByteString) extends ValidRedisMsg {
  override def toString = s"$productPrefix(${RedisMsg.escape(string)})"
}
object SimpleStringMsg {
  def apply(str: String): SimpleStringMsg = SimpleStringMsg(ByteString(str))
}
final case class ErrorMsg(errorString: ByteString) extends RedisMsg {
  override def toString = s"$productPrefix(${RedisMsg.escape(errorString)})"
  lazy val errorCode: String = errorString.indexOf(' '.toByte) match {
    case -1 => errorString.utf8String
    case i => errorString.slice(0, i).utf8String
  }
}
object ErrorMsg {
  def apply(str: String): ErrorMsg = ErrorMsg(ByteString(str))
}
final case class IntegerMsg(value: Long) extends ValidRedisMsg
case object NullBulkStringMsg extends ValidRedisMsg
sealed case class BulkStringMsg(string: ByteString) extends ValidRedisMsg {
  override def toString: String = s"$productPrefix(${RedisMsg.escape(string)})"
  def isCommandKey: Boolean = false
}
final class CommandKeyMsg(key: ByteString) extends BulkStringMsg(key) {
  override def isCommandKey: Boolean = true
}
object CommandKeyMsg {
  def apply(key: ByteString): CommandKeyMsg = new CommandKeyMsg(key)
  def unapply(keyBulkStringMsg: CommandKeyMsg): Opt[ByteString] = Opt(keyBulkStringMsg.string)
}
case object NullArrayMsg extends ValidRedisMsg
final case class ArrayMsg[+E <: RedisMsg](elements: IndexedSeq[E]) extends ValidRedisMsg
object ArrayMsg {
  final val Empty = ArrayMsg(IndexedSeq.empty)
}

object SimpleStringStr {
  def unapply(ss: SimpleStringMsg): Opt[String] =
    Opt(ss.string.utf8String)
}

object RedisMsg {
  final val Ok = SimpleStringMsg(ByteString("OK"))
  final val Queued = SimpleStringMsg(ByteString("QUEUED"))
  final val Nokey = SimpleStringMsg(ByteString("NOKEY"))

  def escape(bs: ByteString, quote: Boolean = true): String = {
    val sb = new StringBuilder(if quote then "\"" else "")
    bs.foreach {
      case '\t' => sb ++= "\\r"
      case '\b' => sb ++= "\\b"
      case '\n' => sb ++= "\\n"
      case '\r' => sb ++= "\\r"
      case '\f' => sb ++= "\\f"
      case '\'' => sb ++= "\\'"
      case '\"' => sb ++= "\\"
      case '\\' => sb ++= "\\\\"
      case b if b > 0x1f && b < 0x7f => sb += b.toChar
      case b => sb ++= f"\\x$b%02x"
    }
    if quote then {
      sb += '\"'
    }
    sb.result()
  }

  private final val CRLF = ByteString("\r\n")
  private final val NullBulk = ByteString("$-1\r\n")
  private final val NullArray = ByteString("*-1\r\n")

  private final val CRByte: Byte = '\r'
  private final val LFByte: Byte = '\n'
  private final val SimpleInd: Byte = '+'
  private final val ErrorInd: Byte = '-'
  private final val IntegerInd: Byte = ':'
  private final val BulkInd: Byte = '$'
  private final val ArrayInd: Byte = '*'

  private final val LongMinValue = ByteString(Long.MinValue.toString)

  def encodedSize(msg: RedisMsg): Int = {
    def integerSize(value: Long): Int = value match {
      case 0 => 1
      case Long.MinValue => LongMinValue.size
      case v if v < 0 => integerSize(-v) + 1
      case v =>
        @tailrec def posIntegerSize(v: Long, acc: Int): Int =
          if v == 0 then acc
          else posIntegerSize(v / 10, acc + 1)
        posIntegerSize(v, 0)
    }

    msg match {
      case NullBulkStringMsg | NullArrayMsg => 5
      case SimpleStringMsg(data) => data.size + 3
      case ErrorMsg(data) => data.size + 3
      case IntegerMsg(value) => integerSize(value) + 3
      case BulkStringMsg(data) => integerSize(data.size) + data.size + 5
      case ArrayMsg(data) => integerSize(data.size) + data.foldLeft(0)((acc, msg) => acc + encodedSize(msg)) + 3
    }
  }

  def encode(msg: RedisMsg): ByteString = {
    val builder = new ByteStringBuilder
    encode(msg, builder)
    builder.result()
  }

  def encode(msgs: IterableOnce[RedisMsg]): ByteString = {
    val builder = new ByteStringBuilder
    msgs.iterator.foreach(encode(_, builder))
    builder.result()
  }

  @tailrec def encodeInteger(value: Long, bsb: ByteStringBuilder): Unit = value match {
    case 0 => bsb.putByte('0')
    case Long.MinValue => bsb.append(LongMinValue)
    case v if v < 0 => bsb.putByte('-'); encodeInteger(-v, bsb)
    case v =>
      @tailrec def encodePosInteger(value: Long, pow: Long): Unit =
        if pow > 0 then {
          bsb.putByte(('0' + (value / pow)).toByte)
          encodePosInteger(value % pow, pow / 10)
        }
      @tailrec def maxPow10(value: Long, pow: Long): Long =
        if value < 10 then pow else maxPow10(value / 10, pow * 10)
      encodePosInteger(v, maxPow10(v, 1))
  }

  private implicit class ByteStringBuilderOps(private val bsb: ByteStringBuilder) extends AnyVal {
    def append(value: Long): ByteStringBuilder = {
      encodeInteger(value, bsb)
      bsb
    }
  }

  def encode(msg: RedisMsg, builder: ByteStringBuilder): Unit = {
    def encodeIn(msg: RedisMsg): Unit = msg match {
      case SimpleStringMsg(string) =>
        builder.putByte(SimpleInd).append(string).append(CRLF)
      case ErrorMsg(errorString) =>
        builder.putByte(ErrorInd).append(errorString).append(CRLF)
      case IntegerMsg(value: Long) =>
        builder.putByte(IntegerInd).append(value).append(CRLF)
      case NullBulkStringMsg =>
        builder.append(NullBulk)
      case BulkStringMsg(string) =>
        builder.putByte(BulkInd).append(string.size).append(CRLF).append(string).append(CRLF)
      case NullArrayMsg =>
        builder.append(NullArray)
      case ArrayMsg(elements) =>
        builder.putByte(ArrayInd).append(elements.size).append(CRLF)
        elements.foreach(encodeIn)
    }
    encodeIn(msg)
  }

  @tailrec def encodeInteger(value: Long, bb: ByteBuffer): Unit = value match {
    case 0 => bb.put('0': Byte)
    case Long.MinValue => LongMinValue.copyToBuffer(bb)
    case v if v < 0 => bb.put('-': Byte); encodeInteger(-v, bb)
    case v =>
      @tailrec def encodePosInteger(value: Long, pow: Long): Unit =
        if pow > 0 then {
          bb.put(('0' + (value / pow)).toByte)
          encodePosInteger(value % pow, pow / 10)
        }
      @tailrec def maxPow10(value: Long, pow: Long): Long =
        if value < 10 then pow else maxPow10(value / 10, pow * 10)
      encodePosInteger(v, maxPow10(v, 1))
  }

  private implicit class ByteBufferOps(private val bb: ByteBuffer) extends AnyVal {
    def putNum(value: Long): ByteBuffer = {
      encodeInteger(value, bb)
      bb
    }

    def put(bs: ByteString): ByteBuffer = {
      bs.copyToBuffer(bb)
      bb
    }
  }

  private final val CRLFBytes = "\r\n".getBytes
  private final val NullBulkBytes = "$-1\r\n".getBytes
  private final val NullArrayBytes = "*-1\r\n".getBytes

  def encode(msg: RedisMsg, buffer: ByteBuffer): Unit = {
    def encodeIn(msg: RedisMsg): Unit = msg match {
      case SimpleStringMsg(string) =>
        buffer.put(SimpleInd).put(string).put(CRLFBytes)
      case ErrorMsg(errorString) =>
        buffer.put(ErrorInd).put(errorString).put(CRLFBytes)
      case IntegerMsg(value: Long) =>
        buffer.put(IntegerInd).putNum(value).put(CRLFBytes)
      case NullBulkStringMsg =>
        buffer.put(NullBulkBytes)
      case BulkStringMsg(string) =>
        buffer.put(BulkInd).putNum(string.size).put(CRLFBytes).put(string).put(CRLFBytes)
      case NullArrayMsg =>
        buffer.put(NullArrayBytes)
      case ArrayMsg(elements) =>
        buffer.put(ArrayInd).putNum(elements.size).put(CRLFBytes)
        elements.foreach(encodeIn)
    }
    encodeIn(msg)
  }

  def decode(bs: ByteString): Seq[RedisMsg] = {
    val builder = new VectorBuilder[RedisMsg]
    val decoder = new Decoder
    decoder.decodeMore(bs)(builder += _)
    builder.result()
  }

  object Decoder {
    private final val Initial = 0
    private final val ReadingSimple = 1
    private final val CREncountered = 2
    private final val StartingInt = 3
    private final val ReadingInt = 4
    private final val ReadingBulk = 5

    private final val ZeroDigitByte: Byte = '0'
    private final val NineDigitByte: Byte = '9'
    private final val MinusByte: Byte = '-'

    private class Digit(private val b: Byte) extends AnyVal {
      def isEmpty: Boolean = b < ZeroDigitByte || b > NineDigitByte
      def get: Long = b - ZeroDigitByte
    }
    private object Digit {
      def unapply(b: Byte): Digit = new Digit(b)
    }
  }

  final class Decoder {

    import Decoder.*

    private[this] var arrayStack: List[SizedArraySeqBuilder[RedisMsg]] = Nil
    private[this] var state: Int = Initial
    private[this] var currentType: Byte = 0
    private[this] var readingLength: Boolean = false
    private[this] var numberNegative: Boolean = false
    private[this] var numberValue: Long = 0
    private[this] val dataBuilder = new ByteStringBuilder

    def fail(msg: String) = throw new InvalidDataException(msg)

    def decodeMore(bytes: ByteString)(consumer: RedisMsg => Unit): Unit = {
      @tailrec def completed(msg: RedisMsg): Unit = {
        arrayStack match {
          case Nil => consumer(msg)
          case builder :: tail =>
            builder += msg
            if builder.complete then {
              arrayStack = tail
              completed(ArrayMsg(builder.result()))
            }
        }
      }

      @tailrec def decode(idx: Int, prevDataStart: Int): Unit = if idx < bytes.length then {
        val byte = bytes(idx)
        var dataStart = prevDataStart
        state match {
          case Initial =>
            currentType = byte
            byte match {
              case SimpleInd | ErrorInd =>
                state = ReadingSimple
              case IntegerInd =>
                state = StartingInt
              case BulkInd | ArrayInd =>
                state = StartingInt
                readingLength = true
              case _ => fail("Expected one of: '+', '-', ':', '$', '*'")
            }
          case StartingInt =>
            numberValue = 0
            state = ReadingInt
            byte match {
              case MinusByte =>
                numberNegative = true
              case Digit(digitValue) =>
                numberValue = digitValue
              case _ => fail("Expected '-' sign or digit")
            }
          case ReadingInt =>
            byte match {
              case CRByte =>
                numberNegative = false
                state = CREncountered
              case Digit(digitValue) =>
                numberValue = numberValue * 10 + (if numberNegative then -digitValue else digitValue)
              case _ => fail("Expected digit or CR")
            }
          case ReadingSimple =>
            if dataStart < 0 then {
              dataStart = idx
            }
            byte match {
              case CRByte =>
                dataBuilder.append(bytes.slice(dataStart, idx))
                dataStart = -1
                state = CREncountered
              case LFByte => fail("LF not allowed in simple string message")
              case _ =>
            }
          case ReadingBulk =>
            if dataStart < 0 then {
              dataStart = idx
            }
            if dataBuilder.length + idx - dataStart == numberValue then {
              if byte == CRByte then {
                dataBuilder.append(bytes.slice(dataStart, idx))
                dataStart = -1
                state = CREncountered
              } else fail("Expected CR at the end of bulk string message")
            }
          case CREncountered =>
            byte match {
              case LFByte if readingLength =>
                readingLength = false
                currentType match {
                  case BulkInd =>
                    numberValue match {
                      case -1 =>
                        state = Initial
                        completed(NullBulkStringMsg)
                      case size if size >= 0 =>
                        state = ReadingBulk
                      case _ => fail("Invalid bulk string length")
                    }
                  case ArrayInd =>
                    state = Initial
                    numberValue match {
                      case -1 => completed(NullArrayMsg)
                      case 0 => completed(ArrayMsg.Empty)
                      case size if size > 0 =>
                        val is = size.toInt
                        arrayStack = new SizedArraySeqBuilder[RedisMsg](is) :: arrayStack
                      case _ => fail("Invalid array size")
                    }
                  case _ => fail("Length can be read only for bulk strings or arrays")
                }
              case LFByte =>
                def extractData() = {
                  val res = dataBuilder.result()
                  dataBuilder.clear()
                  res
                }
                val msg = currentType match {
                  case SimpleInd => SimpleStringMsg(extractData())
                  case ErrorInd => ErrorMsg(extractData())
                  case BulkInd => BulkStringMsg(extractData())
                  case IntegerInd => IntegerMsg(numberValue)
                }
                completed(msg)
                state = Initial
              case _ => fail("Expected LF after CR")
            }
        }
        decode(idx + 1, dataStart)
      } else
        state match {
          case ReadingSimple | ReadingBulk if prevDataStart >= 0 =>
            dataBuilder.append(bytes.drop(prevDataStart))
          case _ =>
        }
      decode(0, -1)
    }
  }
}
