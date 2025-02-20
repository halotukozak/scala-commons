package com.avsystem.commons
package serialization

import serialization.GenCodec.ReadFailure

import java.io.{DataInputStream, DataOutputStream}
import java.lang as jl

private object FormatConstants {
  final val ByteBytes = jl.Byte.BYTES
  final val ShortBytes = jl.Short.BYTES
  final val IntBytes = jl.Integer.BYTES
  final val LongBytes = jl.Long.BYTES
  final val FloatBytes = jl.Float.BYTES
  final val DoubleBytes = jl.Double.BYTES

  final val NullMarker: Byte = 0
  final val StringMarker: Byte = 1
  final val ByteMarker: Byte = 2
  final val ShortMarker: Byte = 3
  final val IntMarker: Byte = 4
  final val LongMarker: Byte = 5
  final val FloatMarker: Byte = 6
  final val DoubleMarker: Byte = 7
  final val ByteArrayMarker: Byte = 8
  final val BooleanMarker: Byte = 9
  final val ListStartMarker: Byte = 10
  final val ObjectStartMarker: Byte = 11
  final val ListEndMarker: Byte = 12
  final val ObjectEndMarker: Byte = 13
  final val BitIntMarker: Byte = 14
  final val BigDecimalMarker: Byte = 15
}

import serialization.FormatConstants.*

class StreamInput(is: DataInputStream) extends InputAndSimpleInput {
  private[serialization] val markerByte = is.readByte()

  def readNull(): Boolean =
    markerByte == NullMarker

  def readString(): String =
    if markerByte == StringMarker then is.readUTF()
    else throw new ReadFailure(s"Expected string but $markerByte found")

  def readBoolean(): Boolean =
    if markerByte == BooleanMarker then is.readBoolean()
    else throw new ReadFailure(s"Expected boolean but $markerByte found")

  def readInt(): Int =
    if markerByte == IntMarker then is.readInt()
    else throw new ReadFailure(s"Expected int but $markerByte found")

  def readLong(): Long =
    if markerByte == LongMarker then is.readLong()
    else throw new ReadFailure(s"Expected long but $markerByte found")

  def readDouble(): Double =
    if markerByte == DoubleMarker then is.readDouble()
    else throw new ReadFailure(s"Expected double but $markerByte found")

  def readBigInt(): BigInt =
    if markerByte == BitIntMarker then {
      val len = is.readInt()
      val bytes = new Array[Byte](len)
      is.read(bytes)
      BigInt(bytes)
    } else {
      throw new ReadFailure(s"Expected big integer but $markerByte found")
    }

  def readBigDecimal(): BigDecimal =
    if markerByte == BigDecimalMarker then {
      val len = is.readInt()
      val bytes = new Array[Byte](len)
      is.read(bytes)
      val unscaled = BigInt(bytes)
      val scale = is.readInt()
      BigDecimal(unscaled, scale)
    } else {
      throw new ReadFailure(s"Expected big decimal but $markerByte found")
    }

  def readBinary(): Array[Byte] =
    if markerByte == ByteArrayMarker then {
      val binary = Array.ofDim[Byte](is.readInt())
      is.readFully(binary)
      binary
    } else {
      throw new ReadFailure(s"Expected binary array but $markerByte found")
    }

  def readList(): ListInput =
    if markerByte == ListStartMarker then new StreamListInput(is)
    else throw new ReadFailure(s"Expected list but $markerByte found")

  def readObject(): ObjectInput =
    if markerByte == ObjectStartMarker then new StreamObjectInput(is)
    else throw new ReadFailure(s"Expected object but $markerByte found")

  def skip(): Unit = markerByte match
    case NullMarker =>
    // no op
    case StringMarker =>
      val n = is.readUnsignedShort()
      is.skipBytes(n)
    case ByteMarker =>
      is.skipBytes(ByteBytes)
    case ShortMarker =>
      is.skipBytes(ShortBytes)
    case IntMarker =>
      is.skipBytes(IntBytes)
    case LongMarker =>
      is.skipBytes(LongBytes)
    case FloatMarker =>
      is.skipBytes(FloatBytes)
    case DoubleMarker =>
      is.skipBytes(DoubleBytes)
    case ByteArrayMarker =>
      is.skipBytes(is.readInt())
    case BooleanMarker =>
      is.skipBytes(ByteBytes)
    case ListStartMarker =>
      new StreamListInput(is).skipRemaining()
    case ObjectStartMarker =>
      new StreamObjectInput(is).skipRemaining()
    case BitIntMarker =>
      is.skipBytes(is.readInt())
    case BigDecimalMarker =>
      is.skipBytes(is.readInt() + Integer.BYTES)
    case unexpected =>
      throw new ReadFailure(s"Unexpected marker byte: $unexpected")
}

class StreamFieldInput(val fieldName: String, is: DataInputStream) extends StreamInput(is) with FieldInput

private class StreamListInput(is: DataInputStream) extends ListInput {
  private var currentInput: Opt[StreamInput] = Opt.empty

  def nextElement(): Input = {
    if !hasNext then throw new ReadFailure("List already emptied")
    val input = currentInput
    currentInput = Opt.empty
    input.get
  }

  def hasNext: Boolean = {
    ensureInput()
    currentInput.get.markerByte != ListEndMarker
  }

  private def ensureInput(): Unit =
    if currentInput == Opt.empty then currentInput = Opt.some(new StreamInput(is))
}

private class StreamObjectInput(is: DataInputStream) extends ObjectInput {

  import StreamObjectInput.*

  private var currentField: FieldInput = NoneYet

  def nextField(): FieldInput = {
    if !hasNext then throw new ReadFailure("Object already emptied")
    val field = currentField
    currentField = NoneYet
    field
  }

  def hasNext: Boolean = {
    ensureInput()
    currentField ne End
  }

  private def ensureInput(): Unit =
    if currentField eq NoneYet then {
      val keyInput = new StreamInput(is)
      currentField = if keyInput.markerByte != ObjectEndMarker then {
        val keyString = keyInput.readString()
        new StreamFieldInput(keyString, is)
      } else End
    }
}

private object StreamObjectInput {
  val NoneYet = EmptyFieldInput("NONE")
  val End = EmptyFieldInput("END")

  case class EmptyFieldInput(name: String) extends InputAndSimpleInput with FieldInput {
    def isNull: Boolean = false

    def isList: Boolean = false

    def isObject: Boolean = false

    def fieldName: String = name

    def readNull(): Boolean = false

    def readString(): String = nope

    def readBoolean(): Boolean = nope

    def readInt(): Int = nope

    def readLong(): Long = nope

    def readDouble(): Double = nope

    def readBigInt(): BigInt = nope

    def readBigDecimal(): BigDecimal = nope

    def readBinary(): Array[Byte] = nope

    private def nope: Nothing = throw new ReadFailure(s"Something went horribly wrong ($name)")

    def readList(): ListInput = nope

    def readObject(): ObjectInput = nope

    def skip(): Unit = nope
  }
}

class StreamOutput(os: DataOutputStream) extends OutputAndSimpleOutput {

  private val streamList = new StreamListOutput(os, this)
  private val streamObject = new StreamObjectOutput(os, this)

  def writeNull(): Unit = os.write(NullMarker)

  def writeString(str: String): Unit = {
    os.writeByte(StringMarker)
    os.writeUTF(str)
  }

  def writeBoolean(boolean: Boolean): Unit = {
    os.writeByte(BooleanMarker)
    os.writeBoolean(boolean)
  }

  def writeInt(int: Int): Unit = {
    os.writeByte(IntMarker)
    os.writeInt(int)
  }

  def writeLong(long: Long): Unit = {
    os.writeByte(LongMarker)
    os.writeLong(long)
  }

  def writeDouble(double: Double): Unit = {
    os.writeByte(DoubleMarker)
    os.writeDouble(double)
  }

  def writeBigInt(bigInt: BigInt): Unit = {
    os.writeByte(BitIntMarker)
    val bytes = bigInt.toByteArray
    os.writeInt(bytes.length)
    os.write(bytes)
  }

  def writeBigDecimal(bigDecimal: BigDecimal): Unit = {
    os.writeByte(BigDecimalMarker)
    val bytes = bigDecimal.bigDecimal.unscaledValue.toByteArray
    os.writeInt(bytes.length)
    os.write(bytes)
    os.writeInt(bigDecimal.scale)
  }

  def writeBinary(binary: Array[Byte]): Unit = {
    os.writeByte(ByteArrayMarker)
    os.writeInt(binary.length)
    os.write(binary)
  }

  def writeList(): ListOutput = {
    os.writeByte(ListStartMarker)
    streamList
  }

  def writeObject(): ObjectOutput = {
    os.writeByte(ObjectStartMarker)
    streamObject
  }
}

private class StreamListOutput(os: DataOutputStream, output: StreamOutput) extends ListOutput {
  override def sizePolicy: SizePolicy = SizePolicy.Ignored

  def writeElement(): Output = output

  def finish(): Unit = os.writeByte(ListEndMarker)
}

private class StreamObjectOutput(os: DataOutputStream, output: StreamOutput) extends ObjectOutput {
  override def sizePolicy: SizePolicy = SizePolicy.Ignored

  def writeField(key: String): Output = {
    output.writeString(key)
    output
  }

  def finish(): Unit = os.writeByte(ObjectEndMarker)
}
