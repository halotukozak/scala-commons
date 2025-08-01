package com.avsystem.commons
package jiop

import scala.annotation.unchecked.uncheckedVariance as uV
import scala.collection.Factory

opaque type ScalaJStream[+A] = JStream[A @uV]
object ScalaJStream {
  inline def apply[A](jStream: JStream[A]): ScalaJStream[A] = jStream

  extension [A](jStream: ScalaJStream[A]) {
    inline def asJava[B >: A]: JStream[B] =
      jStream.asInstanceOf[JStream[B]]

    inline def close(): Unit =
      jStream.close()

    inline def isParallel: Boolean =
      jStream.isParallel

    inline def parallel: ScalaJStream[A] =
      ScalaJStream(jStream.parallel())

    inline def onClose(inline closeHandler: Any): ScalaJStream[A] =
      ScalaJStream(jStream.onClose(() => closeHandler))

    inline def sequential: ScalaJStream[A] =
      ScalaJStream(jStream.sequential())

    inline def unordered: ScalaJStream[A] =
      ScalaJStream(jStream.unordered())

    inline def iterator: Iterator[A] =
      jStream.iterator().asScala

    inline def asDoubleStream(using ev: A <:< Double): ScalaJDoubleStream =
      mapToDouble(ev)

    inline def asIntStream(using ev: A <:< Int): ScalaJIntStream =
      mapToInt(ev)

    inline def asLongStream(using ev: A <:< Long): ScalaJLongStream =
      mapToLong(ev)

    inline def allMatch(inline predicate: A => Boolean): Boolean =
      jStream.allMatch(predicate(_))

    inline def anyMatch(inline predicate: A => Boolean): Boolean =
      jStream.anyMatch(predicate(_))

    inline def collect[R, B](collector: JCollector[? >: A @uV, B, R]): R =
      jStream.collect(collector)

    inline def collect[R](inline supplier: => R)(inline accumulator: (R, A) => Any, inline combiner: (R, R) => Any): R =
      jStream.collect(() => supplier, accumulator(_, _), combiner(_, _))

    inline def count: Long =
      jStream.count()

    inline def distinct: ScalaJStream[A] =
      ScalaJStream(jStream.distinct())

    inline def filter(inline predicate: A => Boolean): ScalaJStream[A] =
      ScalaJStream(jStream.filter(predicate(_)))

    inline def findAny: Option[A] =
      jStream.findAny().asScala

    inline def findFirst: Option[A] =
      jStream.findFirst().asScala

    inline def flatMap[R](inline mapper: A => ScalaJStream[R]): ScalaJStream[R] =
      ScalaJStream(jStream.flatMap(t => mapper(t)))

    inline def flatMapToDouble(inline mapper: A => ScalaJDoubleStream): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.flatMapToDouble(t => mapper(t).asJava))

    inline def flatMapToInt(inline mapper: A => ScalaJIntStream): ScalaJIntStream =
      ScalaJIntStream((jStream: JStream[A]).flatMapToInt(t => mapper(t).asJava))

    inline def flatMapToLong(inline mapper: A => ScalaJLongStream): ScalaJLongStream =
      ScalaJLongStream(jStream.flatMapToLong(t => mapper(t).asJava))

    inline def forEach(inline action: A => Any): Unit =
      jStream.forEach(action(_))

    inline def forEachOrdered(inline action: A => Any): Unit =
      jStream.forEachOrdered(action(_))

    inline def limit(maxSize: Long): ScalaJStream[A] =
      ScalaJStream(jStream.limit(maxSize))

    inline def map[R](inline mapper: A => R): ScalaJStream[R] =
      ScalaJStream(jStream.map[R](mapper(_)))

    inline def mapToDouble(inline mapper: A => Double): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.mapToDouble(mapper(_)))

    inline def mapToInt(inline mapper: A => Int): ScalaJIntStream =
      ScalaJIntStream(jStream.mapToInt(mapper(_)))

    inline def mapToLong(inline mapper: A => Long): ScalaJLongStream =
      ScalaJLongStream(jStream.mapToLong(mapper(_)))

    inline def max(inline comparator: (A, A) => Int): Option[A] =
      jStream.max(comparator(_, _)).asScala

    inline def min(inline comparator: (A, A) => Int): Option[A] =
      jStream.min(comparator(_, _)).asScala

    inline def noneMatch(inline predicate: A => Boolean): Boolean =
      jStream.noneMatch(predicate(_))

    inline def peek(inline action: A => Any): ScalaJStream[A] =
      ScalaJStream(jStream.peek(action(_)))

    inline def reduce[B >: A](inline accumulator: (B, B) => B): Option[B] =
      jStream.asInstanceOf[JStream[B]].reduce(accumulator(_, _)).asScala

    inline def reduce[B >: A](identity: B)(inline accumulator: (B, B) => B): B =
      jStream.asInstanceOf[JStream[B]].reduce(identity, accumulator(_, _))

    inline def reduce[U](identity: U)(inline accumulator: (U, A) => U, inline combiner: (U, U) => U): U =
      jStream.reduce(identity, accumulator(_, _), combiner(_, _))

    inline def skip(n: Long): ScalaJStream[A] =
      ScalaJStream(jStream.skip(n))

    inline def sorted: ScalaJStream[A] =
      ScalaJStream(jStream.sorted)

    inline def sorted(inline comparator: (A, A) => Int): ScalaJStream[A] =
      ScalaJStream(jStream.sorted(comparator(_, _)))

    inline def toArray[B >: A <: AnyRef: ClassTag]: Array[B] =
      jStream.toArray[B](n => Array.ofDim[B](n))

    inline def to[C](fac: Factory[A, C]): C = {
      val b = fac.newBuilder
      forEachOrdered(b += _)
      b.result()
    }
  }
}
