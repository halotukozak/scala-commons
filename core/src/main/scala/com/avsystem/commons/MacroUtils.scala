package com.avsystem.commons

import annotation.positioned
import misc.macros.MacroCommons
import serialization.optionalParam

import scala.collection.mutable
import scala.quoted.*

trait HasMacroUtils extends MacroCommons {

  extension (using quotes: Quotes)(any: Any) {
    inline def info: any.type = {
      quotes.reflect.report.info(s"${any}")
      any
    }
  }

  inline final def printTree[T](inline x: T): T = ${ PrintTree.printTreeImpl('{ x }) }

  final def printSymbolInfo(using quotes: Quotes)(symbol: quotes.reflect.Symbol): Unit = quotes.reflect.report.info {
    s"""
       |maybeOwner: ${symbol.maybeOwner}
       |flags: ${symbol.flags.show}
       |privateWithin: ${symbol.privateWithin.map(_.show)}
       |protectedWithin: ${symbol.protectedWithin.map(_.show)}
       |name: ${symbol.name}
       |fullName: ${symbol.fullName}
       |pos: ${symbol.pos}
       |docstring: ${symbol.docstring}
       |tree: ${Try(symbol.tree.show) getOrElse "no tree"}
       |annotations: ${symbol.annotations.map(_.show)}
       |isDefinedInCurrentRun: ${symbol.isDefinedInCurrentRun}
       |isLocalDummy: ${symbol.isLocalDummy}
       |isRefinementClass: ${symbol.isRefinementClass}
       |isAliasType: ${symbol.isAliasType}
       |isAnonymousClass: ${symbol.isAnonymousClass}
       |isAnonymousFunction: ${symbol.isAnonymousFunction}
       |isAbstractType: ${symbol.isAbstractType}
       |isClassConstructor: ${symbol.isClassConstructor}
       |isSuperAccessor: ${symbol.isSuperAccessor}
       |isType: ${symbol.isType}
       |isTerm: ${symbol.isTerm}
       |isPackageDef: ${symbol.isPackageDef}
       |isClassDef: ${symbol.isClassDef}
       |isTypeDef: ${symbol.isTypeDef}
       |isValDef: ${symbol.isValDef}
       |isDefDef: ${symbol.isDefDef}
       |isBind: ${symbol.isBind}
       |isNoSymbol: ${symbol.isNoSymbol}
       |exists: ${symbol.exists}
       |declaredFields: ${symbol.declaredFields}
       |fieldMembers: ${symbol.fieldMembers}
       |declaredMethods: ${symbol.declaredMethods}
       |methodMembers: ${symbol.methodMembers}
       |declaredTypes: ${symbol.declaredTypes}
       |typeMembers: ${symbol.typeMembers}
       |declarations: ${symbol.declarations}
       |paramSymss: ${symbol.paramSymss}
       |allOverriddenSymbols: ${symbol.allOverriddenSymbols}
       |primaryConstructor: ${symbol.primaryConstructor}
       |caseFields: ${symbol.caseFields}
       |isTypeParam: ${symbol.isTypeParam}
       |paramVariance: ${symbol.paramVariance.show}
       |signature: ${symbol.signature}
       |moduleClass: ${symbol.moduleClass}
       |companionClass: ${symbol.companionClass}
       |companionModule: ${symbol.companionModule}
       |children: ${symbol.children}
       |typeRef: ${Try(symbol.typeRef.show) getOrElse "no typeRef"}
       |termRef: ${Try(symbol.termRef.show) getOrElse "no termRef"}
       |""".stripMargin
  }

  final def printTypeReprInfo(using quotes: Quotes)(tpe: quotes.reflect.TypeRepr): Unit = quotes.reflect.report.info {
    s"""
       |type: ${tpe.show}
       |widen: ${tpe.widen}
       |widenTermRefByName: ${tpe.widenTermRefByName}
       |widenByName: ${tpe.widenByName}
       |dealias: ${tpe.dealias}
       |dealiasKeepOpaques: ${tpe.dealiasKeepOpaques}
       |simplified: ${tpe.simplified}
       |classSymbol: ${tpe.classSymbol}
       |typeSymbol: ${tpe.typeSymbol}
       |termSymbol: ${tpe.termSymbol}
       |isSingleton: ${tpe.isSingleton}
       |baseClasses: ${tpe.baseClasses}
       |isFunctionType: ${tpe.isFunctionType}
       |isContextFunctionType: ${tpe.isContextFunctionType}
       |isErasedFunctionType: ${tpe.isErasedFunctionType}
       |isDependentFunctionType: ${tpe.isDependentFunctionType}
       |isTupleN: ${tpe.isTupleN}
       |typeArgs: ${tpe.typeArgs}
       |""".stripMargin
  }
}

object HasMacroUtils extends HasMacroUtils

object PrintTree {
  def printTreeImpl[T: Type](x: Expr[T])(using quotes: Quotes): Expr[T] = {
    import quotes.reflect.*

    println("Structure " + x.asTerm.show(using Printer.TreeStructure))
    println("ShortCode " + x.asTerm.show(using Printer.TreeShortCode))
    x
  }
}
