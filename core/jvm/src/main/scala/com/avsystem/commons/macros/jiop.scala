package com.avsystem.commons
package macros

import jiop.*

import scala.compiletime.summonInline
import scala.quoted.{Expr, Quotes, Type}

inline def specializedOptionAsJava[T]: AsJava[Option[T], ReturnOfOptionalAsJava[T]] = ${
  specialized[T, [U] =>> AsJava[Option[U], ReturnOfOptionalAsJava[U]]](
    '{ specializedOptionAsJavaInt },
    '{ specializedOptionAsJavaDouble },
    '{ specializedOptionAsJavaLong },
    '{ specializedOptionAsJavaAny[T] },
  )
}

private val specializedOptionAsJavaInt: AsJava[Option[Int], JOptionalInt] = _.toJOptionalInt
private val specializedOptionAsJavaLong: AsJava[Option[Long], JOptionalLong] = _.toJOptionalLong
private val specializedOptionAsJavaDouble: AsJava[Option[Double], JOptionalDouble] = _.toJOptionalDouble
private def specializedOptionAsJavaAny[T]: AsJava[Option[T], ReturnOfOptionalAsJava[T]] = _.toJOptional.asInstanceOf[ReturnOfOptionalAsJava[T]]


inline def specializedJStreamAsScala[T]: AsScala[SpecializedJStream[T] | JStream[T], SpecializedScalaJStream[T]] = ${
  specialized[T, [U] =>> AsScala[SpecializedJStream[T] | JStream[T], SpecializedScalaJStream[T]]](
    '{ specializedJStreamAsScalaInt },
    '{ specializedJStreamAsScalaLong },
    '{ specializedJStreamAsScalaDouble },
    '{ specializedJStreamAsScalaAny },
  )
}

private val specializedJStreamAsScalaInt: AsScala[JIntStream | JStream[Int], ScalaJIntStream] = {
  case jStream: JIntStream => new ScalaJIntStream(jStream)
  case jStream: JStream[Int] => new ScalaJStream(jStream).asIntStream
}
private val specializedJStreamAsScalaLong: AsScala[JLongStream | JStream[Long], ScalaJLongStream] = {
  case jStream: JLongStream => new ScalaJLongStream(jStream)
  case jStream: JStream[Long] => new ScalaJStream(jStream).asLongStream
}
private val specializedJStreamAsScalaDouble: AsScala[JDoubleStream | JStream[Double], ScalaJDoubleStream] = {
  case jStream: JDoubleStream => new ScalaJDoubleStream(jStream)
  case jStream: JStream[Double] => new ScalaJStream(jStream).asDoubleStream
}
private def specializedJStreamAsScalaAny[T]: AsScala[JStream[T], ScalaJStream[T]] = new ScalaJStream(_)

def specializedScalaStreamAsJava[T]: AsJava[SpecializedScalaJStream[T] | ScalaJStream[T], SpecializedJStream[T]] = {
  specialized[T, [U] =>> AsJava[SpecializedScalaJStream[T] | ScalaJStream[T], SpecializedJStream[T]]](
    _.asJava,
    _.asJava,
    _.asJava,
    _.asJava,
  )
}