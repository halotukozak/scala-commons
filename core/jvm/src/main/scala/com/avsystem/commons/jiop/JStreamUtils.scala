package com.avsystem.commons
package jiop

import java.util as ju

trait JStreamUtils {
  type JBaseStream[T, S <: ju.stream.BaseStream[T, S]] = ju.stream.BaseStream[T, S]
  type JStream[T] = ju.stream.Stream[T]
  type JDoubleStream = ju.stream.DoubleStream
  type JIntStream = ju.stream.IntStream
  type JLongStream = ju.stream.LongStream
  type JCollector[T, A, R] = ju.stream.Collector[T, A, R]

  import JStreamUtils.*

  type SpecializedJStream[T] = T match
    case Int => JIntStream
    case Long => JLongStream
    case Double => JDoubleStream
    case _ => JStream[T]

  type SpecializedScalaJStream[T] = T match
    case Int => ScalaJIntStream
    case Long => ScalaJLongStream
    case Double => ScalaJDoubleStream
    case _ => ScalaJStream[T]

  inline given [T]: AsScala[JStream[T], SpecializedScalaJStream[T]] = 
    ???
//    specializedJStreamAsScala[T]
  inline given [T]: AsScala[SpecializedJStream[T], SpecializedScalaJStream[T]] = 
    ???
//    specializedJStreamAsScala[T]

  inline given [T]: AsJava[ScalaJStream[T] | SpecializedScalaJStream[T], SpecializedJStream[T]] = ???
//  specializedScalaStreamAsJava[T]

  extension [T](stream: JStream[T]) {
    def scalaStream: SpecializedScalaJStream[T] = stream.asScala
  }

}

object JStreamUtils extends JStreamUtils
