package com.avsystem.commons
package jiop

import java.util.LongSummaryStatistics
import scala.collection.Factory

opaque type ScalaJLongStream = JLongStream
object ScalaJLongStream {
  def apply(jStream: JLongStream): ScalaJLongStream = (jStream)

  extension (jStream: ScalaJLongStream) {
    def asJava: JLongStream = jStream

    def close(): Unit =
      jStream.close()

    def isParallel: Boolean =
      jStream.isParallel

    def iterator: Iterator[Long] =
      jStream.iterator().asInstanceOf[JIterator[Long]].asScala

    def onClose(closeHandler: => Any): ScalaJLongStream =
      ScalaJLongStream(jStream.onClose(() => closeHandler))

    def parallel: ScalaJLongStream =
      ScalaJLongStream(jStream.parallel())

    def sequential: ScalaJLongStream =
      ScalaJLongStream(jStream.sequential())

    def unordered: ScalaJLongStream =
      ScalaJLongStream(jStream.unordered())

    def allMatch(predicate: Long => Boolean): Boolean =
      jStream.allMatch(predicate(_))

    def anyMatch(predicate: Long => Boolean): Boolean =
      jStream.allMatch(predicate(_))

    def asDoubleStream: ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.asDoubleStream())

    def average: Option[Double] =
      jStream.average.asScala

    def boxed: ScalaJStream[Long] =
      ScalaJStream(jStream.boxed.asInstanceOf[JStream[Long]])

    def collect[R](supplier: => R)(accumulator: (R, Long) => Any, combiner: (R, R) => Any): R =
      jStream.collect(() => supplier, accumulator(_, _), combiner(_, _))

    def count: Long =
      jStream.count

    def distinct: ScalaJLongStream =
      ScalaJLongStream(jStream.distinct)

    def filter(predicate: Long => Boolean): ScalaJLongStream =
      ScalaJLongStream(jStream.filter(predicate(_)))

    def findAny: Option[Long] =
      jStream.findAny().asScala

    def findFirst: Option[Long] =
      jStream.findFirst.asScala

    def flatMap(mapper: Long => ScalaJLongStream): ScalaJLongStream =
      ScalaJLongStream(jStream.flatMap(d => mapper(d)))

    def forEach(action: Long => Any): Unit =
      jStream.forEach(action(_))

    def forEachOrdered(action: Long => Any): Unit =
      jStream.forEachOrdered(action(_))

    def limit(maxSize: Long): ScalaJLongStream =
      ScalaJLongStream(jStream.limit(maxSize))

    def map(mapper: Long => Long): ScalaJLongStream =
      ScalaJLongStream(jStream.map(mapper(_)))

    def mapToDouble(mapper: Long => Double): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.mapToDouble(mapper(_)))

    def mapToInt(mapper: Long => Int): ScalaJIntStream =
      ScalaJIntStream(jStream.mapToInt(mapper(_)))

    def mapToObj[U](mapper: Long => U): ScalaJStream[U] =
      ScalaJStream(jStream.mapToObj(mapper(_)))

    def max: Option[Long] =
      jStream.max.asScala

    def min: Option[Long] =
      jStream.min.asScala

    def noneMatch(predicate: Long => Boolean): Boolean =
      jStream.noneMatch(predicate(_))

    def peek(action: Long => Any): ScalaJLongStream =
      ScalaJLongStream(jStream.peek(action(_)))

    def reduce(identity: Long)(op: (Long, Long) => Long): Long =
      jStream.reduce(identity, op(_, _))

    def reduce(op: (Long, Long) => Long): Option[Long] =
      jStream.reduce(op(_, _)).asScala

    def skip(n: Long): ScalaJLongStream =
      ScalaJLongStream(jStream.skip(n))

    def sorted: ScalaJLongStream =
      ScalaJLongStream(jStream.sorted)

    def sum: Long =
      jStream.sum

    def summaryStatistics: LongSummaryStatistics =
      jStream.summaryStatistics()

    def toArray: Array[Long] =
      jStream.toArray

    def to[C](using fac: Factory[Long, C]): C = {
      val b = fac.newBuilder
      forEachOrdered(b += _)
      b.result()
    }

  }
}