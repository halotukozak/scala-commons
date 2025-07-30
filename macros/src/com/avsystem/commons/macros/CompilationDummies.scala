package com.avsystem.commons
package macros

import com.sun.tools.javac.code.TypeTag

import scala.reflect.macros.{Universe, blackbox}

trait CompilationDummies {
  val c: blackbox.Context
  given [T]: c.TypeTag[T] = ???
}
