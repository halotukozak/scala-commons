package com.avsystem.commons
package misc

import com.avsystem.commons.annotation.MayBeReplacedWith

import scala.annotation.implicitNotFound
import scala.quoted.*

/**
  * Macro materialized typeclass which captures the single value of a singleton type.
  */
@implicitNotFound("Cannot derive value of ${T} - is not a singleton type")
@MayBeReplacedWith("scala.ValueOf${T}")
class ValueOf[T](val value: T) extends AnyVal
object ValueOf {
  @MayBeReplacedWith("scala.valueOf${T}")
  def apply[T](implicit vof: ValueOf[T]): T = vof.value

  inline given mkValueOf[T]: ValueOf[T] = ${ mkValueOfImpl[T] }
  def mkValueOfImpl[T: Type](using quotes: Quotes): Expr[ValueOf[T]] = {
    import quotes.reflect.*
    Expr.summon[scala.ValueOf[T]].map(expr => '{ new ValueOf[T]($expr.value) })
      .getOrElse {
        TypeRepr.of[T] match
          case ThisType(tpe) =>
            val value = This(tpe.typeSymbol).asExprOf[T]
            '{ new ValueOf[T]($value) }
          case _ =>
            report.errorAndAbort(s"Cannot derive ValueOf for type ${Type.show[T]} - is not a singleton type.")
      }
  }
}
