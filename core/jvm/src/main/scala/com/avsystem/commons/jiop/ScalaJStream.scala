package com.avsystem.commons
package jiop

import scala.annotation.unchecked.uncheckedVariance as uV
import scala.collection.Factory

opaque type ScalaJStream[+A] = JStream[A @uV]
object ScalaJStream {
  def apply[A](jStream: JStream[A]): ScalaJStream[A] = (jStream)

  extension [A](jStream: ScalaJStream[A]) {
    def asJava[B >: A]: JStream[B] =
      jStream.asInstanceOf[JStream[B]]

    def close(): Unit =
      jStream.close()

    def isParallel: Boolean =
      jStream.isParallel

    def parallel: ScalaJStream[A] =
      ScalaJStream(jStream.parallel())

    def onClose(closeHandler: => Any): ScalaJStream[A] =
      ScalaJStream(jStream.onClose(() => closeHandler))

    def sequential: ScalaJStream[A] =
      ScalaJStream(jStream.sequential())

    def unordered: ScalaJStream[A] =
      ScalaJStream(jStream.unordered())

    def iterator: Iterator[A] =
      jStream.iterator().asScala

    def asDoubleStream(using ev: A <:< Double): ScalaJDoubleStream =
      mapToDouble(ev)

    def asIntStream(using ev: A <:< Int): ScalaJIntStream =
      mapToInt(ev)

    def asLongStream(using ev: A <:< Long): ScalaJLongStream =
      mapToLong(ev)

    def allMatch(predicate: A => Boolean): Boolean =
      jStream.allMatch(predicate(_))

    def anyMatch(predicate: A => Boolean): Boolean =
      jStream.anyMatch(predicate(_))

    def collect[R, B](collector: JCollector[? >: A @uV, B, R]): R =
      jStream.collect(collector)

    def collect[R](supplier: => R)(accumulator: (R, A) => Any, combiner: (R, R) => Any): R =
      jStream.collect(() => supplier, accumulator(_, _), combiner(_, _))

    def count: Long =
      jStream.count()

    def distinct: ScalaJStream[A] =
      ScalaJStream(jStream.distinct())

    def filter(predicate: A => Boolean): ScalaJStream[A] =
      ScalaJStream(jStream.filter(predicate(_)))

    def findAny: Option[A] =
      jStream.findAny().asScala

    def findFirst: Option[A] =
      jStream.findFirst().asScala

    def flatMap[R](mapper: A => ScalaJStream[R]): ScalaJStream[R] =
      ScalaJStream(jStream.flatMap(t => mapper(t)))

    def flatMapToDouble(mapper: A => ScalaJDoubleStream): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.flatMapToDouble(t => mapper(t).asJava))

    def flatMapToInt(mapper: A => ScalaJIntStream): ScalaJIntStream =
      ScalaJIntStream((jStream: JStream[A]).flatMapToInt(t => mapper(t).asJava))

    def flatMapToLong(mapper: A => ScalaJLongStream): ScalaJLongStream =
      ScalaJLongStream(jStream.flatMapToLong(t => mapper(t).asJava))

    def forEach(action: A => Any): Unit =
      jStream.forEach(action(_))

    def forEachOrdered(action: A => Any): Unit =
      jStream.forEachOrdered(action(_))

    def limit(maxSize: Long): ScalaJStream[A] =
      ScalaJStream(jStream.limit(maxSize))

    def map[R](mapper: A => R): ScalaJStream[R] =
      ScalaJStream(jStream.map[R](mapper(_)))

    def mapToDouble(mapper: A => Double): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.mapToDouble(mapper(_)))

    def mapToInt(mapper: A => Int): ScalaJIntStream =
      ScalaJIntStream(jStream.mapToInt(mapper(_)))

    def mapToLong(mapper: A => Long): ScalaJLongStream =
      ScalaJLongStream(jStream.mapToLong(mapper(_)))

    def max(comparator: (A, A) => Int): Option[A] =
      jStream.max(comparator(_, _)).asScala

    def min(comparator: (A, A) => Int): Option[A] =
      jStream.min(comparator(_, _)).asScala

    def noneMatch(predicate: A => Boolean): Boolean =
      jStream.noneMatch(predicate(_))

    def peek(action: A => Any): ScalaJStream[A] =
      ScalaJStream(jStream.peek(action(_)))

    def reduce[B >: A](accumulator: (B, B) => B): Option[B] =
      jStream.asInstanceOf[JStream[B]].reduce(accumulator(_, _)).asScala

    def reduce[B >: A](identity: B)(accumulator: (B, B) => B): B =
      jStream.asInstanceOf[JStream[B]].reduce(identity, accumulator(_, _))

    def reduce[U](identity: U)(accumulator: (U, A) => U, combiner: (U, U) => U): U =
      jStream.reduce(identity, accumulator(_, _), combiner(_, _))

    def skip(n: Long): ScalaJStream[A] =
      ScalaJStream(jStream.skip(n))

    def sorted: ScalaJStream[A] =
      ScalaJStream(jStream.sorted)

    def sorted(comparator: (A, A) => Int): ScalaJStream[A] =
      ScalaJStream(jStream.sorted(comparator(_, _)))

    def toArray[B >: A <: AnyRef : ClassTag]: Array[B] =
      jStream.toArray[B](n => Array.ofDim[B](n))

    def to[C](fac: Factory[A, C]): C = {
      val b = fac.newBuilder
      forEachOrdered(b += _)
      b.result()
    }
  }
}