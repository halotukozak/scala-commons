package com.avsystem.commons
package misc.macros.misc

import misc.TypeString

import scala.quoted.{Expr, Quotes, Type}

def typeString[T: Type](using quotes: Quotes): Expr[TypeString[T]] = {
  import quotes.reflect.*
  val tpe = Expr(TypeRepr.of[T].show) //todo? .widen /.dealias
  '{ new TypeString(${ tpe }) }
  //    try typeStringParts(tpe) match {
  //      case List(Select(pre, TermName("value"))) => pre
  //      case trees => q"new $MiscPkg.TypeString[$tpe](${mkStringConcat(trees)})"
  //    } catch {
  //      case NonConcreteTypeException(stpe) =>
  //        abort(s"Could not materialize TypeString for $tpe because instance for $stpe is lacking")
  //    }
}
