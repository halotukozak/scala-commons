package com.avsystem.commons
package jiop.macros

import jiop.ScalaJStream

import scala.compiletime.summonInline
import scala.quoted.*

private[macros] object scalaJStreamOps {
  inline def mkScalaJsStreamSpecialized[T](stream: ScalaJStream[T]): SpecializedScalaJStream[T] =
    ${ mkScalaJsStreamSpecializedImpl[T]('{ stream }) }

  private def mkScalaJsStreamSpecializedImpl[T: Type](
    stream: Expr[ScalaJStream[T]],
  )(using Quotes): Expr[SpecializedScalaJStream[T]] = {
    Type.of[T] match
      case '[Int] => '{ $stream.asIntStream(using summonInline[T =:= Int]) }
      case '[Long] => '{ $stream.asLongStream(using summonInline[T =:= Long]) }
      case '[Double] => '{ $stream.asDoubleStream(using summonInline[T =:= Double]) }
      case _ => stream
  }.asInstanceOf[Expr[SpecializedScalaJStream[T]]]
}
