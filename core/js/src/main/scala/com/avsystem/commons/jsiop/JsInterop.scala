package com.avsystem.commons
package jsiop

import misc.Timestamp

import scala.scalajs.js
import scala.scalajs.js.UndefOr

trait JsInterop {
  extension (jsDate: js.Date) {
    inline def toTimestamp: Timestamp = Timestamp(jsDate.getTime().toLong)
    inline def toJsDate: js.Date = new js.Date(jsDate.getTime())
    inline def toJDate: JDate = new JDate(jsDate.getTime().toLong)
  }

  extension [A](value: UndefOr[A]) inline def toOpt: Opt[A] = if value.isDefined then Opt(value.get) else Opt.Empty

  extension [A](raw: A)
    inline def orUndefined: UndefOr[A] = Opt(raw) match
      case Opt.Empty => js.undefined
      case Opt(value) => js.defined(value)
}

object JsInterop extends JsInterop
