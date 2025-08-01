package com.avsystem.commons
package jiop

import java.util.DoubleSummaryStatistics
import scala.collection.Factory

opaque type ScalaJDoubleStream = JDoubleStream
object ScalaJDoubleStream {
  inline def apply(jStream: JDoubleStream): ScalaJDoubleStream = jStream

  extension (jStream: ScalaJDoubleStream) {
    inline def asJava: JDoubleStream = jStream

    inline def close(): Unit =
      jStream.close()

    inline def isParallel: Boolean =
      jStream.isParallel

    inline def iterator: Iterator[Double] =
      jStream.iterator().asInstanceOf[JIterator[Double]].asScala

    inline def onClose(inline closeHandler: Any): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.onClose(() => closeHandler))

    inline def parallel: ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.parallel())

    inline def sequential: ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.sequential())

    inline def unordered: ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.unordered())

    inline def allMatch(inline predicate: Double => Boolean): Boolean =
      jStream.allMatch(predicate(_))

    inline def anyMatch(inline predicate: Double => Boolean): Boolean =
      jStream.anyMatch(predicate(_))

    inline def average: Option[Double] =
      jStream.average.asScala

    inline def boxed: ScalaJStream[Double] =
      ScalaJStream(jStream.boxed.asInstanceOf[JStream[Double]])

    inline def collect[R](
      inline supplier: => R
    )(inline accumulator: (R, Double) => Any, inline combiner: (R, R) => Any): R =
      jStream.collect(() => supplier, accumulator(_, _), combiner(_, _))

    inline def count: Long =
      jStream.count

    inline def distinct: ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.distinct)

    inline def filter(predicate: Double => Boolean): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.filter(predicate(_)))

    inline def findAny: Option[Double] =
      jStream.findAny().asScala

    inline def findFirst: Option[Double] =
      jStream.findFirst.asScala

    inline def flatMap(mapper: Double => ScalaJDoubleStream): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.flatMap(mapper(_)))

    inline def forEach(inline action: Double => Any): Unit =
      jStream.forEach(action(_))

    inline def forEachOrdered(inline action: Double => Any): Unit =
      jStream.forEachOrdered(action(_))

    inline def limit(maxSize: Long): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.limit(maxSize))

    inline def map(inline mapper: Double => Double): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.map(mapper(_)))

    inline def mapToInt(inline mapper: Double => Int): ScalaJIntStream =
      ScalaJIntStream(jStream.mapToInt(mapper(_)))

    inline def mapToLong(inline mapper: Double => Long): ScalaJLongStream =
      ScalaJLongStream(jStream.mapToLong(mapper(_)))

    inline def mapToObj[U](inline mapper: Double => U): ScalaJStream[U] =
      ScalaJStream(jStream.mapToObj(mapper(_)))

    inline def max: Option[Double] =
      jStream.max.asScala

    inline def min: Option[Double] =
      jStream.min.asScala

    inline def noneMatch(predicate: Double => Boolean): Boolean =
      jStream.noneMatch(predicate(_))

    inline def peek(inline action: Double => Any): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.peek(action(_)))

    inline def reduce(identity: Double)(op: (Double, Double) => Double): Double =
      jStream.reduce(identity, op(_, _))

    inline def reduce(inline op: (Double, Double) => Double): Option[Double] =
      jStream.reduce(op(_, _)).asScala

    inline def skip(n: Long): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.skip(n))

    inline def sorted: ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.sorted)

    inline def sum: Double =
      jStream.sum

    inline def summaryStatistics: DoubleSummaryStatistics =
      jStream.summaryStatistics()

    inline def toArray: Array[Double] =
      jStream.toArray

    inline def to[C](using fac: Factory[Double, C]): C = {
      val b = fac.newBuilder
      forEachOrdered(b += _)
      b.result()
    }
  }
}
