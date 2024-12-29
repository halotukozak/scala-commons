package com.avsystem.commons
package macros

import scala.quoted.*

trait MacroUtils {

  extension (any: Any)(using quotes: Quotes) {
    inline def info: any.type = {
      import quotes.reflect.*

      report.info(s"${any}")
      any
    }
  }

  extension (using quotes: Quotes)(flags: quotes.reflect.Flags) {
    def isAnyOf(other: quotes.reflect.Flags*): Boolean = other.foldLeft(false)(_ || flags.is(_))
  }
  extension (using quotes: Quotes)(symbol: quotes.reflect.Symbol) {
    def singleMethodMember(other: String): quotes.reflect.Symbol = {
      import quotes.reflect.*

      symbol.methodMember(other) match
        case Nil => report.errorAndAbort(s"Symbol $symbol does not have a single method member $other")
        case e :: Nil => e
        case _ => report.errorAndAbort(s"Symbol $symbol has multiple method members $other")
    }
  }

  inline def printTree[T](inline x: T): T = ${ PrintTree.printTreeImpl('{ x }) }
}

object MacroUtils extends MacroUtils

object PrintTree {

  def printTreeImpl[T: Type](x: Expr[T])(using qctx: Quotes): Expr[T] =
    import qctx.reflect.*
    println("Structure " + x.asTerm.show(using Printer.TreeStructure))
    println("ShortCode " + x.asTerm.show(using Printer.TreeShortCode))
    x
}
