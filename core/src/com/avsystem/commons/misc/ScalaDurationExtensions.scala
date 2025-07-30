package com.avsystem.commons
package misc

import scala.concurrent.duration.{DoubleMult, DurationDouble, DurationInt, DurationLong, IntMult, LongMult}

/** Gathers all extensions from [[scala.concurrent.duration]] into one trait that can be mixed in with package object.
  */
trait ScalaDurationExtensions {
  given durationIntOps: Conversion[Int, DurationInt] = new DurationInt(_)
  given durationLongOps: Conversion[Long, DurationLong] = new DurationLong(_)
  given durationDoubleOps: Conversion[Double, DurationDouble] = new DurationDouble(_)
  given durationIntMulOps: Conversion[Int, IntMult] = new IntMult(_)
  given durationLongMulOps: Conversion[Long, LongMult] = new LongMult(_)
  given durationDoubleMulOps: Conversion[Double, DoubleMult] = new DoubleMult(_)
}
object ScalaDurationExtensions extends ScalaDurationExtensions
