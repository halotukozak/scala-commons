package com.avsystem.commons
package macros

import scala.compiletime.summonInline
import scala.deriving.Mirror
import scala.quoted.{Expr, Quotes, Type}

//private def listOfTypesToTupleType(types: List[Type[?]])(using quotes: Quotes): Type[Tuple] = {
//  import quotes.reflect.*
//  val tupleType = types.foldRight[Type](Type.of[EmptyTuple])((tpe, acc) => Type.of[tpe.T *: acc.T])
//  Type.of[tupleType.T]
//}

//def testKnownSubtypesImpl[T: Mirror.ProductOf, R: Type](using quotes: Quotes): Expr[Unit] = {
//  import quotes.reflect.*
//  val computedResultType = knownSubtypes[T]
//    .map(listOfTypesToTupleType)
//    .getOrElse(TypeRepr.of[Nothing])
//
//  '{ assert(TypeRepr.of[R] =:= computedResultType) }
//}

inline def assertEquals[T, R]: Unit = ${ assertEqualsImpl[T, R] }
def assertEqualsImpl[T: Type, R: Type](using quotes: Quotes): Expr[Unit] = {
  import quotes.reflect.*
  val result = Expr(TypeRepr.of[T] =:= TypeRepr.of[R])
  '{ assert($result) }
}