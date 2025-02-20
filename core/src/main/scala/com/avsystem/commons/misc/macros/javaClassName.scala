package com.avsystem.commons
package misc
package macros

import misc.JavaClassName

import scala.quoted.{Expr, Quotes, Type}

def javaClassName[T: Type](using quotes: Quotes): Expr[JavaClassName[T]] = {
  import quotes.reflect.*
  val tpe = TypeRepr.of[T].dealias
  if tpe.classSymbol.nonEmpty && tpe.typeSymbol != defn.ArrayClass then {
    val className = javaClassName(tpe.classSymbol.get)
    '{ new JavaClassName[T](${ Expr(className) }) } // todo: .widen /.dealias
  } else report.errorAndAbort(s"${tpe.show} does not represent a regular class")
}

private def javaClassName(using quotes: Quotes)(sym: quotes.reflect.Symbol): String = {
  val nameSuffix = if sym.moduleClass == sym && !sym.isPackageDef then "$" else ""
  val selfName = sym.name.toString + nameSuffix // todo: fullname?
  val owner = sym.owner
  val prefix =
    if owner == quotes.reflect.defn.RootClass then ""
    else if owner.isPackageDef then javaClassName(owner) + "."
    else if owner.companionModule == owner then javaClassName(owner)
    else javaClassName(owner) + "$"
  prefix + selfName
}
