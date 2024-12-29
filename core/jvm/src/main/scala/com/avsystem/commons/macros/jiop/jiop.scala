package com.avsystem.commons
package macros.jiop

import jiop.AsJava

import scala.quoted.{Expr, Quotes, Type}

def specializedOptionAsJava[T: Type](using quotes: Quotes): Expr[AsJava[Option[T], ReturnOfAsJava[T]]] = {
  Type.of[T] match
    case '[Int] => '{ specializedOptionAsJavaInt }
    case '[Long] => '{ specializedOptionAsJavaLong }
    case '[Double] => '{ specializedOptionAsJavaDouble }
    case _ => '{ specializedOptionAsJavaAny[T] }
}.asInstanceOf[Expr[AsJava[Option[T], ReturnOfAsJava[T]]]]

private val specializedOptionAsJavaInt: AsJava[Option[Int], JOptionalInt] = _.toJOptionalInt
private val specializedOptionAsJavaLong: AsJava[Option[Long], JOptionalLong] = _.toJOptionalLong
private val specializedOptionAsJavaDouble: AsJava[Option[Double], JOptionalDouble] = _.toJOptionalDouble
private def specializedOptionAsJavaAny[T]: AsJava[Option[T], JOptional[T]] = _.toJOptional
