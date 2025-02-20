package com.avsystem.commons
package serialization.cbor

import serialization.CustomEventMarker
import serialization.GenCodec.ReadFailure

/**
 * Custom encoder for CBOR field names. This can be used to encode textual object keys (e.g. case class field names) as
 * arbitrary CBOR data types, e.g. in order to make the final representation more compact, every textual field name may
 * have a numeric label assigned. This numeric label is written as key into the [[CborObjectOutput]] rather than the
 * textual field name.
 */
trait CborKeyCodec {
  def writeFieldKey(fieldName: String, output: CborOutput): Unit

  def readFieldKey(input: CborInput): String
}

object CborKeyCodec {
  final val Default = new CborKeyCodec {
    def writeFieldKey(fieldName: String, output: CborOutput): Unit = output.writeString(fieldName)

    def readFieldKey(input: CborInput): String = input.readString()
  }
}

/**
 * Use this with `ObjectOutput.customEvent`/`ObjectInput.customEvent` in order to set custom CBOR key codec for some
 * particular object output or input.
 */
object ForceCborKeyCodec extends CustomEventMarker[CborKeyCodec]
