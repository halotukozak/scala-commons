package com.avsystem.commons
package jiop

import com.avsystem.commons.annotation.MayBeReplacedWith

import java.util.function as juf
import com.avsystem.commons.misc.Sam

/**
  * Utils to convert Scala functions and expressions to most common Java functional interfaces.
  */
trait JFunctionUtils {
  type JBiConsumer[T, U] = juf.BiConsumer[T, U]
  type JBiFunction[T, U, R] = juf.BiFunction[T, U, R]
  type JBiPredicate[T, U] = juf.BiPredicate[T, U]
  type JBinaryOperator[T] = juf.BinaryOperator[T]
  type JBooleanSupplier = juf.BooleanSupplier
  type JConsumer[T] = juf.Consumer[T]
  type JDoubleBinaryOperator = juf.DoubleBinaryOperator
  type JDoubleConsumer = juf.DoubleConsumer
  type JDoubleFunction[R] = juf.DoubleFunction[R]
  type JDoublePredicate = juf.DoublePredicate
  type JDoubleSupplier = juf.DoubleSupplier
  type JDoubleToIntFunction = juf.DoubleToIntFunction
  type JDoubleToLongFunction = juf.DoubleToLongFunction
  type JDoubleUnaryOperator = juf.DoubleUnaryOperator
  type JFunction[T, R] = juf.Function[T, R]
  type JIntBinaryOperator = juf.IntBinaryOperator
  type JIntConsumer = juf.IntConsumer
  type JIntFunction[R] = juf.IntFunction[R]
  type JIntPredicate = juf.IntPredicate
  type JIntSupplier = juf.IntSupplier
  type JIntToDoubleFunction = juf.IntToDoubleFunction
  type JIntToLongFunction = juf.IntToLongFunction
  type JIntUnaryOperator = juf.IntUnaryOperator
  type JLongBinaryOperator = juf.LongBinaryOperator
  type JLongConsumer = juf.LongConsumer
  type JLongFunction[R] = juf.LongFunction[R]
  type JLongPredicate = juf.LongPredicate
  type JLongSupplier = juf.LongSupplier
  type JLongToDoubleFunction = juf.LongToDoubleFunction
  type JLongToIntFunction = juf.LongToIntFunction
  type JLongUnaryOperator = juf.LongUnaryOperator
  type JObjDoubleConsumer[T] = juf.ObjDoubleConsumer[T]
  type JObjIntConsumer[T] = juf.ObjIntConsumer[T]
  type JObjLongConsumer[T] = juf.ObjLongConsumer[T]
  type JPredicate[T] = juf.Predicate[T]
  type JSupplier[T] = juf.Supplier[T]
  type JToDoubleBiFunction[T, U] = juf.ToDoubleBiFunction[T, U]
  type JToDoubleFunction[T] = juf.ToDoubleFunction[T]
  type JToIntBiFunction[T, U] = juf.ToIntBiFunction[T, U]
  type JToIntFunction[T] = juf.ToIntFunction[T]
  type JToLongBiFunction[T, U] = juf.ToLongBiFunction[T, U]
  type JToLongFunction[T] = juf.ToLongFunction[T]
  type JUnaryOperator[T] = juf.UnaryOperator[T]

  @MayBeReplacedWith("code(_,_)")
  def jBiConsumer[T, U](code: (T, U) => Any) = Sam[JBiConsumer[T, U]](code)
  @MayBeReplacedWith("fun(_,_)")
  def jBiFunction[T, U, R](fun: (T, U) => R) = Sam[JBiFunction[T, U, R]](fun)
  @MayBeReplacedWith("pred(_,_)")
  def jBiPredicate[T, U](pred: (T, U) => Boolean) = Sam[JBiPredicate[T, U]](pred)
  @MayBeReplacedWith("op(_,_)")
  def jBinaryOperator[T](op: (T, T) => T) = Sam[JBinaryOperator[T]](op)
  @MayBeReplacedWith("() => expr")
  def jBooleanSupplier(expr: => Boolean) = Sam[JBooleanSupplier](expr)
  @MayBeReplacedWith("code")
  def jConsumer[T](code: T => Any) = Sam[JConsumer[T]](code)
  @MayBeReplacedWith("op(_,_)")
  def jDoubleBinaryOperator(op: (Double, Double) => Double) = Sam[JDoubleBinaryOperator](op)
  @MayBeReplacedWith("code")
  def jDoubleConsumer(code: Double => Any) = Sam[JDoubleConsumer](code)
  @MayBeReplacedWith("fun")
  def jDoubleFunction[R](fun: Double => R) = Sam[JDoubleFunction[R]](fun)
  @MayBeReplacedWith("pred")
  def jDoublePredicate(pred: Double => Boolean) = Sam[JDoublePredicate](pred)
  @MayBeReplacedWith("() => expr")
  def jDoubleSupplier(expr: => Double) = Sam[JDoubleSupplier](expr)
  @MayBeReplacedWith("fun")
  def jDoubleToIntFunction(fun: Double => Int) = Sam[JDoubleToIntFunction](fun)
  @MayBeReplacedWith("fun")
  def jDoubleToLongFunction(fun: Double => Long) = Sam[JDoubleToLongFunction](fun)
  @MayBeReplacedWith("op")
  def jDoubleUnaryOperator(op: Double => Double) = Sam[JDoubleUnaryOperator](op)
  @MayBeReplacedWith("fun")
  def jFunction[T, R](fun: T => R) = Sam[JFunction[T, R]](fun)
  @MayBeReplacedWith("op(_,_)")
  def jIntBinaryOperator(op: (Int, Int) => Int) = Sam[JIntBinaryOperator](op)
  @MayBeReplacedWith("code")
  def jIntConsumer(code: Int => Any) = Sam[JIntConsumer](code)
  @MayBeReplacedWith("fun")
  def jIntFunction[R](fun: Int => R) = Sam[JIntFunction[R]](fun)
  @MayBeReplacedWith("pred")
  def jIntPredicate(pred: Int => Boolean) = Sam[JIntPredicate](pred)
  @MayBeReplacedWith("() => expr")
  def jIntSupplier(expr: => Int) = Sam[JIntSupplier](expr)
  @MayBeReplacedWith("fun")
  def jIntToDoubleFunction(fun: Int => Double) = Sam[JIntToDoubleFunction](fun)
  @MayBeReplacedWith("fun")
  def jIntToLongFunction(fun: Int => Long) = Sam[JIntToLongFunction](fun)
  @MayBeReplacedWith("op")
  def jIntUnaryOperator(op: Int => Int) = Sam[JIntUnaryOperator](op)
  @MayBeReplacedWith("op(_,_)")
  def jLongBinaryOperator(op: (Long, Long) => Long) = Sam[JLongBinaryOperator](op)
  @MayBeReplacedWith("code")
  def jLongConsumer(code: Long => Any) = Sam[JLongConsumer](code)
  @MayBeReplacedWith("fun")
  def jLongFunction[R](fun: Long => R) = Sam[JLongFunction[R]](fun)
  @MayBeReplacedWith("pred")
  def jLongPredicate(pred: Long => Boolean) = Sam[JLongPredicate](pred)
  @MayBeReplacedWith("() => expr")
  def jLongSupplier(expr: => Long) = Sam[JLongSupplier](expr)
  @MayBeReplacedWith("fun")
  def jLongToDoubleFunction(fun: Long => Double) = Sam[JLongToDoubleFunction](fun)
  @MayBeReplacedWith("fun")
  def jLongToIntFunction(fun: Long => Int) = Sam[JLongToIntFunction](fun)
  @MayBeReplacedWith("op")
  def jLongUnaryOperator(op: Long => Long) = Sam[JLongUnaryOperator](op)
  @MayBeReplacedWith("code(_,_)")
  def jObjDoubleConsumer[T](code: (T, Double) => Any) = Sam[JObjDoubleConsumer[T]](code)
  @MayBeReplacedWith("code(_,_)")
  def jObjIntConsumer[T](code: (T, Int) => Any) = Sam[JObjIntConsumer[T]](code)
  @MayBeReplacedWith("code(_,_)")
  def jObjLongConsumer[T](code: (T, Long) => Any) = Sam[JObjLongConsumer[T]](code)
  @MayBeReplacedWith("pred")
  def jPredicate[T](pred: T => Boolean) = Sam[JPredicate[T]](pred)
  @MayBeReplacedWith("() => expr")
  def jSupplier[T](expr: => T) = Sam[JSupplier[T]](expr)
  @MayBeReplacedWith("fun(_,_)")
  def jToDoubleBiFunction[T, U](fun: (T, U) => Double) = Sam[JToDoubleBiFunction[T, U]](fun)
  @MayBeReplacedWith("fun")
  def jToDoubleFunction[T](fun: T => Double) = Sam[JToDoubleFunction[T]](fun)
  @MayBeReplacedWith("fun(_,_)")
  def jToIntBiFunction[T, U](fun: (T, U) => Int) = Sam[JToIntBiFunction[T, U]](fun)
  @MayBeReplacedWith("fun")
  def jToIntFunction[T](fun: T => Int) = Sam[JToIntFunction[T]](fun)
  @MayBeReplacedWith("fun(_,_)")
  def jToLongBiFunction[T, U](fun: (T, U) => Long) = Sam[JToLongBiFunction[T, U]](fun)
  @MayBeReplacedWith("fun(_)")
  def jToLongFunction[T](fun: T => Long) = Sam[JToLongFunction[T]](fun)
  @MayBeReplacedWith("op(_)")
  def jUnaryOperator[T](op: T => T) = Sam[JUnaryOperator[T]](op)
}
