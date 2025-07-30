package com.avsystem.commons
package jiop

import com.avsystem.commons.annotation.MayBeReplacedWith

import java.util.Comparator
import java.util.concurrent.Callable
import java.{lang as jl, math as jm, util as ju}
import com.avsystem.commons.misc.{Sam, TimestampConversions}

trait JBasicUtils {
  @MayBeReplacedWith("() => code)")
  def jRunnable(code: => Any): Runnable = Sam[Runnable](code)
  @MayBeReplacedWith("() => expr)")
  def jCallable[T](expr: => T): Callable[T] = Sam[Callable[T]](expr)
  @MayBeReplacedWith("cmp(_,_)")
  def jComparator[T](cmp: (T, T) => Int): Comparator[T] = Sam[Comparator[T]](cmp)

  given Conversion[JDate, TimestampConversions] = date => TimestampConversions(date.getTime)

  type JByte = jl.Byte
  type JShort = jl.Short
  type JInteger = jl.Integer
  type JLong = jl.Long
  type JFloat = jl.Float
  type JDouble = jl.Double
  type JBoolean = jl.Boolean
  type JCharacter = jl.Character
  type JBigInteger = jm.BigInteger
  type JBigDecimal = jm.BigDecimal
  type JDate = ju.Date
  type JNumber = jl.Number
  type JVoid = jl.Void
  type JEnum[E <: jl.Enum[E]] = jl.Enum[E]
  type JStringBuilder = jl.StringBuilder
}
