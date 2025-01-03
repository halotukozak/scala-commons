package com.avsystem.commons
package serialization

import java.util.Base64 as JBase64

object Base64 {
  def encode(bytes: Array[Byte], withoutPadding: Boolean = false, urlSafe: Boolean = false): String = {
    val encoder = (if urlSafe then JBase64.getUrlEncoder else JBase64.getEncoder) |>
      (e => if withoutPadding then e.withoutPadding else e)
    encoder.encodeToString(bytes)
  }

  def decode(base64: String, urlSafe: Boolean = false): Array[Byte] = {
    val decoder = if urlSafe then JBase64.getUrlDecoder else JBase64.getDecoder
    decoder.decode(base64)
  }
}
