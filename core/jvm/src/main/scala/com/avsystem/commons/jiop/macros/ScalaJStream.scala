package com.avsystem.commons
package jiop.macros

import jiop.*

import com.avsystem.commons.*
import jiop.macros.scalaJStreamOps.mkScalaJsStreamSpecialized

import java.util.stream.*
import scala.compiletime.summonInline
import scala.quoted.{Expr, Quotes, Type}

inline def specializedJStreamAsScala[T, Stream <: SpecializedJStream[T] | JStream[T]]
  : AsScala[Stream, SpecializedScalaJStream[T]] =
  ${ specializedJStreamAsScalaImpl[T, Stream] }

private def specializedJStreamAsScalaImpl[T: Type, Stream <: SpecializedJStream[T] | JStream[T]: Type](using
  Quotes,
): Expr[AsScala[Stream, SpecializedScalaJStream[T]]] = {
  Type.of[Stream] match
    case '[JIntStream] => '{ new ScalaJIntStream(_: JIntStream) }
    case '[JStream[Int]] => '{ new ScalaJStream(_: JStream[Int]).asIntStream }
    case '[JLongStream] => '{ new ScalaJLongStream(_: JLongStream) }
    case '[JStream[Long]] => '{ new ScalaJStream(_: JStream[Long]).asLongStream }
    case '[JDoubleStream] => '{ new ScalaJDoubleStream(_: JDoubleStream) }
    case '[JStream[Double]] => '{ new ScalaJStream(_: JStream[Double]).asDoubleStream }
    case '[JStream[T]] => '{ specializedJStreamAsScalaAny[T, JStream[T]] }
}.asInstanceOf[Expr[AsScala[Stream, SpecializedScalaJStream[T]]]]

def specializedJStreamAsScalaAny[T, Stream <: JStream[T]]: AsScala[Stream, SpecializedScalaJStream[T]] =
  stream => mkScalaJsStreamSpecialized(new ScalaJStream(stream))

inline def specializedScalaJStreamAsJava[T, Stream <: SpecializedScalaJStream[T] | ScalaJStream[T]]
  : AsJava[Stream, JStream[T]] =
  ${ specializedScalaJStreamAsJavaImpl[T, Stream] }
def specializedScalaJStreamAsJavaImpl[T: Type, Stream <: SpecializedScalaJStream[T] | ScalaJStream[T]: Type](using
  Quotes,
): Expr[AsJava[Stream, JStream[T]]] = {
  Type.of[Stream] match
    case '[ScalaJIntStream] => '{ (_: ScalaJIntStream).asJava }
    case '[ScalaJStream[Int]] => '{ (_: ScalaJStream[Int]).asJava }
    case '[ScalaJLongStream] => '{ (_: ScalaJLongStream).asJava }
    case '[ScalaJStream[Long]] => '{ (_: ScalaJStream[Long]).asJava }
    case '[ScalaJDoubleStream] => '{ (_: ScalaJDoubleStream).asJava }
    case '[ScalaJStream[Double]] => '{ (_: ScalaJStream[Double]).asJava }
    case '[ScalaJStream[T]] => '{ (_: ScalaJStream[T]).asJava }
}.asInstanceOf[Expr[AsJava[Stream, JStream[T]]]]
