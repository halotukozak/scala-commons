package com.avsystem.commons
package mongo

import org.bson.codecs.{BsonValueCodec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonValue, BsonWriter}

object BsonValueUtils {
  private val bsonValueCodec = new BsonValueCodec
  private val encoderContext = EncoderContext.builder.build
  private val decoderContext = DecoderContext.builder.build

  def encode(bw: BsonWriter, bv: BsonValue): Unit =
    bsonValueCodec.encode(bw, bv, encoderContext)

  def decode(br: BsonReader): BsonValue = {
    if br.getCurrentBsonType eq null then {
      br.readBsonType()
    }
    bsonValueCodec.decode(br, decoderContext)
  }
}
