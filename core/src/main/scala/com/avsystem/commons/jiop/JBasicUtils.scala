package com.avsystem.commons
package jiop

import misc.{Sam, Timestamp}

import com.avsystem.commons.JDate

import java.util.Comparator
import java.util.concurrent.Callable
import java.{lang as jl, math as jm, util as ju}

trait JBasicUtils {
  def jRunnable(code: => Any) = Sam[Runnable](code)

  def jCallable[T](expr: => T) = Sam[Callable[T]](expr)

  def jComparator[T](cmp: (T, T) => Int) = Sam[Comparator[T]](cmp)
  
  extension (jDate: JDate)
    def toTimestamp: Timestamp = Timestamp(jDate.getTime)
    def toJsDate: JDate = new JDate(jDate.getTime)
    def toJDate: JDate = new JDate(jDate.getTime)

  end extension

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
