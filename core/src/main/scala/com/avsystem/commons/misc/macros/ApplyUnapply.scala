package com.avsystem.commons
package misc.macros

import scala.Tuple.Elem
import scala.compiletime.summonFrom
import scala.deriving.Mirror

/**
 * @param apply
 *   case class constructor or companion object's apply method
 * @param unapply
 *   companion object'a unapply method or `NoSymbol` for case class with more than 22 fields
 * @param params
 *   parameters with trees evaluating to default values (or `EmptyTree`s)
 */
final case class ApplyUnapply[T](ownerTpe: Type[T], params: Seq[ApplyUnapply.Param])(using val m: Mirror.ProductOf[T]) {

  def standardCaseClass: Boolean = ???
  //      apply.isClassConstructor

  def synthetic: Boolean = ???
  //      apply.isClassConstructor || apply.isSynthetic && unapply.isSynthetic

  def defaultValueFor(idx: Int) =
    params(idx).default.asInstanceOf[Elem[m.MirroredElemTypes, idx.type]]

  def mkApply(args: Seq[Any]): T =
    m.fromProduct(Tuple.fromArray(args.toArray))
}

object ApplyUnapply:
  final case class Param(
    label: String,
    index: Int,
    tpe: Type[? <: AnyKind],
    repeated: Boolean,
    default: Option[Expr[() => Any]],
  )

inline def applyUnapplyFor[T](using quotes: Quotes): Option[ApplyUnapply[T]] = {
  import quotes.reflect.*
  Expr.summon[Mirror.ProductOf[T]].flatMap { x =>
    x.asTerm.info
    None
  }
}

//  summonFrom {
//  case given Mirror.ProductOf[T] =>
//    ???
////    val params =
////      types[m.MirroredElemTypes].zip(labels[T]).zip(repeated[T]).zip(defaultValue[T]).zipWithIndex.map {
////        case ((((tpe, label), repeated), default), idx) =>
////          ApplyUnapply.Param(label, idx, tpe, repeated, default)
////      }
////    ApplyUnapply(typeOf[T], params)
//  case _ => None
//}
