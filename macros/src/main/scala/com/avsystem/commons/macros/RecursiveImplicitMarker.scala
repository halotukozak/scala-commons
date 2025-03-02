package com.avsystem.commons
package macros

import scala.annotation.compileTimeOnly
import scala.language.experimental.erasedDefinitions

//todo: erased
trait RecursiveImplicitMarker[T]
object RecursiveImplicitMarker {
//  @compileTimeOnly("this can only be used by derivation macros")
  def mark[T]: RecursiveImplicitMarker[T] = throw new NotImplementedError
}
