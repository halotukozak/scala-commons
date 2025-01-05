package com.avsystem.commons
package macros

import MacroUtils.*
import annotation.explicitGenerics

import scala.deriving.Mirror
import scala.quoted.{Expr, Quotes, Type}


//def caseObjectsFor[T](using tpe: Type[T])(using quotes: Quotes): Expr[List[T]] = {
//  knownSubtypes(tpe).map { subtypes =>
//    val objects = subtypes.map(subTpe => singleValueFor(subTpe)
//      .getOrElse(abort(s"All possible values of a SealedEnum must be objects but $subTpe is not")))
//    val result = Expr.ofList(objects)
//    if (tpe <:< OrderedEnumType) '{ $result.sorted } else result
//  }.getOrElse(abort(s"$tpe is not a sealed trait or class"))
//}

//  def knownSubtypes[T](tpe: Type[T], ordered: Boolean = false)(using quotes: Quotes): Option[List[Type[T]]] = {
//    import quotes.reflect.*
//
//    val dtpe = TypeRepr.of[T].dealias
//    val tpeSym: Symbol = dtpe match
//      case Refined(List(single: TypeTree), _) => single.tpe.typeSymbol
//      case _ => dtpe.typeSymbol
//
//    def sort(subclasses: List[Symbol]): List[Symbol] =
//      if (ordered || Position.ofMacroExpansion.sourceCode == tpeSym.pos.map(_.sourceCode))
//        subclasses.sortBy(sym => positionPoint(sym))
//      else subclasses
//
//    def isSealedHierarchyRoot(sym: Symbol): Boolean =
//      sym.isClassDef && sym.flags.is(Flags.Abstract & Flags.Sealed)
//
//    def knownNonAbstractSubclasses(sym: Symbol): Set[Symbol] =
//      sym.tree
//
//
//    Option(tpeSym).filter(isSealedHierarchyRoot).map { sym =>
//      sort(knownNonAbstractSubclasses(sym).toList)
//        .flatMap(subSym => determineSubtype(dtpe, subSym.asType))
//    }
//  }
