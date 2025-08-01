package com.avsystem.commons
package jiop

import java.util.IntSummaryStatistics
import scala.collection.Factory

opaque type ScalaJIntStream = JIntStream
object ScalaJIntStream {
  inline def apply(jStream: JIntStream): ScalaJIntStream = jStream

  extension (jStream: ScalaJIntStream) {
    inline def asJava: JIntStream = jStream

    inline def close(): Unit =
      jStream.close()

    inline def isParallel: Boolean =
      jStream.isParallel

    inline def iterator: Iterator[Int] =
      jStream.iterator().asInstanceOf[JIterator[Int]].asScala

    inline def onClose(inline closeHandler: Any): ScalaJIntStream =
      ScalaJIntStream(jStream.onClose(() => closeHandler))

    inline def parallel: ScalaJIntStream =
      ScalaJIntStream(jStream.parallel())

    inline def sequential: ScalaJIntStream =
      ScalaJIntStream(jStream.sequential())

    inline def unordered: ScalaJIntStream =
      ScalaJIntStream(jStream.unordered())

    inline def allMatch(inline predicate: Int => Boolean): Boolean =
      jStream.allMatch(predicate(_))

    inline def anyMatch(inline predicate: Int => Boolean): Boolean =
      jStream.anyMatch(predicate(_))

    inline def asDoubleStream: ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.asDoubleStream())

    inline def asLongStream: ScalaJLongStream =
      ScalaJLongStream(jStream.asLongStream())

    inline def average: Option[Double] =
      jStream.average.asScala

    inline def boxed: ScalaJStream[Int] =
      ScalaJStream(jStream.boxed.asInstanceOf[JStream[Int]])

    inline def collect[R](
      inline supplier: => R
    )(inline accumulator: (R, Int) => Any, inline combiner: (R, R) => Any): R =
      jStream.collect(() => supplier, accumulator(_, _), combiner(_, _))

    inline def count: Long =
      jStream.count

    inline def distinct: ScalaJIntStream =
      ScalaJIntStream(jStream.distinct)

    inline def filter(inline predicate: Int => Boolean): ScalaJIntStream =
      ScalaJIntStream(jStream.filter(predicate(_)))

    inline def findAny: Option[Int] =
      jStream.findAny().asScala

    inline def findFirst: Option[Int] =
      jStream.findFirst.asScala

    inline def flatMap(mapper: Int => ScalaJIntStream): ScalaJIntStream =
      ScalaJIntStream(jStream.flatMap((d => mapper(d))))

    inline def forEach(inline action: Int => Any): Unit =
      jStream.forEach(action(_))

    inline def forEachOrdered(inline action: Int => Any): Unit =
      jStream.forEachOrdered(action(_))

    inline def limit(maxSize: Long): ScalaJIntStream =
      ScalaJIntStream(jStream.limit(maxSize))

    inline def map(inline mapper: Int => Int): ScalaJIntStream =
      ScalaJIntStream(jStream.map(mapper(_)))

    inline def mapToDouble(inline mapper: Int => Double): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.mapToDouble(mapper(_)))

    inline def mapToLong(inline mapper: Int => Long): ScalaJLongStream =
      ScalaJLongStream(jStream.mapToLong(mapper(_)))

    inline def mapToObj[U](inline mapper: Int => U): ScalaJStream[U] =
      ScalaJStream(jStream.mapToObj(mapper(_)))

    inline def max: Option[Int] =
      jStream.max.asScala

    inline def min: Option[Int] =
      jStream.min.asScala

    inline def noneMatch(inline predicate: Int => Boolean): Boolean =
      jStream.noneMatch(predicate(_))

    inline def peek(inline action: Int => Any): ScalaJIntStream =
      ScalaJIntStream(jStream.peek(action(_)))

    inline def reduce(identity: Int)(op: (Int, Int) => Int): Int =
      jStream.reduce(identity, op(_, _))

    inline def reduce(inline op: (Int, Int) => Int): Option[Int] = {
      jStream.reduce(op(_, _)).asScala
    }

    inline def skip(n: Long): ScalaJIntStream =
      ScalaJIntStream(jStream.skip(n))

    inline def sorted: ScalaJIntStream =
      ScalaJIntStream(jStream.sorted)

    inline def sum: Int =
      jStream.sum

    inline def summaryStatistics: IntSummaryStatistics =
      jStream.summaryStatistics()

    inline def toArray: Array[Int] =
      jStream.toArray

    inline def to[C](using fac: Factory[Int, C]): C = {
      val b = fac.newBuilder
      forEachOrdered(b += _)
      b.result()
    }
  }
}
