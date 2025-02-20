package com.avsystem.commons
package misc

import scala.quoted.{Expr, Quotes, Type}

import scala.annotation.implicitNotFound

object ValueOf:
  def apply[T](using vof: ValueOf[T]): T = vof.value

  inline given mkValueOf[T]: ValueOf[T] = ${ singleValueForImpl[T] }

def singleValueForImpl[T: Type](using quotes: Quotes): Expr[ValueOf[T]] = {
  import quotes.reflect.*
  val tpe = TypeRepr.of[T]
  val value = Expr.summon[ValueOf[T]] match
    case Some(expr: Expr[ValueOf[T]]) => expr
    case None if tpe.isSingleton =>
      tpe match
        case t @ ThisType(tref) =>
          val singleton = This(t.typeSymbol).asExprOf[T]
          '{ scala.ValueOf($singleton) }
        case _ =>
          report.errorAndAbort(s"Cannot find a value of type ${tpe.show}")
    case None => report.errorAndAbort(s"Cannot find a value of type ${tpe.show}")
  value
}
