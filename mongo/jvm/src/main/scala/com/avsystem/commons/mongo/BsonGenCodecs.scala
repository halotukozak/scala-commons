//package com.avsystem.commons
//package mongo
//
//import com.avsystem.commons.serialization.GenCodec.ReadFailure
//import com.avsystem.commons.serialization._
//import org.bson._
//import org.bson.io.BasicOutputBuffer
//import org.bson.types.{Decimal128, ObjectId}
//
//import java.nio.ByteBuffer
//
//trait BsonGenCodecs {
//  given objectIdIdentityWrapping: TransparentWrapping[ObjectId, ObjectId] = BsonGenCodecs.objectIdIdentityWrapping
//  given objectIdCodec: GenCodec[ObjectId] = BsonGenCodecs.objectIdCodec
//  given objectIdKeyCodec: GenKeyCodec[ObjectId] = BsonGenCodecs.objectIdKeyCodec
//  given decimal128Codec: GenCodec[Decimal128] = BsonGenCodecs.decimal128Codec
//
//  given bsonArrayCodec: GenCodec[BsonArray] = BsonGenCodecs.bsonArrayCodec
//  given bsonBinaryCodec: GenCodec[BsonBinary] = BsonGenCodecs.bsonBinaryCodec
//  given bsonBooleanCodec: GenCodec[BsonBoolean] = BsonGenCodecs.bsonBooleanCodec
//  given bsonDateTimeCodec: GenCodec[BsonDateTime] = BsonGenCodecs.bsonDateTimeCodec
//  given bsonDocumentCodec: GenCodec[BsonDocument] = BsonGenCodecs.bsonDocumentCodec
//  given bsonDecimal128Codec: GenCodec[BsonDecimal128] = BsonGenCodecs.bsonDecimal128Codec
//  given bsonDoubleCodec: GenCodec[BsonDouble] = BsonGenCodecs.bsonDoubleCodec
//  given bsonInt32Codec: GenCodec[BsonInt32] = BsonGenCodecs.bsonInt32Codec
//  given bsonInt64Codec: GenCodec[BsonInt64] = BsonGenCodecs.bsonInt64Codec
//  given bsonNullCodec: GenCodec[BsonNull] = BsonGenCodecs.bsonNullCodec
//  given bsonObjectIdCodec: GenCodec[BsonObjectId] = BsonGenCodecs.bsonObjectIdCodec
//  given bsonStringCodec: GenCodec[BsonString] = BsonGenCodecs.bsonStringCodec
//  given bsonValueCodec: GenCodec[BsonValue] = BsonGenCodecs.bsonValueCodec
//}
//
//object BsonGenCodecs {
//  // needed so that ObjectId can be used as ID type in AutoIdMongoEntity
//  // (TransparentWrapping is used in EntityIdMode)
//  given objectIdIdentityWrapping: TransparentWrapping[ObjectId, ObjectId] = TransparentWrapping.identity
//
//  given objectIdCodec: GenCodec[ObjectId] = GenCodec.nullable(
//    i => i.readCustom(ObjectIdMarker).getOrElse(new ObjectId(i.readSimple().readString())),
//    (o, v) => if (!o.writeCustom(ObjectIdMarker, v)) o.writeSimple().writeString(v.toHexString),
//  )
//
//  given objectIdKeyCodec: GenKeyCodec[ObjectId] =
//    GenKeyCodec.create(new ObjectId(_), _.toHexString)
//
//  given decimal128Codec: GenCodec[Decimal128] = GenCodec.nullable(
//    i => i.readCustom(Decimal128Marker).getOrElse(new Decimal128(i.readSimple().readBigDecimal().bigDecimal)),
//    (o, v) => if (!o.writeCustom(Decimal128Marker, v)) o.writeSimple().writeBigDecimal(v.bigDecimalValue()),
//  )
//
//  given bsonValueCodec: GenCodec[BsonValue] = GenCodec.create(
//    i => i.readCustom(BsonValueMarker).getOrElse {
//      val reader = new BsonBinaryReader(ByteBuffer.wrap(i.readSimple().readBinary()))
//      BsonValueUtils.decode(reader).asDocument().get("v")
//    },
//    (o, bv) => if (!o.writeCustom(BsonValueMarker, bv)) {
//      val buffer = new BasicOutputBuffer()
//      val writer = new BsonBinaryWriter(buffer)
//      BsonValueUtils.encode(writer, new BsonDocument("v", bv))
//      writer.flush()
//      writer.close()
//      o.writeSimple().writeBinary(buffer.toByteArray)
//    },
//  )
//
//  private def bsonValueSubCodec[T <: BsonValue](fromBsonValue: BsonValue => T): GenCodec[T] =
//    bsonValueCodec.transform(identity, fromBsonValue)
//
//  given bsonArrayCodec: GenCodec[BsonArray] = bsonValueSubCodec(_.asArray())
//  given bsonBinaryCodec: GenCodec[BsonBinary] = bsonValueSubCodec(_.asBinary())
//  given bsonBooleanCodec: GenCodec[BsonBoolean] = bsonValueSubCodec(_.asBoolean())
//  given bsonDateTimeCodec: GenCodec[BsonDateTime] = bsonValueSubCodec(_.asDateTime())
//  given bsonDocumentCodec: GenCodec[BsonDocument] = bsonValueSubCodec(_.asDocument())
//  given bsonDecimal128Codec: GenCodec[BsonDecimal128] = bsonValueSubCodec(_.asDecimal128())
//  given bsonDoubleCodec: GenCodec[BsonDouble] = bsonValueSubCodec(_.asDouble())
//  given bsonInt32Codec: GenCodec[BsonInt32] = bsonValueSubCodec(_.asInt32())
//  given bsonInt64Codec: GenCodec[BsonInt64] = bsonValueSubCodec(_.asInt64())
//
//  given bsonNullCodec: GenCodec[BsonNull] =
//    bsonValueSubCodec { bv =>
//      if (bv.isNull) BsonNull.VALUE
//      else throw new ReadFailure("Input did not contain expected null value")
//    }
//
//  given bsonObjectIdCodec: GenCodec[BsonObjectId] =
//    objectIdCodec.transform(_.getValue, new BsonObjectId(_))
//
//  given bsonStringCodec: GenCodec[BsonString] =
//    GenCodec.StringCodec.transform(_.getValue, new BsonString(_))
//}
