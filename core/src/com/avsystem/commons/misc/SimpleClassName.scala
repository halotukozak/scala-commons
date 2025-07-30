package com.avsystem.commons
package misc

case class SimpleClassName[T](name: String) extends AnyVal
object SimpleClassName {
  def of[T](using scn: SimpleClassName[T]): String = scn.name

  given materialize[T]: SimpleClassName[T] = ??? // macro macros.misc.MiscMacros.simpleClassName[T]
}
