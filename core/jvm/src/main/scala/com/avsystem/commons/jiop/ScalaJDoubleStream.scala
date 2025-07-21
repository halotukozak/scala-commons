package com.avsystem.commons
package jiop

import java.util.DoubleSummaryStatistics
import scala.collection.Factory

opaque type ScalaJDoubleStream = JDoubleStream
object ScalaJDoubleStream {
  def apply(jStream: JDoubleStream): ScalaJDoubleStream = jStream

  extension (jStream: ScalaJDoubleStream) {
    def asJava: JDoubleStream = jStream

    def close(): Unit =
      jStream.close()

    def isParallel: Boolean =
      jStream.isParallel

    def iterator: Iterator[Double] =
      jStream.iterator().asInstanceOf[JIterator[Double]].asScala

    def onClose(closeHandler: => Any): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.onClose(jRunnable(closeHandler)))

    def parallel: ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.parallel())

    def sequential: ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.sequential())

    def unordered: ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.unordered())

    def allMatch(predicate: Double => Boolean): Boolean =
      jStream.allMatch(jDoublePredicate(predicate))

    def anyMatch(predicate: Double => Boolean): Boolean =
      jStream.allMatch(jDoublePredicate(predicate))

    def average: Option[Double] =
      jStream.average.asScala

    def boxed: ScalaJStream[Double] =
      ScalaJStream(jStream.boxed.asInstanceOf[JStream[Double]])

    def collect[R](supplier: => R)(accumulator: (R, Double) => Any, combiner: (R, R) => Any): R =
      jStream.collect(() => supplier, accumulator(_, _), combiner(_, _))

    def count: Long =
      jStream.count

    def distinct: ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.distinct)

    def filter(predicate: Double => Boolean): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.filter(predicate(_)))

    def findAny: Option[Double] =
      jStream.findAny().asScala

    def findFirst: Option[Double] =
      jStream.findFirst.asScala

    def flatMap(mapper: Double => ScalaJDoubleStream): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.flatMap(mapper(_)))

    def forEach(action: Double => Any): Unit =
      jStream.forEach((action(_)))

    def forEachOrdered(action: Double => Any): Unit =
      jStream.forEachOrdered((action(_)))

    def limit(maxSize: Long): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.limit(maxSize))

    def map(mapper: Double => Double): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.map((mapper(_))))

    def mapToInt(mapper: Double => Int): ScalaJIntStream =
      ScalaJIntStream(jStream.mapToInt((mapper(_))))

    def mapToLong(mapper: Double => Long): ScalaJLongStream =
      ScalaJLongStream(jStream.mapToLong((mapper(_))))

    def mapToObj[U](mapper: Double => U): ScalaJStream[U] =
      ScalaJStream(jStream.mapToObj((mapper(_))))

    def max: Option[Double] =
      jStream.max.asScala

    def min: Option[Double] =
      jStream.min.asScala

    def noneMatch(predicate: Double => Boolean): Boolean =
      jStream.noneMatch(jDoublePredicate(predicate))

    def peek(action: Double => Any): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.peek(jDoubleConsumer(action)))

    def reduce(identity: Double)(op: (Double, Double) => Double): Double =
      jStream.reduce(identity, jDoubleBinaryOperator(op))

    def reduce(op: (Double, Double) => Double): Option[Double] =
      jStream.reduce(jDoubleBinaryOperator(op)).asScala

    def skip(n: Long): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.skip(n))

    def sorted: ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.sorted)

    def sum: Double =
      jStream.sum

    def summaryStatistics: DoubleSummaryStatistics =
      jStream.summaryStatistics()

    def toArray: Array[Double] =
      jStream.toArray

    def to[C](using fac: Factory[Double, C]): C = {
      val b = fac.newBuilder
      forEachOrdered(b += _)
      b.result()
    }
  }
}
