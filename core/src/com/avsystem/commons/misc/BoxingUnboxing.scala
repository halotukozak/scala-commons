package com.avsystem.commons
package misc

import annotation.MayBeReplacedWith
import scala.annotation.targetName

opaque type Boxing[-A, +B] <: A => B = A => B

object Boxing extends LowPrioBoxing {
  inline def apply[A, B](inline fun: A => B): Boxing[A, B] = fun

  extension [A, B](boxing: Boxing[A, B])
    @MayBeReplacedWith("_")
    def fun: A => B = boxing

  def fromImplicitConv[A, B](using conv: A => B): Boxing[A, B] = Boxing(conv)

  given BooleanBoxing: Boxing[Boolean, JBoolean] = fromImplicitConv[Boolean, JBoolean]
  given ByteBoxing: Boxing[Byte, JByte] = fromImplicitConv[Byte, JByte]
  given ShortBoxing: Boxing[Short, JShort] = fromImplicitConv[Short, JShort]
  given IntBoxing: Boxing[Int, JInteger] = fromImplicitConv[Int, JInteger]
  given LongBoxing: Boxing[Long, JLong] = fromImplicitConv[Long, JLong]
  given FloatBoxing: Boxing[Float, JFloat] = fromImplicitConv[Float, JFloat]
  given DoubleBoxing: Boxing[Double, JDouble] = fromImplicitConv[Double, JDouble]
}
trait LowPrioBoxing { this: Boxing.type =>
  given nullableBoxing[A >: Null]: Boxing[A, A] = Boxing(identity)
}

opaque type Unboxing[+A, -B] <: B => A = B => A

object Unboxing extends LowPrioUnboxing {

  inline def apply[A, B](inline fun: B => A): Unboxing[A, B] = fun

  extension [A, B](unboxing: Unboxing[A, B])
    @MayBeReplacedWith("_")
    def fun: B => A = unboxing

  def fromImplicitConv[A, B](using conv: B => A): Unboxing[A, B] = Unboxing[A, B](conv)

  given BooleanUnboxing: Unboxing[Boolean, JBoolean] = fromImplicitConv[Boolean, JBoolean]
  given ByteUnboxing: Unboxing[Byte, JByte] = fromImplicitConv[Byte, JByte]
  given ShortUnboxing: Unboxing[Short, JShort] = fromImplicitConv[Short, JShort]
  given IntUnboxing: Unboxing[Int, JInteger] = fromImplicitConv[Int, JInteger]
  given LongUnboxing: Unboxing[Long, JLong] = fromImplicitConv[Long, JLong]
  given FloatUnboxing: Unboxing[Float, JFloat] = fromImplicitConv[Float, JFloat]
  given DoubleUnboxing: Unboxing[Double, JDouble] = fromImplicitConv[Double, JDouble]
}

trait LowPrioUnboxing { this: Unboxing.type =>
  given nullableUnboxing[A >: Null]: Unboxing[A, A] = Unboxing(identity)
}
