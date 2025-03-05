package com.avsystem.commons
package serialization

abstract class InputWrapper extends Input {
  def readNull(): Boolean = wrapped.readNull()
  def readSimple(): SimpleInput = wrapped.readSimple()
  def readList(): ListInput = wrapped.readList()
  def readObject(): ObjectInput = wrapped.readObject()
  override def readMetadata[T](metadata: InputMetadata[T]): Opt[T] = wrapped.readMetadata(metadata)
  override def readCustom[T](typeMarker: TypeMarker[T]): Opt[T] = wrapped.readCustom(typeMarker)
  override def customEvent[T](marker: CustomEventMarker[T], value: T): Boolean = wrapped.customEvent(marker, value)
  override def legacyOptionEncoding: Boolean = wrapped.legacyOptionEncoding
  def skip(): Unit = wrapped.skip()
  protected def wrapped: Input
}

abstract class SimpleInputWrapper extends SimpleInput {
  def readString(): String = wrapped.readString()
  def readBoolean(): Boolean = wrapped.readBoolean()
  def readInt(): Int = wrapped.readInt()
  def readLong(): Long = wrapped.readLong()
  def readDouble(): Double = wrapped.readDouble()
  def readBigInt(): BigInt = wrapped.readBigInt()
  def readBigDecimal(): BigDecimal = wrapped.readBigDecimal()
  def readBinary(): Array[Byte] = wrapped.readBinary()
  override def readChar(): Char = wrapped.readChar()
  override def readByte(): Byte = wrapped.readByte()
  override def readShort(): Short = wrapped.readShort()
  override def readTimestamp(): Long = wrapped.readTimestamp()
  override def readFloat(): Float = wrapped.readFloat()
  protected def wrapped: SimpleInput
}

abstract class FieldInputWrapper extends InputWrapper with FieldInput {
  def fieldName: String = wrapped.fieldName
  protected def wrapped: FieldInput
}

abstract class SequentialInputWrapper extends SequentialInput {
  override def knownSize: Int = wrapped.knownSize
  override def customEvent[T](marker: CustomEventMarker[T], value: T): Boolean = wrapped.customEvent(marker, value)
  def hasNext: Boolean = wrapped.hasNext
  protected def wrapped: SequentialInput
}

abstract class ListInputWrapper extends SequentialInputWrapper with ListInput {
  def nextElement(): Input = wrapped.nextElement()
  protected def wrapped: ListInput
}

abstract class ObjectInputWrapper extends SequentialInputWrapper with ObjectInput {
  override def peekField(name: String): Opt[FieldInput] = wrapped.peekField(name)
  def nextField(): FieldInput = wrapped.nextField()
  protected def wrapped: ObjectInput
}

abstract class OutputWrapper extends Output {
  def writeNull(): Unit = wrapped.writeNull()
  def writeSimple(): SimpleOutput = wrapped.writeSimple()
  def writeList(): ListOutput = wrapped.writeList()
  def writeObject(): ObjectOutput = wrapped.writeObject()
  override def writeCustom[T](typeMarker: TypeMarker[T], value: T): Boolean = wrapped.writeCustom(typeMarker, value)
  override def keepsMetadata(metadata: InputMetadata[?]): Boolean = wrapped.keepsMetadata(metadata)
  override def customEvent[T](marker: CustomEventMarker[T], value: T): Boolean = wrapped.customEvent(marker, value)
  override def legacyOptionEncoding: Boolean = wrapped.legacyOptionEncoding
  protected def wrapped: Output
}

abstract class SimpleOutputWrapper extends SimpleOutput {
  def writeString(str: String): Unit = wrapped.writeString(str)
  def writeBoolean(boolean: Boolean): Unit = wrapped.writeBoolean(boolean)
  def writeInt(int: Int): Unit = wrapped.writeInt(int)
  def writeLong(long: Long): Unit = wrapped.writeLong(long)
  def writeDouble(double: Double): Unit = wrapped.writeDouble(double)
  def writeBigInt(bigInt: BigInt): Unit = wrapped.writeBigInt(bigInt)
  def writeBigDecimal(bigDecimal: BigDecimal): Unit = wrapped.writeBigDecimal(bigDecimal)
  def writeBinary(binary: Array[Byte]): Unit = wrapped.writeBinary(binary)
  override def writeChar(char: Char): Unit = wrapped.writeChar(char)
  override def writeByte(byte: Byte): Unit = wrapped.writeByte(byte)
  override def writeShort(short: Short): Unit = wrapped.writeShort(short)
  override def writeTimestamp(millis: Long): Unit = wrapped.writeTimestamp(millis)
  override def writeFloat(float: Float): Unit = wrapped.writeFloat(float)
  protected def wrapped: SimpleOutput
}

abstract class SequentialOutputWrapper extends SequentialOutput {
  override def declareSize(size: Int): Unit = wrapped.declareSize(size)
  override def sizePolicy: SizePolicy = wrapped.sizePolicy
  override def customEvent[T](marker: CustomEventMarker[T], value: T): Boolean = wrapped.customEvent(marker, value)
  def finish(): Unit = wrapped.finish()
  protected def wrapped: SequentialOutput
}

abstract class ListOutputWrapper extends SequentialOutputWrapper with ListOutput {
  def writeElement(): Output = wrapped.writeElement()
  protected def wrapped: ListOutput
}

abstract class ObjectOutputWrapper extends SequentialOutputWrapper with ObjectOutput {
  def writeField(key: String): Output = wrapped.writeField(key)
  protected def wrapped: ObjectOutput
}
