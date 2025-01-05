package com.avsystem.commons
package misc
package macros

import misc.TypeString

import scala.quoted.{Expr, Quotes, Type}

def typeString[T: Type](using quotes: Quotes): Expr[TypeString[T]] = {
  import quotes.reflect.*

  def mkName: TypeRepr => String = {
      case MethodType(paramNames, paramInfos, resultType) =>
        println(s"paramNames: $paramNames")
        println(s"paramInfos: $paramInfos")
        println(s"resultType: $resultType")
        s"${paramInfos.map(mkName)} => ${resultType.show}"
        ???
      case t => t.show
  }

  val tpe = Expr(mkName(TypeRepr.of[T]))
  '{ new TypeString(${ tpe }) }
  //    try typeStringParts(tpe) match {
  //      case List(Select(pre, TermName("value"))) => pre
  //      case trees => q"new $MiscPkg.TypeString[$tpe](${mkStringConcat(trees)})"
  //    } catch {
  //      case NonConcreteTypeException(stpe) =>
  //        abort(s"Could not materialize TypeString for $tpe because instance for $stpe is lacking")
  //    }
}
