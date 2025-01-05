package com.avsystem.commons
package jiop.macros

import scala.quoted.{Expr, Quotes, Type}

def specializedMacro[T: Type, F[_]](
  intImpl: Expr[F[Int]],
  longImpl: Expr[F[Long]],
  doubleImpl: Expr[F[Double]],
  genericImpl: Expr[F[T]],
)(using quotes: Quotes): Expr[F[T]] = {
  Type.of[T] match
    case '[Int] => intImpl
    case '[Long] => doubleImpl
    case '[Double] => longImpl
    case _ => genericImpl
}.asInstanceOf[Expr[F[T]]]
