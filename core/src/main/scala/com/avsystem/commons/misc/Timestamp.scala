package com.avsystem.commons.misc

import com.avsystem.commons.serialization.IsoInstant

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

/**
  * Millisecond-precision, general purpose, cross compiled timestamp representation.
  *
  * @param millis milliseconds since UNIX epoch, UTC
  */
opaque type Timestamp = Long

extension (timestamp: Timestamp) {
  def millis: Long = timestamp

  // I don't want to inherit them from Ordered or something because that would cause boxing
  def <(other: Timestamp): Boolean = millis < other.millis
  def <=(other: Timestamp): Boolean = millis <= other.millis
  def >(other: Timestamp): Boolean = millis > other.millis
  def >=(other: Timestamp): Boolean = millis >= other.millis

  def add(amount: Long, unit: TimeUnit): Timestamp = Timestamp(millis + unit.toMillis(amount))

  def +(duration: FiniteDuration): Timestamp = Timestamp(millis + duration.toMillis)
  def -(duration: FiniteDuration): Timestamp = Timestamp(millis - duration.toMillis)

  /**
    * Computes a [[FiniteDuration]] between this timestamp and some other timestamp later in time.
    */
  def until(end: Timestamp): FiniteDuration =
    FiniteDuration(end.millis - millis, TimeUnit.MILLISECONDS)

  /**
    * Computes a [[FiniteDuration]] between some timestamp earlier in time and this timestamp.
    */
  def since(start: Timestamp): FiniteDuration =
    start.until(timestamp)

  /**
    * Computes a difference between two timestamps, expressed as [[FiniteDuration]]. Alias for [[since]].
    */
  def -(start: Timestamp): FiniteDuration =
    since(start)

  def toString: String = IsoInstant.format(millis)
}
object Timestamp {
  final val Zero = Timestamp(0)

  def apply(millis: Long): Timestamp = millis
  def unapply(timestamp: Timestamp): Opt[Long] = Opt(timestamp.millis)
  def parse(str: String): Timestamp = Timestamp(IsoInstant.parse(str))

  def now(): Timestamp = Timestamp(System.currentTimeMillis())

  given Conversion[Timestamp, TimestampConversions] = TimestampConversions(_)
  given Conversion[Timestamp, Comparable[Timestamp]] = timestamp => other => timestamp.millis.compareTo(other.millis)

  given ordering: Ordering[Timestamp] =
    Ordering.by(_.millis)
}
