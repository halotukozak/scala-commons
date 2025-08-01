package com.avsystem.commons
package jiop

import java.util.LongSummaryStatistics
import scala.collection.Factory

opaque type ScalaJLongStream = JLongStream
object ScalaJLongStream {
  inline def apply(jStream: JLongStream): ScalaJLongStream = (jStream)

  extension (jStream: ScalaJLongStream) {
    inline def asJava: JLongStream = jStream

    inline def close(): Unit =
      jStream.close()

    inline def isParallel: Boolean =
      jStream.isParallel

    inline def iterator: Iterator[Long] =
      jStream.iterator().asInstanceOf[JIterator[Long]].asScala

    inline def onClose(inline closeHandler: Any): ScalaJLongStream =
      ScalaJLongStream(jStream.onClose(() => closeHandler))

    inline def parallel: ScalaJLongStream =
      ScalaJLongStream(jStream.parallel())

    inline def sequential: ScalaJLongStream =
      ScalaJLongStream(jStream.sequential())

    inline def unordered: ScalaJLongStream =
      ScalaJLongStream(jStream.unordered())

    inline def allMatch(inline predicate: Long => Boolean): Boolean =
      jStream.allMatch(predicate(_))

    inline def anyMatch(inline predicate: Long => Boolean): Boolean =
      jStream.anyMatch(predicate(_))

    inline def asDoubleStream: ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.asDoubleStream())

    inline def average: Option[Double] =
      jStream.average.asScala

    inline def boxed: ScalaJStream[Long] =
      ScalaJStream(jStream.boxed.asInstanceOf[JStream[Long]])

    inline def collect[R](
      inline supplier: => R
    )(inline accumulator: (R, Long) => Any, inline combiner: (R, R) => Any): R =
      jStream.collect(() => supplier, accumulator(_, _), combiner(_, _))

    inline def count: Long =
      jStream.count

    inline def distinct: ScalaJLongStream =
      ScalaJLongStream(jStream.distinct)

    inline def filter(inline predicate: Long => Boolean): ScalaJLongStream =
      ScalaJLongStream(jStream.filter(predicate(_)))

    inline def findAny: Option[Long] =
      jStream.findAny().asScala

    inline def findFirst: Option[Long] =
      jStream.findFirst.asScala

    inline def flatMap(inline mapper: Long => ScalaJLongStream): ScalaJLongStream =
      ScalaJLongStream(jStream.flatMap(d => mapper(d)))

    inline def forEach(inline action: Long => Any): Unit =
      jStream.forEach(action(_))

    inline def forEachOrdered(inline action: Long => Any): Unit =
      jStream.forEachOrdered(action(_))

    inline def limit(maxSize: Long): ScalaJLongStream =
      ScalaJLongStream(jStream.limit(maxSize))

    inline def map(inline mapper: Long => Long): ScalaJLongStream =
      ScalaJLongStream(jStream.map(mapper(_)))

    inline def mapToDouble(inline mapper: Long => Double): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.mapToDouble(mapper(_)))

    inline def mapToInt(inline mapper: Long => Int): ScalaJIntStream =
      ScalaJIntStream(jStream.mapToInt(mapper(_)))

    inline def mapToObj[U](inline mapper: Long => U): ScalaJStream[U] =
      ScalaJStream(jStream.mapToObj(mapper(_)))

    inline def max: Option[Long] =
      jStream.max.asScala

    inline def min: Option[Long] =
      jStream.min.asScala

    inline def noneMatch(inline predicate: Long => Boolean): Boolean =
      jStream.noneMatch(predicate(_))

    inline def peek(inline action: Long => Any): ScalaJLongStream =
      ScalaJLongStream(jStream.peek(action(_)))

    inline def reduce(identity: Long)(inline op: (Long, Long) => Long): Long =
      jStream.reduce(identity, op(_, _))

    inline def reduce(inline op: (Long, Long) => Long): Option[Long] =
      jStream.reduce(op(_, _)).asScala

    inline def skip(n: Long): ScalaJLongStream =
      ScalaJLongStream(jStream.skip(n))

    inline def sorted: ScalaJLongStream =
      ScalaJLongStream(jStream.sorted)

    inline def sum: Long =
      jStream.sum

    inline def summaryStatistics: LongSummaryStatistics =
      jStream.summaryStatistics()

    inline def toArray: Array[Long] =
      jStream.toArray

    inline def to[C](using fac: Factory[Long, C]): C = {
      val b = fac.newBuilder
      forEachOrdered(b += _)
      b.result()
    }

  }
}
