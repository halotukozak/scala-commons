package com.avsystem.commons
package jiop

import java.util.IntSummaryStatistics
import scala.collection.Factory

opaque type ScalaJIntStream = JIntStream
object ScalaJIntStream {
  def apply(jStream: JIntStream): ScalaJIntStream = jStream

  extension (jStream: ScalaJIntStream) {
    def asJava: JIntStream = jStream

    def close(): Unit =
      jStream.close()

    def isParallel: Boolean =
      jStream.isParallel

    def iterator: Iterator[Int] =
      jStream.iterator().asInstanceOf[JIterator[Int]].asScala

    def onClose(closeHandler: => Any): ScalaJIntStream =
      ScalaJIntStream(jStream.onClose(() => closeHandler))

    def parallel: ScalaJIntStream =
      ScalaJIntStream(jStream.parallel())

    def sequential: ScalaJIntStream =
      ScalaJIntStream(jStream.sequential())

    def unordered: ScalaJIntStream =
      ScalaJIntStream(jStream.unordered())

    def allMatch(predicate: Int => Boolean): Boolean =
      jStream.allMatch(predicate(_))

    def anyMatch(predicate: Int => Boolean): Boolean =
      jStream.allMatch(predicate(_))

    def asDoubleStream: ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.asDoubleStream())

    def asLongStream: ScalaJLongStream =
      ScalaJLongStream(jStream.asLongStream())

    def average: Option[Double] =
      jStream.average.asScala

    def boxed: ScalaJStream[Int] =
      ScalaJStream(jStream.boxed.asInstanceOf[JStream[Int]])

    def collect[R](supplier: => R)(accumulator: (R, Int) => Any, combiner: (R, R) => Any): R =
      jStream.collect(() => supplier, accumulator(_, _), combiner(_, _))

    def count: Long =
      jStream.count

    def distinct: ScalaJIntStream =
      ScalaJIntStream(jStream.distinct)

    def filter(predicate: Int => Boolean): ScalaJIntStream =
      ScalaJIntStream(jStream.filter(predicate(_)))

    def findAny: Option[Int] =
      jStream.findAny().asScala

    def findFirst: Option[Int] =
      jStream.findFirst.asScala

    def flatMap(mapper: Int => ScalaJIntStream): ScalaJIntStream =
      ScalaJIntStream(jStream.flatMap((d => mapper(d))))

    def forEach(action: Int => Any): Unit =
      jStream.forEach(action(_))

    def forEachOrdered(action: Int => Any): Unit =
      jStream.forEachOrdered(action(_))

    def limit(maxSize: Long): ScalaJIntStream =
      ScalaJIntStream(jStream.limit(maxSize))

    def map(mapper: Int => Int): ScalaJIntStream =
      ScalaJIntStream(jStream.map(mapper(_)))

    def mapToDouble(mapper: Int => Double): ScalaJDoubleStream =
      ScalaJDoubleStream(jStream.mapToDouble(mapper(_)))

    def mapToLong(mapper: Int => Long): ScalaJLongStream =
      ScalaJLongStream(jStream.mapToLong(mapper(_)))

    def mapToObj[U](mapper: Int => U): ScalaJStream[U] =
      ScalaJStream(jStream.mapToObj(mapper(_)))

    def max: Option[Int] =
      jStream.max.asScala

    def min: Option[Int] =
      jStream.min.asScala

    def noneMatch(predicate: Int => Boolean): Boolean =
      jStream.noneMatch(predicate(_))

    def peek(action: Int => Any): ScalaJIntStream =
      ScalaJIntStream(jStream.peek((action(_))))

    def reduce(identity: Int)(op: (Int, Int) => Int): Int =
      jStream.reduce(identity, op(_, _))

    def reduce(op: (Int, Int) => Int): Option[Int] = {
      jStream.reduce(op(_, _)).asScala
    }

    def skip(n: Long): ScalaJIntStream =
      ScalaJIntStream(jStream.skip(n))

    def sorted: ScalaJIntStream =
      ScalaJIntStream(jStream.sorted)

    def sum: Int =
      jStream.sum

    def summaryStatistics: IntSummaryStatistics =
      jStream.summaryStatistics()

    def toArray: Array[Int] =
      jStream.toArray

    def to[C](using fac: Factory[Int, C]): C = {
      val b = fac.newBuilder
      forEachOrdered(b += _)
      b.result()
    }
  }
}
