package com.avsystem.commons
package misc

import com.avsystem.commons.annotation.MayBeReplacedWith

import scala.annotation.implicitNotFound

/**
  * Macro materialized typeclass which captures the single value of a singleton type.
  */
@implicitNotFound("Cannot derive value of ${T} - is not a singleton type")
@MayBeReplacedWith("scala.ValueOf${T}")
class ValueOf[T](val value: T) extends AnyVal
object ValueOf {
  @MayBeReplacedWith("scala.valueOf${T}")
  def apply[T](implicit vof: ValueOf[T]): T = vof.value

  implicit def mkValueOf[T]: ValueOf[T] = ??? // macro MiscMacros.mkValueOf[T]
}
