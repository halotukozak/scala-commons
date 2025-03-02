package com.avsystem.commons
package serialization.json

import misc.CaseMethods
import serialization.GenCodec
import serialization.json.RawJson

/**
 * Wrapper for raw JSON string
 *
 * It will be serialized as JSON value when used with [[com.avsystem.commons.serialization.Output]] supporting
 * [[RawJson]] marker.
 */
final case class WrappedJson(value: String) extends AnyVal with CaseMethods
object WrappedJson {
  implicit val codec: GenCodec[WrappedJson] = GenCodec.create(
    in => WrappedJson(in.readCustom(RawJson).getOrElse(in.readSimple().readString())),
    (out, v) => if !out.writeCustom(RawJson, v.value) then out.writeSimple().writeString(v.value),
  )
}
