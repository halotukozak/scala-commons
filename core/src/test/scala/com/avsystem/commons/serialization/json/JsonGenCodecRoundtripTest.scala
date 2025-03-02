package com.avsystem.commons
package serialization.json

import serialization.{GenCodecRoundtripTest, Input, Output}

final class JsonGenCodecRoundtripTest extends GenCodecRoundtripTest:
  type Raw = String

  def writeToOutput(write: Output => Unit): String = {
    val sb = new JStringBuilder
    write(new JsonStringOutput(sb))
    sb.toString
  }

  def createInput(raw: String): Input =
    new JsonStringInput(new JsonReader(raw))
