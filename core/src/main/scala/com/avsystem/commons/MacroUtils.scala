package com.avsystem.commons

import MacroUtils.positionCache
import annotation.positioned

import scala.collection.mutable
import scala.quoted.{Expr, Quotes, Type}

trait MacroUtils {
  protected def positionPoint(using quotes: Quotes)(sym: quotes.reflect.Symbol): Int = {
    import quotes.reflect.*

    if Position.ofMacroExpansion == sym.pos then sym.pos.get.start
    else positionCache.getOrElseUpdate(
      sym,
      rawAnnotations(sym).find(_.tpe <:< TypeRepr.of[positioned]).map {
        case Apply(_, List(Literal(point: Int))) => point
        case t => report.errorAndAbort(s"expected literal int as argument of @positioned annotation on $sym, got $t")
      } getOrElse report.errorAndAbort(s"Could not determine source position of $sym - it resides in separate file than macro invocation and has no @positioned annotation"),
    )
  }

  def rawAnnotations(using quotes: Quotes)(s: quotes.reflect.Symbol): List[quotes.reflect.Term] = {
    import quotes.reflect.*

    // for vals or vars, fetch annotations from underlying field
    if s.isDefDef && s.flags.is(Flags.FieldAccessor) then {
      val accessors = Select(s.tree.asInstanceOf[Term], s)
      s.annotations ++ accessors.qualifier.tpe.termSymbol.annotations
    }
    else s.annotations
  }

  def containsInaccessibleThises(using quotes: Quotes)(tree: quotes.reflect.Symbol): Boolean = {
    import quotes.reflect.*
    tree.children.exists {
      case t@This(_) if !t.symbol.isPackageDef /*&& !enclosingClasses.contains(t.symbol)*/ => true
      case _ => false
    }
  }

  extension (using quotes: Quotes)(any: Any) {
    inline def info: any.type = {
      quotes.reflect.report.info(s"${any}")
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
    
    def isPublic: Boolean = {
      import quotes.reflect.*
      !symbol.flags.isAnyOf(Flags.Private, Flags.PrivateLocal, Flags.Protected)
    }
  }

  inline def printTree[T](inline x: T): T = ${ PrintTree.printTreeImpl('{ x }) }

}

object MacroUtils extends MacroUtils {
  private val positionCache = new mutable.HashMap[Any, Int]
}

object PrintTree {
  def printTreeImpl[T: Type](x: Expr[T])(using quotes: Quotes): Expr[T] = {
    import quotes.reflect.*

    println("Structure " + x.asTerm.show(using Printer.TreeStructure))
    println("ShortCode " + x.asTerm.show(using Printer.TreeShortCode))
    x
  }
}
