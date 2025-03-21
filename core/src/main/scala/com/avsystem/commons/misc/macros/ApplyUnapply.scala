package com.avsystem.commons
package misc.macros

import scala.Tuple.Elem
import scala.annotation.tailrec
import scala.compiletime.{summonFrom, summonInline}
import scala.deriving.Mirror

/**
 * @param apply
 *   case class constructor or companion object's apply method
 * @param unapply
 *   companion object'a unapply method or `NoSymbol` for case class with more than 22 fields
 * @param params
 *   parameters with trees evaluating to default values (or `EmptyTree`s)
 */
final case class ApplyUnapply[T](ownerTpe: Type[T], params: Seq[ApplyUnapply.Param]) {

  def standardCaseClass: Boolean = ???
  //      apply.isClassConstructor

  def synthetic: Boolean = ???
  //      apply.isClassConstructor || apply.isSynthetic && unapply.isSynthetic

  def defaultValueFor(idx: Int) = ???
//    params(idx).default.asInstanceOf[Option[Expr[Elem[m.MirroredElemTypes, idx.type]]]]

  def mkApply(args: Seq[Any]): T = ???
//    m.fromProduct(Tuple.fromArray(args.toArray))
}

object ApplyUnapply:
  final case class Param(
    label: String,
    index: Int,
    tpe: Type[? <: AnyKind],
    repeated: Boolean,
    default: Option[Expr[() => Any]],
  )

def applyUnapplyForImpl[T: Type](using quotes: Quotes): Option[ApplyUnapply[T]] = {
  import quotes.reflect.*
  val tpe = TypeRepr.of[T]

  val comp = tpe.classSymbol.get.companionModule
  val ts = tpe.dealias.typeSymbol
  val isCaseClass = ts.isClassDef && ts.flags.is(Flags.Case)

  def isFirstListVarargs(sym: Symbol) =
    sym.paramSymss.headOption.flatMap(_.lastOption).exists(_.isRepeatedParam)

  def defaultValue(i: Int): Option[Expr[() => Any]] = {
    val defaultMethodName = s"$$lessinit$$greater$$default$$${i + 1}"
    comp
      .declaredMethod(defaultMethodName)
      .headOption
      .map { defaultMethod =>
        def callDefault = {
          val base = Ident(comp.termRef).select(defaultMethod)
          val tParams = defaultMethod.paramSymss.headOption.filter(_.forall(_.isType))
          tParams match
            case Some(tParams) => TypeApply(base, tParams.map(TypeTree.ref))
            case _ => base
        }

        defaultMethod.tree match
          case tree: DefDef => tree.rhs.getOrElse(callDefault)
          case _ => callDefault
      }
      .map(_.asExprOf[Any])
      .map(expr => '{ () => $expr })
  }

  def params(methodSig: DefDef): List[ApplyUnapply.Param] =
    methodSig.termParamss.flatMap(_.params).zipWithIndex.map { case (param, idx) =>
      ApplyUnapply.Param(param.name, idx, param.tpt.tpe.widen.asType, param.symbol.isRepeatedParam, defaultValue(idx))
    }

  def typeParamsMatch(apply: Symbol, unapply: Symbol) = {
    val expected = tpe.typeArgs.length
    apply.typeRef.typeArgs.length == expected && unapply.typeRef.typeArgs.length == expected
  }

  val applyUnapplyPairs: List[(Symbol, Symbol)] =
    if comp == TypeRepr.of[Seq.type].typeSymbol then Nil
    else
      for
        apply <- comp.methodMember("apply")
        unapplyName = if isFirstListVarargs(apply) then "unapplySeq" else "unapply"
        unapply <- comp.methodMember(unapplyName)
      yield (apply, unapply)

  val applicableResults = applyUnapplyPairs.flatMap:
    case (apply, unapply) if isCaseClass && apply.isSynthetic && unapply.isSynthetic =>
      val constructor = tpe.classSymbol.get.primaryConstructor
      Some(ApplyUnapply(Type.of[T], params(constructor.tree.asInstanceOf[DefDef])))
    case (apply, unapply) if typeParamsMatch(apply, unapply) =>
      Some(ApplyUnapply(Type.of[T], params(apply.tree.asInstanceOf[DefDef])))
    case _ => None

  @tailrec def choose(results: List[ApplyUnapply[T]]): Option[ApplyUnapply[T]] = results match
    case Nil => None
    case result :: Nil => Some(result)
    case multiple if multiple.exists(_.synthetic) =>
      // prioritize non-synthetic apply/unapply pairs
      choose(multiple.filterNot(_.synthetic))
    case _ => None

  choose(applicableResults)
}
