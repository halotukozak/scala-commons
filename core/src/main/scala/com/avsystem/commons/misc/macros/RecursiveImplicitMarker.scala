package com.avsystem.commons
package misc.macros

import misc.macros.RecursiveImplicitMarker.history

import scala.annotation.compileTimeOnly
import scala.collection.mutable
import scala.language.experimental.erasedDefinitions
import scala.quoted.Type

object RecursiveImplicitMarker {
  private val history = mutable.Set.empty[Type[?]]

  @compileTimeOnly("this can only be used by derivation macros")
  transparent inline def mark[T: Type](inline f: Boolean => Any): Any = {
    val tpe = summon[Type[T]]
    try f(history add tpe)
    finally history -= tpe
  }
}
