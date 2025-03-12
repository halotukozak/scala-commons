package com.avsystem.commons
package misc.macros

import misc.OrderedEnum

import scala.compiletime.{summonFrom, summonInline}
import scala.quoted.{Expr, Quotes, Type}

def caseObjectsForImpl[T: Type](using quotes: Quotes): Expr[List[? <: T]] = {
  import quotes.reflect.*
  knownSubtypes[T]() match
    case Some(subtypes) =>
      val objects = subtypes.map { case '[t] =>
        singleValueForImpl[t] match
          case '{ Some($value: T) } => value
          case '{ None } =>
            report.errorAndAbort(
              s"Cannot find a singleton value for ${TypeRepr.of[t].dealias.show(using Printer.TypeReprShortCode)}",
            )
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

  def symbolOf[U: Type] = TypeTree.of[U] match
    case Refined(single: TypeTree, _) => single.tpe.typeSymbol
    case Inferred() => TypeRepr.of[U].typeSymbol
    case _ => TypeTree.of[U].symbol

  def sort(subclasses: List[Type[? <: T]]): List[Type[? <: T]] =
    if ordered || Position.ofMacroExpansion.sourceCode == symbolOf[T].pos.map(_.sourceCode) then
      subclasses.sortBy(tpe => positionPoint(TypeRepr.of(using tpe).typeSymbol))
    else subclasses

  def isSealedHierarchyRoot[U: Type]: Boolean = {
    val sym = symbolOf[U]
    sym.isClassDef && sym.flags.is(Flags.Sealed)
  }

  def knownNonAbstractSubclasses[U: Type]: Set[Type[? <: U]] =
    collectKnownDirectSubtypes[U].flatMap { s =>
      if isSealedHierarchyRoot(using s) then knownNonAbstractSubclasses(using s)
      else Set(s)
    }

  Option.when(isSealedHierarchyRoot[T]) {
    sort(knownNonAbstractSubclasses[T].toList)
  }
}

inline def valueOfValueFor[T]: Option[ValueOf[T]] = ${ valueOfForImpl[T] }

private def valueOfForImpl[T: Type](using quotes: Quotes): Expr[Option[ValueOf[T]]] = {
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

inline def singleValueFor[T]: Option[T] = ${ singleValueForImpl[T] }

private def singleValueForImpl[T: Type](using quotes: Quotes): Expr[Option[T]] = {
  import quotes.reflect.*
  val tpe = TypeRepr.of[T]
  Expr.summon[ValueOf[T]] match
    case Some(value: Expr[ValueOf[T]]) => '{ Some($value.value) }
    case None if tpe.isSingleton =>
      tpe match
        case t @ ThisType(tref) =>
          val singleton = This(t.typeSymbol).asExprOf[T]
          '{ Some($singleton) }
        case _ =>
          '{ None }
    case None if tpe.isCaseObject =>
      val tref = tpe.classSymbol.get.companionModule.termRef
      val singleton = Ref.term(tref).asExprOf[T]
      '{ Some($singleton) }
    case _ =>
      '{ None }
}

def mkValueOfImpl[T: Type](using quotes: Quotes): Expr[ValueOf[T]] = {
  import quotes.reflect.*

  valueOfForImpl[T] match
    case '{ Some($value: ValueOf[T]) } => value
    case '{ None } =>
      report.errorAndAbort(s"Could not find implicitly or generate ValueOf instance for ${Type.show[T]}")
}

private def collectKnownDirectSubtypes[T: Type](using quotes: Quotes): Set[Type[? <: T]] = {
  import quotes.reflect.*
  val tpe: TypeRepr = TypeRepr.of[T]
  val children: List[Symbol] = tpe.classSymbol.get.children
  children
    .map { s =>
      val classDef = s.tree match
        case valDef: ValDef => valDef.tpt.tpe.typeSymbol.tree.asInstanceOf[ClassDef] // todo: maybe can be simplified
        case classDef: ClassDef => classDef

      val constructor = classDef.constructor

      val childType = classDef.parents
        .collect { case t: TypeTree => t.tpe }
        .collectFirst {
          case t if t <:< tpe =>
            s.typeRef.asType
          case AppliedType(parent, args) =>
            constructor.leadingTypeParams match
              case Nil =>
                s.termRef.asType
              case _ =>
                report.errorAndAbort("GADT is not currently supported")
        }
        .get

      childType
    }
    .toSet
    .asInstanceOf[Set[Type[? <: T]]]
}
