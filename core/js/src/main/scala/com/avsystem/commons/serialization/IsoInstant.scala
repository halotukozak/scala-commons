package com.avsystem.commons
package serialization

import serialization.GenCodec.ReadFailure

import scala.scalajs.js
import scala.scalajs.js.RegExp

object IsoInstant {
  private val regex: RegExp =
    js.RegExp("""^(\+|-)?[0-9]+-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\.[0-9]{3})?Z$""")

  inline def format(millis: Long): String = new js.Date(millis.toDouble).toISOString()

  def parse(string: String): Long = {
    def fail() = throw new ReadFailure(s"invalid ISO instant: $string")

    if regex.test(string) then {
      val parsed = js.Date.parse(string)
      if parsed.isNaN then fail()
      else parsed.toLong
    }
    else fail()
  }
}
