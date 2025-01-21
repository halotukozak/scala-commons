package com.avsystem.commons
package misc
package macros

import scala.annotation.tailrec
import scala.quoted.{Expr, Quotes, Type}

def typeStringImpl[T: Type](using quotes: Quotes): Expr[TypeString[T]] = {
  import quotes.reflect.*

  def mkName: TypeRepr => String =
    case ConstantType(value) =>
      value.toString
    case ThisType(t) =>
      s"${t.typeSymbol.name}.type"
    //    case tpe if tpe.isSingleton =>
    //      s"${tpe.show(using Printer.TypeReprShortCode)}.type"
    case tpe: TermRef =>
      def generatePath: TypeRepr => List[String] =
        case TermRef(qual, name) => generatePath(qual) ::: (name :: Nil)
        case _ => Nil

      s"${generatePath(tpe).mkString(".")}.type"
    case Refinement(parent, name, TypeBounds(low, hi)) if low =:= TypeRepr.of[Nothing] =>
      s"${mkName(parent)} {type $name <: ${mkName(hi)}}"
    case Refinement(parent, name, info) =>
      s"${mkName(parent)} {type $name: ${mkName(info)}}"
    case tpe@AppliedType(t, args) if tpe.isFunctionType =>
      val preparedArgs = args.init match
        case ByNameType(head) :: Nil => s"(=> ${mkName(head)})"
        case (head: AppliedType) :: Nil => s"(${mkName(head)})"
        case head :: Nil => mkName(head)
        case args => args.map(mkName).mkString("(", ", ", ")")

      s"$preparedArgs => ${mkName(args.last)}"
    case tpe@AppliedType(t, args) if tpe.isTupleN =>
      s"(${args.map(mkName).mkString(", ")})"
    case AppliedType(t, args) =>
      s"${mkName(t)}[${args.map(mkName).mkString(", ")}]"
    case AnnotatedType(t, annot) =>
      ???
    case AndType(t, u) =>
      s"${mkName(t)} & ${mkName(u)}"
    case OrType(t, u) =>
      s"${mkName(t)} | ${mkName(u)}"
    case MatchType(bound, scrutinee, cases) =>
      s"${mkName(scrutinee)} match {${cases.map(mkName).mkString(" | ")}}"
    case ByNameType(tpe) =>
      s"=> ${mkName(tpe)}"
    case ParamRef(binder, idx) =>
      s"${binder.typeSymbol.name}[${idx}]"
    case RecursiveThis(_) =>
      ???
    case MethodType(paramNames, paramTypes, resType) =>
      s"(${paramNames.zip(paramTypes).map((n, t) => s"${n}: ${mkName(t)}").mkString(", ")}) => ${mkName(resType)}"
    case PolyType(paramNames, paramBounds, resType) =>
      s"[${paramNames.zip(paramBounds).map((n, t) => s"${n}: ${mkName(t)}").mkString(", ")}] => ${mkName(resType)}"
    case TypeLambda(paramNames, paramBounds, resType) =>
      s"[${paramNames.zip(paramBounds).map((n, t) => s"${n}: ${mkName(t)}").mkString(", ")}] => ${mkName(resType)}"
    case MatchCase(caseDef, body) =>
      ???
    case TypeBounds(low, hi) if low =:= TypeRepr.of[Nothing] =>
      val left = if low =:= TypeRepr.of[Nothing] then "?" else s"${mkName(low)}"
      val right = if hi =:= TypeRepr.of[Any] then "" else s" <: ${mkName(hi)}"
      s"$left$right"
    case TypeBounds(low, hi) =>
      s"${mkName(low)} <: ${mkName(hi)}"
    case NoPrefix() =>
      ???
    case t =>
      printTypeReprInfo(t)
      //      printSymbolInfo(t.typeSymbol)
      t.typeSymbol.name

  val tpe = Expr(mkName(TypeRepr.of[T]))
  '{ new TypeString(${ tpe }) }
}
