package com.avsystem.commons
package macros

import jiop.AsJava

import scala.quoted.{Expr, Quotes, Type}

def specialized[T: Type, F[_]](
  intImpl: Expr[F[Int]],
  doubleImpl: Expr[F[Double]],
  longImpl: Expr[F[Long]],
  genericImpl: Expr[F[T]],
)(using quotes: Quotes): Expr[F[T]] = {
  Type.of[T] match
    case '[Int] => intImpl
    case '[Long] => doubleImpl
    case '[Double] => longImpl
    case _ => genericImpl 
}.asInstanceOf[Expr[F[T]]]
