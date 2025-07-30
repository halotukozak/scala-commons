package com.avsystem.commons
package concurrent

import scala.concurrent.duration._

trait DurationPostfixConverters {

  given durationInt: Conversion[Int, DurationInt] = new DurationInt(_)
  given durationLong: Conversion[Long, DurationLong] = new DurationLong(_)
  given durationDouble: Conversion[Double, DurationDouble] = new DurationDouble(_)
}
