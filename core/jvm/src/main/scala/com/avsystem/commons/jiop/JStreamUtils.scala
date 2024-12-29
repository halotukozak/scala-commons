package com.avsystem.commons
package jiop

import java.util as ju

trait JStreamUtils {
  opaque type ScalaJStream[A] = JStream[A]
  opaque type ScalaJDoubleStream = JDoubleStream
  opaque type ScalaJIntStream = JIntStream
  opaque type ScalaJLongStream = JLongStream

  type JBaseStream[T, S <: ju.stream.BaseStream[T, S]] = ju.stream.BaseStream[T, S]
  type JStream[T] = ju.stream.Stream[T]
  type JDoubleStream = ju.stream.DoubleStream
  type JIntStream = ju.stream.IntStream
  type JLongStream = ju.stream.LongStream
  type JCollector[T, A, R] = ju.stream.Collector[T, A, R]

  given [A]: AsScala[JStream[A], ScalaJStream[A]] = identity

  given [A]: AsJava[ScalaJStream[A], JStream[A]] = identity

  given AsScala[JDoubleStream, ScalaJDoubleStream] = identity

  given AsJava[ScalaJDoubleStream, JDoubleStream] = identity

  given AsScala[JIntStream, ScalaJIntStream] = identity

  given AsJava[ScalaJIntStream, JIntStream] = identity

  given AsScala[JLongStream, ScalaJLongStream] = identity

  given AsJava[ScalaJLongStream, JLongStream] = identity
}

object JStreamUtils extends JStreamUtils
