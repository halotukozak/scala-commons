package com.avsystem.commons
package misc

import serialization.IsoInstant

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

/** Millisecond-precision, general purpose, cross compiled timestamp
 * representation.
 *
 * @param millis
 * milliseconds since UNIX epoch, UTC
 */
opaque type Timestamp = Long

extension (timestamp: Timestamp)

  inline def millis: Long = timestamp

  def compareTo(o: Timestamp): Int = java.lang.Long.compare(millis, o.millis)

  // I don't want to inherit them from Ordered or something because that would cause boxing
  def <(other: Timestamp): Boolean = millis < other.millis
  def <=(other: Timestamp): Boolean = millis <= other.millis
  def >(other: Timestamp): Boolean = millis > other.millis
  def >=(other: Timestamp): Boolean = millis >= other.millis

  def add(amount: Long, unit: TimeUnit): Timestamp = Timestamp(millis + unit.toMillis(amount))

  def +(duration: FiniteDuration): Timestamp = Timestamp(millis + duration.toMillis)
  def -(duration: FiniteDuration): Timestamp = Timestamp(millis - duration.toMillis)

  /** Computes a [[FiniteDuration]] between this timestamp and some other
   * timestamp later in time.
   */
  def until(end: Timestamp): FiniteDuration = FiniteDuration(end.millis - millis, TimeUnit.MILLISECONDS)

  /** Computes a [[FiniteDuration]] between some timestamp earlier in time and
   * this timestamp.
   */
  def since(start: Timestamp): FiniteDuration = start.until(timestamp)

  /** Computes a difference between two timestamps, expressed as
   * [[FiniteDuration]]. Alias for [[since]].
   */
  def -(start: Timestamp): FiniteDuration = since(start)

  def toString: String = IsoInstant.format(millis)
end extension

object Timestamp {
  final val Zero = Timestamp(0)

  def apply(millis: Long): Timestamp = millis.asInstanceOf

  def unapply(timestamp: Timestamp): Opt[Long] = Opt(timestamp.millis)

  def parse(str: String): Timestamp = Timestamp(IsoInstant.parse(str))

  def now(): Timestamp = Timestamp(System.currentTimeMillis())

  given Ordering[Timestamp] = Ordering.Long
}
