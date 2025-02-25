package com.avsystem.commons
package misc
package macros

import scala.collection.mutable
import scala.quoted.{Expr, Quotes, Type}

def typeStringImpl[T: Type](using quotes: Quotes): Expr[TypeString[T]] = {
  import quotes.reflect.*

  val showVariance: Flags => String =
    case f if f.is(Flags.Covariant) => "+"
    case f if f.is(Flags.Contravariant) => "-"
    case _ => ""

  val history = mutable.ListBuffer.newBuilder[String]
  history.addOne("Start")

  val simpleName: TypeRepr => String = _.show(using Printer.TypeReprShortCode)

  def mkName(
    tpe: TypeRepr,
    param: Int => TypeRepr = _ => report.errorAndAbort(history.result().mkString("->")),
  ): String = tpe match
    case ConstantType(value) =>
      history.addOne("Constant")
      value.toString
    case tpe: TermRef =>
      def generatePath: TypeRepr => List[String] =
        case TermRef(qual, name) if qual.typeSymbol.isPackageDef || qual =:= TypeRepr.of[Predef.type] => name :: Nil
        case TermRef(qual, name) => generatePath(qual) ::: (name :: Nil)
        case _ => Nil

      history.addOne("TermRef")
      s"${generatePath(tpe).mkString(".")}.type"
    case tpe if tpe.isSingleton =>
      history.addOne("Singleton")
      s"${simpleName(tpe)}.type"
    case Refinement(parent, name, info) =>
      val parentName = if parent =:= TypeRepr.of[Object] then "" else s"${mkName(parent)} "

      history.addOne("Refinement")
      info match
        case TypeBounds(low @ TypeLambda(paramNames, paramBounds, resType), hi) if low =:= hi =>
          history.addOne("Refinement.TypeBounds1")
          s"$parentName{type $name[${paramNames.zip(low.paramVariances).map((p, v) => s"${showVariance(v)}$p").mkString(", ")}] = ${mkName(resType, low.param)}}"
        case TypeBounds(low @ MethodType(paramNames, paramBounds, resType), hi) =>
          history.addOne("Refinement.TypeBounds1.1")
          s"$parentName{type $name(${paramNames.zip(paramBounds).map((p, t) => s"$p: ${mkName(t)}").mkString(", ")}) = ${mkName(resType)}}"
        case TypeBounds(low, hi) if low =:= hi =>
          history.addOne("Refinement.TypeBounds2")
          s"$parentName{type $name = ${mkName(low)}}"
        case TypeBounds(low, hi) =>
          val lowName = if low =:= TypeRepr.of[Nothing] then "" else s" >: ${mkName(low)}"
          val hiName = if hi =:= TypeRepr.of[Any] then "" else s" <: ${mkName(hi)}"
          history.addOne("Refinement.TypeBounds3")
          s"$parentName{type $name$lowName$hiName}"

        case MethodType(paramNames, paramBounds, resType) =>
          history.addOne("Refinement.MethodType")
          s"$parentName{def $name(${paramNames.zip(paramBounds).map((p, t) => s"$p: ${mkName(t)}").mkString(", ")}): ${mkName(resType)}}"
        case PolyType(paramNames, paramBounds, resType) => ???
        case ByNameType(tpe) =>
          history.addOne("Refinement.ByName")
          s"$parentName{def $name: ${mkName(tpe)}}"
        case other =>
          history.addOne("Refinement.Other")
          s"$parentName{type $name = ${mkName(other)}}"
    case tpe @ AppliedType(t, args) if tpe.isFunctionType =>
      val preparedArgs = args.init match
        case ByNameType(head) :: Nil =>
          history.addOne("FunctionType.ByName")
          s"(=> ${mkName(head)})"
        case (head: AppliedType) :: Nil =>
          history.addOne("FunctionType.AppliedType")
          s"(${mkName(head)})"
        case head :: Nil =>
          history.addOne("FunctionType.Head")
          mkName(head)
        case args =>
          history.addOne("FunctionType.Args")
          args.map(mkName(_)).mkString("(", ", ", ")")

      history.addOne("FunctionType")
      s"$preparedArgs => ${mkName(args.last)}"
    case tpe @ AppliedType(t, args) if t <:< defn.RepeatedParamClass.typeRef =>
      history.addOne("RepeatedParam")
      if args.length != 1 then report.errorAndAbort("Repeated param must have exactly one argument")
      s"${mkName(args.head)}*"
    case tpe @ AppliedType(t, args) if tpe.isTupleN =>
      history.addOne("Tuple")
      s"(${args.map(mkName(_)).mkString(", ")})"
    case AppliedType(t, args) =>
      history.addOne("Applied")
      s"${mkName(t, param)}[${args.map(mkName(_, param)).mkString(", ")}]"
    case AnnotatedType(t, annot) =>
      ???
    case AndType(t, u) =>
      history.addOne("And")
      s"${mkName(t)} & ${mkName(u)}"
    case OrType(t, u) =>
      history.addOne("Or")
      s"${mkName(t)} | ${mkName(u)}"
    case MatchType(bound, scrutinee, cases) =>
      history.addOne("Match")
      s"${mkName(scrutinee)} match {${cases.map(mkName(_)).mkString(" | ")}}"
    case ByNameType(tpe) =>
      history.addOne("ByName")
      s"=> ${mkName(tpe)}"
    case ParamRef(binder, idx) =>
      history.addOne("ParamRef")
      s"${simpleName(param(idx))}"
    case RecursiveThis(_) =>
      history.addOne("RecursiveThis")
      report.errorAndAbort("Recursive types are not supported")
    case MethodType(paramNames, paramTypes, resType) =>
      s"def (${paramNames.zip(paramTypes).map((n, t) => s"$n: ${mkName(t)}").mkString(", ")}) => ${mkName(resType)}"
    case PolyType(paramNames, paramBounds, resType) =>
      report.errorAndAbort("Poly types are not supported")
    //      s"[${paramNames.zip(paramBounds).map((n, t) => s"${n}: ${mkName(t)}").mkString(", ")}] => ${mkName(resType)}"
    case tpe @ TypeLambda(paramNames, paramBounds, resType) =>
      history.addOne("TypeLambda")
      s"[${paramNames.zip(paramBounds).map((n, t) => s"$n: ${mkName(t)}").mkString(", ")}] => ${mkName(resType, tpe.param)}"
    case MatchCase(caseDef, body) =>
      history.addOne("MatchCase")
      report.errorAndAbort("Match cases are not supported")
    case TypeBounds(low, hi) =>
      val left = if low =:= TypeRepr.of[Nothing] then "?" else s"${mkName(low)}"
      val right = if hi =:= TypeRepr.of[Any] then "" else s" <: ${mkName(hi)}"
      history.addOne("TypeBounds")
      s"$left$right"
    case NoPrefix() =>
      history.addOne("NoPrefix")
      report.errorAndAbort("No prefix is not supported")
    case t =>
      history.addOne("Other")
      simpleName(t)

  val name = mkName(TypeRepr.of[T])
//  report.info(history.result().mkString("->"))
  val tpe = Expr(name)
  '{ new TypeString(${ tpe }) }
}
