package com.avsystem.commons
package jiop

import jiop.macros.specializedJStreamAsScala

import java.util as ju

trait JStreamUtils {
  type JBaseStream[T, S <: ju.stream.BaseStream[T, S]] = ju.stream.BaseStream[T, S]
  type JStream[T] = ju.stream.Stream[T]
  type JDoubleStream = ju.stream.DoubleStream
  type JIntStream = ju.stream.IntStream
  type JLongStream = ju.stream.LongStream
  type JCollector[T, A, R] = ju.stream.Collector[T, A, R]

  type SpecializedJStream[T] = T match
    case Nothing => Nothing
    case Int => JIntStream
    case Long => JLongStream
    case Double => JDoubleStream
    case _ => JStream[T]

  type SpecializedScalaJStream[T] = T match
    case Nothing => Nothing
    case Int => ScalaJIntStream
    case Long => ScalaJLongStream
    case Double => ScalaJDoubleStream
    case _ => ScalaJStream[T]

  implicit inline def asScalaJStreamToSpecializedScalaJStream[T]: AsScala[JStream[T], SpecializedScalaJStream[T]] =
    specializedJStreamAsScala[T, JStream[T]]

  given AsScala[JIntStream, SpecializedScalaJStream[Int]] = specializedJStreamAsScala[Int, JIntStream]

  given AsScala[JLongStream, SpecializedScalaJStream[Long]] = specializedJStreamAsScala[Long, JLongStream]

  given AsScala[JDoubleStream, SpecializedScalaJStream[Double]] = specializedJStreamAsScala[Double, JDoubleStream]

  given [T]: AsJava[ScalaJStream[T] | SpecializedScalaJStream[T], SpecializedJStream[T]] = ???

}

object JStreamUtils extends JStreamUtils
