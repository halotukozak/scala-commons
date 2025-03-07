package com.avsystem.commons
package misc.macros

import misc.OrderedEnum

import scala.compiletime.{summonFrom, summonInline}
import scala.quoted.{Expr, Quotes, Type}

def valueOfObject[T: Type](using quotes: Quotes): Expr[T] = {
  import quotes.reflect.*
  TypeRepr.of[T].typeSymbol.tree.asInstanceOf[Term].asExpr.asInstanceOf[Expr[T]]
  //    .getOrElse(report.errorAndAbort(s"All possible values of a SealedEnum must be objects but ${Type.show[T]} is not"))
}

def caseObjectsForImpl[T: Type](using quotes: Quotes): Expr[List[T]] = {
  import quotes.reflect.*
  knownSubtypes[T]() match
    case Some(subtypes) =>
      val objects = subtypes.map { subTpe =>
        valueOfObject(using subTpe)
      }
      val result = Expr.ofList(objects)
      if TypeRepr.of[T] <:< TypeRepr.of[OrderedEnum] then '{ $result.sorted(using summonInline) }
      else result
    case None =>
      report.errorAndAbort(
        s"${TypeRepr.of[T].dealias.show(using Printer.TypeReprShortCode)} is not a sealed trait or class",
      )
}

def knownSubtypes[T: Type](ordered: Boolean = false)(using quotes: Quotes): Option[List[Type[? <: T]]] = {
  import quotes.reflect.*

  val dtpe = TypeTree.of[T]
  val tpeSym: Symbol = dtpe match
    case Refined(single: TypeTree, _) => single.tpe.typeSymbol
    case _ => dtpe.symbol

  def sort(subclasses: List[Symbol]): List[Symbol] =
    if ordered || Position.ofMacroExpansion.sourceCode == tpeSym.pos.map(_.sourceCode) then
      subclasses.sortBy(sym => positionPoint(sym))
    else subclasses

  def isSealedHierarchyRoot(sym: Symbol): Boolean =
    sym.isClassDef && sym.flags.is(Flags.Sealed)

  def knownNonAbstractSubclasses(sym: Symbol): Set[Symbol] =
    collectKnownSubtypes(sym.typeRef).flatMap { s =>
      if isSealedHierarchyRoot(s) then knownNonAbstractSubclasses(s) else Set(s)
    }

  Option(tpeSym).filter(isSealedHierarchyRoot).map { sym =>
    sort(knownNonAbstractSubclasses(sym).toList)
      .filter(_.typeRef <:< TypeRepr.of[T])
      .map(_.typeRef.asType.asInstanceOf[Type[? <: T]])
  }
}

inline def singleValueFor[T]: Option[ValueOf[T]] = ${ singleValueForImpl[T] }

private def singleValueForImpl[T: Type](using quotes: Quotes): Expr[Option[ValueOf[T]]] = {
  import quotes.reflect.*
  val tpe = TypeRepr.of[T]
  Expr.summon[ValueOf[T]] match
    case Some(value: Expr[ValueOf[T]]) => '{ Some($value) }
    case None if tpe.isSingleton =>
      tpe match
        case t @ ThisType(tref) =>
          val singleton = This(t.typeSymbol).asExprOf[T]
          '{ Some(scala.ValueOf($singleton)) }
        case _ =>
          '{ None }
    case None
        if tpe.typeSymbol.flags.is(Flags.Case & Flags.Final & Flags.Lazy & Flags.Module & Flags.StableRealizable) =>
      val tref = tpe.classSymbol.get.companionModule.termRef
      val singleton = Ident(tref).asExprOf[T]
      '{ Some(scala.ValueOf($singleton)) }
    case _ =>
      '{ None }
}

def mkValueOfImpl[T: Type](using quotes: Quotes): Expr[ValueOf[T]] = {
  import quotes.reflect.*

  singleValueForImpl[T] match
    case '{ Some($value: ValueOf[T]) } => value
    case '{ None } =>
      report.errorAndAbort(s"Could not find an implicitly or generate ValueOf instance for ${Type.show[T]}")
}

private def collectKnownSubtypes(using quotes: Quotes)(tpe: quotes.reflect.TypeRepr): Set[quotes.reflect.Symbol] = {
  import quotes.reflect.*
  val children: List[Symbol] = tpe.classSymbol.get.children
  children.flatMap { s =>
    if s.flags.is(Flags.Sealed & Flags.Trait) then {
      val childTpe: TypeTree = TypeIdent(s)
      val childType: TypeRepr = childTpe.tpe
      collectKnownSubtypes(using quotes)(childType) + s
    } else {
      s :: Nil
    }
  }.toSet
}
