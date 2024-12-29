package com.avsystem.commons
package jiop

import jiop.ScalaJStreamUtils.{*, given}

import java.util.{DoubleSummaryStatistics, IntSummaryStatistics, LongSummaryStatistics}
import scala.collection.Factory

trait ScalaJStreamUtils extends JStreamUtils {

  extension [A](scalaJStream: ScalaJStream[A])

    inline def close(): Unit =
      scalaJStream.asJava.close()

    inline def isParallel: Boolean =
      scalaJStream.asJava.isParallel

    inline def parallel: ScalaJStream[A] =
      scalaJStream.asJava.parallel().asScala

    inline def onClose(closeHandler: => Any): ScalaJStream[A] =
      scalaJStream.asJava.onClose(jRunnable(closeHandler)).asScala

    inline def sequential: ScalaJStream[A] =
      scalaJStream.asJava.sequential().asScala

    inline def unordered: ScalaJStream[A] =
      scalaJStream.asJava.unordered().asScala

    inline def iterator: Iterator[A] =
      scalaJStream.asJava.iterator().asScala

    inline def asDoubleStream(using ev: A <:< Double): ScalaJDoubleStream =
      mapToDouble(ev)

    inline def asIntStream(using ev: A <:< Int): ScalaJIntStream =
      mapToInt(ev)

    inline def asLongStream(using ev: A <:< Long): ScalaJLongStream =
      mapToLong(ev)

    inline def allMatch(predicate: A => Boolean): Boolean =
      scalaJStream.asJava.allMatch(jPredicate(predicate))

    inline def anyMatch(predicate: A => Boolean): Boolean =
      scalaJStream.asJava.anyMatch(jPredicate(predicate))

    inline def collect[R, B](collector: JCollector[? >: A, B, R]): R =
      scalaJStream.asJava.collect(collector)

    inline def collect[R](supplier: => R)(accumulator: (R, A) => Any, combiner: (R, R) => Any): R =
      scalaJStream.asJava.collect(jSupplier(supplier), jBiConsumer(accumulator), jBiConsumer(combiner))

    inline def count: Long =
      scalaJStream.asJava.count()

    inline def distinct: ScalaJStream[A] =
      scalaJStream.asJava.distinct().asScala

    inline def filter(predicate: A => Boolean): ScalaJStream[A] =
      scalaJStream.asJava.filter(jPredicate(predicate)).asScala

    inline def findAny: Option[A] =
      scalaJStream.asJava.findAny().asScala

    inline def findFirst: Option[A] =
      scalaJStream.asJava.findFirst().asScala

    inline def flatMap[R](mapper: A => ScalaJStream[R]): ScalaJStream[R] =
      scalaJStream.asJava.flatMap(jFunction(t => mapper(t).asJava)).asScala

    inline def flatMapToDouble(mapper: A => ScalaJDoubleStream): ScalaJDoubleStream =
      scalaJStream.asJava.flatMapToDouble(jFunction(t => mapper(t).asJava)).asScala

    inline def flatMapToInt(mapper: A => ScalaJIntStream): ScalaJIntStream =
      scalaJStream.asJava.flatMapToInt(jFunction(t => mapper(t).asJava)).asScala

    inline def flatMapToLong(mapper: A => ScalaJLongStream): ScalaJLongStream =
      scalaJStream.asJava.flatMapToLong(jFunction(t => mapper(t).asJava)).asScala

    inline def forEach(action: A => Any): Unit =
      scalaJStream.asJava.forEach(jConsumer(action))

    inline def forEachOrdered(action: A => Any): Unit =
      scalaJStream.asJava.forEachOrdered(jConsumer(action))

    inline def limit(maxSize: Long): ScalaJStream[A] =
      scalaJStream.asJava.limit(maxSize).asScala

    inline def map[R](mapper: A => R): ScalaJStream[R] =
      scalaJStream.asJava.map(jFunction(mapper)).asScala

    inline def mapToDouble(mapper: A => Double): ScalaJDoubleStream =
      scalaJStream.asJava.mapToDouble(jToDoubleFunction(mapper)).asScala

    inline def mapToInt(mapper: A => Int): ScalaJIntStream =
      scalaJStream.asJava.mapToInt(jToIntFunction(mapper)).asScala

    inline def mapToLong(mapper: A => Long): ScalaJLongStream =
      scalaJStream.asJava.mapToLong(jToLongFunction(mapper)).asScala

    inline def max(comparator: (A, A) => Int): Option[A] =
      scalaJStream.asJava.max(jComparator(comparator)).asScala

    inline def min(comparator: (A, A) => Int): Option[A] =
      scalaJStream.asJava.min(jComparator(comparator)).asScala

    inline def noneMatch(predicate: A => Boolean): Boolean =
      scalaJStream.asJava.noneMatch(jPredicate(predicate))

    inline def peek(action: A => Any): ScalaJStream[A] =
      scalaJStream.asJava.peek(jConsumer(action)).asScala

    inline def reduce[B >: A](accumulator: (B, B) => B): Option[B] =
      scalaJStream.asJava.asInstanceOf[JStream[B]].reduce(jBinaryOperator(accumulator)).asScala

    inline def reduce[B >: A](identity: B)(accumulator: (B, B) => B): B =
      scalaJStream.asJava.asInstanceOf[JStream[B]].reduce(identity, jBinaryOperator(accumulator))

    inline def reduce[U](identity: U)(accumulator: (U, A) => U, combiner: (U, U) => U): U =
      scalaJStream.asJava.reduce(identity, jBiFunction(accumulator), jBinaryOperator(combiner))

    inline def skip(n: Long): ScalaJStream[A] =
      scalaJStream.asJava.skip(n).asScala

    inline def sorted: ScalaJStream[A] =
      scalaJStream.asJava.sorted.asScala

    inline def sorted(comparator: (A, A) => Int): ScalaJStream[A] =
      scalaJStream.asJava.sorted(jComparator(comparator)).asScala

    inline def toArray[B >: A <: AnyRef : ClassTag]: Array[B] =
      scalaJStream.asJava.toArray[B](jIntFunction(n => new Array[B](n)))

    inline def to[C](fac: Factory[A, C]): C = {
      val b = fac.newBuilder
      forEachOrdered(b += _)
      b.result()
    }
  end extension

  extension (jStream: JStream[Int])
    inline def asScalaIntStream: ScalaJIntStream = jStream.mapToInt(identity).asScala

  extension (jStream: ScalaJIntStream)
    inline def close(): Unit =
      jStream.asJava.close()

    inline def isParallel: Boolean =
      jStream.asJava.isParallel

    inline def iterator: Iterator[Int] =
      jStream.asJava.iterator().asInstanceOf[JIterator[Int]].asScala

    inline def onClose(closeHandler: => Any): ScalaJIntStream =
      jStream.asJava.onClose(jRunnable(closeHandler)).asScala

    inline def parallel: ScalaJIntStream =
      jStream.asJava.parallel().asScala

    inline def sequential: ScalaJIntStream =
      jStream.asJava.sequential().asScala

    inline def unordered: ScalaJIntStream =
      jStream.asJava.unordered().asScala

    inline def allMatch(predicate: Int => Boolean): Boolean =
      jStream.asJava.allMatch(jIntPredicate(predicate))

    inline def anyMatch(predicate: Int => Boolean): Boolean =
      jStream.asJava.allMatch(jIntPredicate(predicate))

    inline def asDoubleStream: ScalaJDoubleStream =
      jStream.asJava.asDoubleStream().asScala

    inline def average: Option[Double] =
      jStream.asJava.average.asScala

    inline def boxed: ScalaJStream[Int] =
      jStream.asJava.boxed.asInstanceOf[JStream[Int]].asScala

    inline def collect[R](supplier: => R)(accumulator: (R, Int) => Any, combiner: (R, R) => Any): R =
      jStream.asJava.collect(jSupplier(supplier), jObjIntConsumer(accumulator), jBiConsumer(combiner))

    inline def count: Long =
      jStream.asJava.count

    inline def distinct: ScalaJIntStream =
      jStream.asJava.distinct.asScala

    inline def filter(predicate: Int => Boolean): ScalaJIntStream =
      jStream.asJava.filter(jIntPredicate(predicate)).asScala

    inline def findAny: Option[Int] =
      jStream.asJava.findAny().asScala

    inline def findFirst: Option[Int] =
      jStream.asJava.findFirst.asScala

    inline def flatMap(mapper: Int => ScalaJIntStream): ScalaJIntStream =
      jStream.asJava.flatMap(jIntFunction(d => mapper(d).asJava)).asScala

    inline def forEach(action: Int => Any): Unit =
      jStream.asJava.forEach(jIntConsumer(action))

    inline def forEachOrdered(action: Int => Any): Unit =
      jStream.asJava.forEachOrdered(jIntConsumer(action))

    inline def limit(maxSize: Int): ScalaJIntStream =
      jStream.asJava.limit(maxSize).asScala

    inline def map(mapper: Int => Int): ScalaJIntStream =
      jStream.asJava.map(jIntUnaryOperator(mapper)).asScala

    inline def mapToDouble(mapper: Int => Double): ScalaJDoubleStream =
      jStream.asJava.mapToDouble(jIntToDoubleFunction(mapper)).asScala

    inline def mapToLong(mapper: Int => Long): ScalaJLongStream =
      jStream.asJava.mapToLong(jIntToLongFunction(mapper)).asScala

    inline def mapToObj[U](mapper: Int => U): ScalaJStream[U] =
      jStream.asJava.mapToObj(jIntFunction(mapper)).asScala

    inline def max: Option[Int] =
      jStream.asJava.max.asScala

    inline def min: Option[Int] =
      jStream.asJava.min.asScala

    inline def noneMatch(predicate: Int => Boolean): Boolean =
      jStream.asJava.noneMatch(jIntPredicate(predicate))

    inline def peek(action: Int => Any): ScalaJIntStream =
      jStream.asJava.peek(jIntConsumer(action)).asScala

    inline def reduce(identity: Int)(op: (Int, Int) => Int): Int =
      jStream.asJava.reduce(identity, jIntBinaryOperator(op))

    inline def reduce(op: (Int, Int) => Int): Option[Int] =
      jStream.asJava.reduce(jIntBinaryOperator(op)).asScala

    inline def skip(n: Int): ScalaJIntStream =
      jStream.asJava.skip(n).asScala

    inline def sorted: ScalaJIntStream =
      jStream.asJava.sorted.asScala

    inline def sum: Int =
      jStream.asJava.sum

    inline def summaryStatistics: IntSummaryStatistics =
      jStream.asJava.summaryStatistics()

    inline def toArray: Array[Int] =
      jStream.asJava.toArray

    inline def to[C](using fac: Factory[Int, C]): C = {
      val b = fac.newBuilder
      forEachOrdered(b += _)
      b.result()
    }
  end extension

  extension (jStream: JStream[Long])
    inline def asScalaLongStream: ScalaJLongStream = jStream.mapToLong(identity).asScala

  extension (jStream: ScalaJLongStream)
    inline def close(): Unit =
      jStream.asJava.close()

    inline def isParallel: Boolean =
      jStream.asJava.isParallel

    inline def iterator: Iterator[Long] =
      jStream.asJava.iterator().asInstanceOf[JIterator[Long]].asScala

    inline def onClose(closeHandler: => Any): ScalaJLongStream =
      jStream.asJava.onClose(jRunnable(closeHandler)).asScala

    inline def parallel: ScalaJLongStream =
      jStream.asJava.parallel().asScala

    inline def sequential: ScalaJLongStream =
      jStream.asJava.sequential().asScala

    inline def unordered: ScalaJLongStream =
      jStream.asJava.unordered().asScala

    inline def allMatch(predicate: Long => Boolean): Boolean =
      jStream.asJava.allMatch(jLongPredicate(predicate))

    inline def anyMatch(predicate: Long => Boolean): Boolean =
      jStream.asJava.allMatch(jLongPredicate(predicate))

    inline def asDoubleStream: ScalaJDoubleStream =
      jStream.asJava.asDoubleStream().asScala

    inline def average: Option[Double] =
      jStream.asJava.average.asScala

    inline def boxed: ScalaJStream[Long] =
      jStream.asJava.boxed.asInstanceOf[JStream[Long]].asScala

    inline def collect[R](supplier: => R)(accumulator: (R, Long) => Any, combiner: (R, R) => Any): R =
      jStream.asJava.collect(jSupplier(supplier), jObjLongConsumer(accumulator), jBiConsumer(combiner))

    inline def count: Long =
      jStream.asJava.count

    inline def distinct: ScalaJLongStream =
      jStream.asJava.distinct.asScala

    inline def filter(predicate: Long => Boolean): ScalaJLongStream =
      jStream.asJava.filter(jLongPredicate(predicate)).asScala

    inline def findAny: Option[Long] =
      jStream.asJava.findAny().asScala

    inline def findFirst: Option[Long] =
      jStream.asJava.findFirst.asScala

    inline def flatMap(mapper: Long => ScalaJLongStream): ScalaJLongStream =
      jStream.asJava.flatMap(jLongFunction(d => mapper(d).asJava)).asScala

    inline def forEach(action: Long => Any): Unit =
      jStream.asJava.forEach(jLongConsumer(action))

    inline def forEachOrdered(action: Long => Any): Unit =
      jStream.asJava.forEachOrdered(jLongConsumer(action))

    inline def limit(maxSize: Long): ScalaJLongStream =
      jStream.asJava.limit(maxSize).asScala

    inline def map(mapper: Long => Long): ScalaJLongStream =
      jStream.asJava.map(jLongUnaryOperator(mapper)).asScala

    inline def mapToDouble(mapper: Long => Double): ScalaJDoubleStream =
      jStream.asJava.mapToDouble(jLongToDoubleFunction(mapper)).asScala

    inline def mapToInt(mapper: Long => Int): ScalaJIntStream =
      jStream.asJava.mapToInt(jLongToIntFunction(mapper)).asScala

    inline def mapToObj[U](mapper: Long => U): ScalaJStream[U] =
      jStream.asJava.mapToObj(jLongFunction(mapper)).asScala

    inline def max: Option[Long] =
      jStream.asJava.max.asScala

    inline def min: Option[Long] =
      jStream.asJava.min.asScala

    inline def noneMatch(predicate: Long => Boolean): Boolean =
      jStream.asJava.noneMatch(jLongPredicate(predicate))

    inline def peek(action: Long => Any): ScalaJLongStream =
      jStream.asJava.peek(jLongConsumer(action)).asScala

    inline def reduce(identity: Long)(op: (Long, Long) => Long): Long =
      jStream.asJava.reduce(identity, jLongBinaryOperator(op))

    inline def reduce(op: (Long, Long) => Long): Option[Long] =
      jStream.asJava.reduce(jLongBinaryOperator(op)).asScala

    inline def skip(n: Long): ScalaJLongStream =
      jStream.asJava.skip(n).asScala

    inline def sorted: ScalaJLongStream =
      jStream.asJava.sorted.asScala

    inline def sum: Long =
      jStream.asJava.sum

    inline def summaryStatistics: LongSummaryStatistics =
      jStream.asJava.summaryStatistics()

    inline def toArray: Array[Long] =
      jStream.asJava.toArray

    inline def to[C](using fac: Factory[Long, C]): C = {
      val b = fac.newBuilder
      forEachOrdered(b += _)
      b.result()
    }
  end extension

  extension (jStream: JStream[Double])
    inline def asScalaDoubleStream: ScalaJDoubleStream = jStream.mapToDouble(identity).asScala

  extension (jStream: ScalaJDoubleStream)
    inline def close(): Unit =
      jStream.asJava.close()

    inline def isParallel: Boolean =
      jStream.asJava.isParallel

    inline def iterator: Iterator[Double] =
      jStream.asJava.iterator().asInstanceOf[JIterator[Double]].asScala

    inline def onClose(closeHandler: => Any): ScalaJDoubleStream =
      jStream.asJava.onClose(jRunnable(closeHandler)).asScala

    inline def parallel: ScalaJDoubleStream =
      jStream.asJava.parallel().asScala

    inline def sequential: ScalaJDoubleStream =
      jStream.asJava.sequential().asScala

    inline def unordered: ScalaJDoubleStream =
      jStream.asJava.unordered().asScala

    inline def allMatch(predicate: Double => Boolean): Boolean =
      jStream.asJava.allMatch(jDoublePredicate(predicate))

    inline def anyMatch(predicate: Double => Boolean): Boolean =
      jStream.asJava.allMatch(jDoublePredicate(predicate))

    inline def average: Option[Double] =
      jStream.asJava.average.asScala

    inline def boxed: ScalaJStream[Double] =
      jStream.asJava.boxed.asInstanceOf[JStream[Double]].asScala

    inline def collect[R](supplier: => R)(accumulator: (R, Double) => Any, combiner: (R, R) => Any): R =
      jStream.asJava.collect(jSupplier(supplier), jObjDoubleConsumer(accumulator), jBiConsumer(combiner))

    inline def count: Long =
      jStream.asJava.count

    inline def distinct: ScalaJDoubleStream =
      jStream.asJava.distinct.asScala

    inline def filter(predicate: Double => Boolean): ScalaJDoubleStream =
      jStream.asJava.filter(jDoublePredicate(predicate)).asScala

    inline def findAny: Option[Double] =
      jStream.asJava.findAny().asScala

    inline def findFirst: Option[Double] =
      jStream.asJava.findFirst.asScala

    inline def flatMap(mapper: Double => ScalaJDoubleStream): ScalaJDoubleStream =
      jStream.asJava.flatMap(jDoubleFunction(d => mapper(d).asJava)).asScala

    inline def forEach(action: Double => Any): Unit =
      jStream.asJava.forEach(jDoubleConsumer(action))

    inline def forEachOrdered(action: Double => Any): Unit =
      jStream.asJava.forEachOrdered(jDoubleConsumer(action))

    inline def limit(maxSize: Long): ScalaJDoubleStream =
      jStream.asJava.limit(maxSize).asScala

    inline def map(mapper: Double => Double): ScalaJDoubleStream =
      jStream.asJava.map(jDoubleUnaryOperator(mapper)).asScala

    inline def mapToInt(mapper: Double => Int): ScalaJIntStream =
      jStream.asJava.mapToInt(jDoubleToIntFunction(mapper)).asScala

    inline def mapToLong(mapper: Double => Long): ScalaJLongStream =
      jStream.asJava.mapToLong(jDoubleToLongFunction(mapper)).asScala

    inline def mapToObj[U](mapper: Double => U): ScalaJStream[U] =
      jStream.asJava.mapToObj(jDoubleFunction(mapper)).asScala

    inline def max: Option[Double] =
      jStream.asJava.max.asScala

    inline def min: Option[Double] =
      jStream.asJava.min.asScala

    inline def noneMatch(predicate: Double => Boolean): Boolean =
      jStream.asJava.noneMatch(jDoublePredicate(predicate))

    inline def peek(action: Double => Any): ScalaJDoubleStream =
      jStream.asJava.peek(jDoubleConsumer(action)).asScala

    inline def reduce(identity: Double)(op: (Double, Double) => Double): Double =
      jStream.asJava.reduce(identity, jDoubleBinaryOperator(op))

    inline def reduce(op: (Double, Double) => Double): Option[Double] =
      jStream.asJava.reduce(jDoubleBinaryOperator(op)).asScala

    inline def skip(n: Long): ScalaJDoubleStream =
      jStream.asJava.skip(n).asScala

    inline def sorted: ScalaJDoubleStream =
      jStream.asJava.sorted.asScala

    inline def sum: Double =
      jStream.asJava.sum

    inline def summaryStatistics: DoubleSummaryStatistics =
      jStream.asJava.summaryStatistics()

    inline def toArray: Array[Double] =
      jStream.asJava.toArray

    inline def to[C](using fac: Factory[Double, C]): C = {
      val b = fac.newBuilder
      forEachOrdered(b += _)
      b.result()
    }
  end extension
}

object ScalaJStreamUtils extends ScalaJStreamUtils
