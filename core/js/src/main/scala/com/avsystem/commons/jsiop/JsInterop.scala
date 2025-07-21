package com.avsystem.commons
package jsiop

import com.avsystem.commons.misc.TimestampConversions

import scala.scalajs.js
import scala.scalajs.js.UndefOr

trait JsInterop {
  given Conversion[js.Date, TimestampConversions] = jsDate => TimestampConversions(jsDate.getTime().toLong)

  extension [A](value: UndefOr[A]) {
    def toOpt: Opt[A] = if (value.isDefined) Opt(value.get) else Opt.Empty
  }

  extension [A](raw: A) {
    private def value = Opt(raw)

    def orUndefined: UndefOr[A] = if (value.isDefined) js.defined(value.get) else js.undefined
  }
}
object JsInterop extends JsInterop 