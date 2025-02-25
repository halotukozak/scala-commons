package com.avsystem.commons
package serialization.macros

import serialization.whenAbsent

import scala.util.matching.Regex

object DefaultValueMethod:
  private val DefaultValueMethodName: Regex = """(.*)\$default\$(\d+)$""".r
  private val ConstructorMethodName = "$lessinit$greater"

  def unapply(using quotes: Quotes)(symbol: quotes.reflect.Symbol): Option[quotes.reflect.Symbol] = {
    val macroUtils = new HasMacroUtils {}
    import macroUtils.*
    import quotes.reflect.*
    symbol match
      case ms if ms.isDefDef /* && ms.flags.is(Flags.Synthetic)*/ =>
        ms.name match
          case DefaultValueMethodName(name, idx) =>
            val paramIndex = idx.toInt - 1
            val ownerMethod = name match
              case `ConstructorMethodName` => ms.owner.companionClass.primaryConstructor
              case _ => ms.owner.singleMethodMember(name)

            Some(ownerMethod.paramSymss.flatten.apply(paramIndex))
          case _ => None
      case _ => None
  }

def whenAbsentValueImpl[T: Type](using quotes: Quotes): Expr[T] = {
  import quotes.reflect.*
  val param = Symbol.spliceOwner.owner match
    case DefaultValueMethod(p) => p
    case p => p

  param.getAnnotation(TypeRepr.of[whenAbsent].typeSymbol).map {
    case Apply(_, List(arg)) => arg.asExprOf[T]
    case t => report.errorAndAbort(s"unexpected tree for @whenAbsent annotation: ${t.show}")
  } getOrElse report.errorAndAbort(s"no @whenAbsent annotation found on $param of ${param.owner}")
}
