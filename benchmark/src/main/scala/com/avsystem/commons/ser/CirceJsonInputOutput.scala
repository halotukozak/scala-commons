package com.avsystem.commons
package ser

import com.avsystem.commons.serialization.GenCodec.ReadFailure
import com.avsystem.commons.serialization.*
import io.circe.{Json, JsonObject}

import scala.collection.mutable.ArrayBuffer

object CirceJsonOutput {
  def write[T: GenCodec](value: T): Json = {
    var result: Json = null
    GenCodec.write(new CirceJsonOutput(result = _), value)
    result
  }
}

class CirceJsonOutput(consumer: Json => Any) extends OutputAndSimpleOutput {
  def writeNull(): Unit = consumer(Json.Null)
  def writeString(str: String): Unit = consumer(Json.fromString(str))
  def writeBoolean(boolean: Boolean): Unit = consumer(Json.fromBoolean(boolean))
  def writeInt(int: Int): Unit = consumer(Json.fromInt(int))
  def writeLong(long: Long): Unit = consumer(Json.fromLong(long))
  def writeDouble(double: Double): Unit = consumer(Json.fromDoubleOrString(double))
  def writeBigInt(bigInt: BigInt): Unit = consumer(Json.fromBigInt(bigInt))
  def writeBigDecimal(bigDecimal: BigDecimal): Unit = consumer(Json.fromBigDecimal(bigDecimal))
  def writeBinary(binary: Array[Byte]): Unit = consumer(Json.fromValues(binary.map(Json.fromInt(_))))
  def writeList(): ListOutput = new CirceJsonListOutput(consumer)
  def writeObject(): ObjectOutput = new CirceJsonObjectOutput(consumer)
  override def writeFloat(float: Float): Unit = consumer(Json.fromFloatOrString(float))
}

class CirceJsonListOutput(consumer: Json => Any) extends ListOutput {
  private[this] val elems = Vector.newBuilder[Json]

  def writeElement(): Output = new CirceJsonOutput(elems += _)
  def finish(): Unit = consumer(Json.fromValues(elems.result()))
}

class CirceJsonObjectOutput(consumer: Json => Any) extends ObjectOutput {
  private[this] val elems = new ArrayBuffer[(String, Json)]

  def writeField(key: String): Output = new CirceJsonOutput(json => elems += ((key, json)))
  def finish(): Unit = consumer(Json.fromFields(elems))
}

object CirceJsonInput {
  def read[T: GenCodec](json: Json): T =
    GenCodec.read[T](new CirceJsonInput(json))
}

class CirceJsonInput(json: Json) extends InputAndSimpleInput {
  private def failNot(what: String) =
    throw new ReadFailure(s"not $what")

  private def asNumber = json.asNumber.getOrElse(failNot("number"))

  def readNull(): Boolean = json.isNull
  def readString(): String = json.asString.getOrElse(failNot("string"))
  def readBoolean(): Boolean = json.asBoolean.getOrElse(failNot("boolean"))
  override def readByte(): Byte = asNumber.toByte.getOrElse(failNot("byte"))
  override def readShort(): Short = asNumber.toShort.getOrElse(failNot("short"))
  def readInt(): Int = asNumber.toInt.getOrElse(failNot("int"))
  def readLong(): Long = asNumber.toLong.getOrElse(failNot("long"))
  def readDouble(): Double = asNumber.toDouble
  def readBigInt(): BigInt = asNumber.toBigInt.getOrElse(failNot("bigInteger"))
  def readBigDecimal(): BigDecimal = asNumber.toBigDecimal.getOrElse(failNot("bigDecimal"))
  def readBinary(): Array[Byte] = json.asArray
    .getOrElse(failNot("array"))
    .iterator
    .map(_.asNumber.flatMap(_.toByte).getOrElse(failNot("byte")))
    .toArray
  def readList(): ListInput = new CirceJsonListInput(json.asArray.getOrElse(failNot("array")))
  def readObject(): ObjectInput = new CirceJsonObjectInput(json.asObject.getOrElse(failNot("object")))
  def skip(): Unit = ()
}

class CirceJsonFieldInput(val fieldName: String, json: Json) extends CirceJsonInput(json) with FieldInput

class CirceJsonListInput(jsonArray: Vector[Json]) extends ListInput {
  private[this] val it = jsonArray.iterator

  def hasNext: Boolean = it.hasNext
  def nextElement(): Input = new CirceJsonInput(it.next())
}

class CirceJsonObjectInput(jsonObject: JsonObject) extends ObjectInput {
  private[this] val fieldIt = jsonObject.keys.iterator

  def hasNext: Boolean = fieldIt.hasNext
  def nextField(): FieldInput = {
    val field = fieldIt.next()
    new CirceJsonFieldInput(field, jsonObject(field).get)
  }
}
