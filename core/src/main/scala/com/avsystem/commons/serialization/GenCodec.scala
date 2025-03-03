package com.avsystem.commons
package serialization

import annotation.explicitGenerics
import derivation.{AllowImplicitMacro, DeferredInstance}
import jiop.JFactory
import meta.Fallback
import misc.{Bytes, Timestamp, millis}

import com.avsystem.commons.serialization.macros.materializeImpl

import java.util.UUID
import scala.annotation.{implicitNotFound, tailrec}
import scala.collection.Factory

/**
 * Type class for types that can be serialized to [[Output]] (format-agnostic "output stream") and deserialized from
 * [[Input]] (format-agnostic "input stream"). `GenCodec` is supposed to capture generic structure of serialized
 * objects, without being bound to particular format like JSON. The actual format is determined by implementation of
 * [[Input]] and [[Output]].
 *
 * There are convenient macros for automatic derivation of [[GenCodec]] instances (`materialize` and
 * `materializeRecursively`). However, [[GenCodec]] instances still need to be explicitly declared and won't be derived
 * "automagically".
 */
@implicitNotFound("No GenCodec found for ${T}")
trait GenCodec[T] {

  /**
   * Deserializes a value of type `T` from an [[Input]].
   */
  def read(input: Input): T

  /**
   * Serializes a value of type `T` into an [[Output]].
   */
  def write(output: Output, value: T): Unit

  /**
   * Transforms this codec into a codec of other type using a bidirectional conversion between the original and new
   * type.
   */
  final def transform[U](onWrite: U => T, onRead: T => U): GenCodec[U] =
    new GenCodec.Transformed[U, T](this, onWrite, onRead)
}

object GenCodec extends RecursiveAutoCodecs with TupleGenCodecs {
  def apply[T](using codec: GenCodec[T]): GenCodec[T] = codec

  /**
   * Macro that automatically materializes a [[GenCodec]] for some type `T`, which must be one of: <ul> <li>singleton
   * type, e.g. an `object`</li> <li>case class whose every field type has its own [[GenCodec]]</li> <li>(generalization
   * of case classes) class or trait whose companion object has a pair of case-class-like `apply` and `unapply` methods
   * and every parameter type of `apply` method has its own [[GenCodec]] </li> <li>sealed hierarchy in which every
   * non-abstract subclass either has its own [[GenCodec]] or it can be automatically materialized with the same
   * mechanism</li> </ul> Note that automatic materialization does NOT descend into types that `T` is made of (e.g.
   * types of case class fields must have their own codecs independently declared). If you want recursive
   * materialization, use `materializeRecursively`.
   */
  inline def materialize[T]: GenCodec[T] = ${ materializeImpl[T] }

  /**
   * Materializes a [[GenCodec]] for type `T` using `apply` and `unapply`/`unapplySeq` methods available on passed
   * `applyUnapplyProvider` object. The signatures of `apply` and `unapply` must be as if `T` was a case class and
   * `applyUnapplyProvider` was its companion object. This is useful for easy derivation of [[GenCodec]] for third party
   * classes which don't have their own companion objects with `apply` and `unapply`. So essentially the
   * `applyUnapplyProvider` is a "fake companion object" of type `T`.
   *
   * Example:
   * {{{
   *   class ThirdParty { ... }
   *
   *   object ThirdPartyFakeCompanion {
   *     def apply(int: Int, string: String): ThirdParty = ...
   *     def unapply(tp: ThirdParty): Option[(Int, String)] = ...
   *   }
   *
   *   given thirdPartyCodec: GenCodec[ThirdParty] =
   *     GenCodec.fromApplyUnapplyProvider[ThirdParty](ThirdPartyFakeCompanion)
   * }}}
   */
  def fromApplyUnapplyProvider[T](applyUnapplyProvider: Any): GenCodec[T] = ???

  def applyUnapplyCodec[T]: ApplyUnapplyCodec[T] = ???

  /**
   * Materializes a [[GenCodec]] for a POJO that has a fluent builder. The fluent builder must have setters
   * corresponding to the POJO's getters. Each setter must return the builder itself (because it's fluent). The builder
   * is assumed to have default value for each field. These values are considered "transient", i.e. the codec will omit
   * them during serialization, similarly to [[transientDefault]] annotation in case classes.
   *
   * @param newBuilder
   *   an expression that creates a fresh builder
   * @param build
   *   a function that builds the final value (typically `_.build()` or `_.get()`)
   */
  def fromJavaBuilder[T, B](newBuilder: => B)(build: B => T): GenCodec[T] = ???

  @explicitGenerics
  def read[T: GenCodec](input: Input): T =
    apply[T].read(input)

  def write[T: GenCodec](output: Output, value: T): Unit =
    apply[T].write(output, value)

  def create[T](readFun: Input => T, writeFun: (Output, T) => Any): GenCodec[T] =
    new GenCodec[T] {
      def write(output: Output, value: T): Unit = writeFun(output, value)

      def read(input: Input): T = readFun(input)
    }

  def makeLazy[T](codec: => GenCodec[T]): GenCodec[T] = new GenCodec[T] {
    private lazy val underlying = codec

    def read(input: Input): T = underlying.read(input)

    def write(output: Output, value: T): Unit = underlying.write(output, value)
  }

  def transformed[T, R: GenCodec](toRaw: T => R, fromRaw: R => T): GenCodec[T] =
    new Transformed[T, R](GenCodec[R], toRaw, fromRaw)

  def nullSafe[T](readFun: Input => T, writeFun: (Output, T) => Any, allowNull: Boolean): GenCodec[T] =
    new NullSafeCodec[T] {
      def nullable: Boolean = allowNull

      def readNonNull(input: Input): T = readFun(input)

      def writeNonNull(output: Output, value: T): Unit = writeFun(output, value)
    }

  def nullable[T <: AnyRef](readFun: Input => T, writeFun: (Output, T) => Any): GenCodec[T] =
    nullSafe(readFun, writeFun, allowNull = true)

  def nonNull[T](readFun: Input => T, writeFun: (Output, T) => Any): GenCodec[T] =
    nullSafe(readFun, writeFun, allowNull = false)

  def nonNullString[T](readFun: String => T, writeFun: T => String): GenCodec[T] =
    nonNullSimple(i => readFun(i.readString()), (o, v) => o.writeString(writeFun(v)))

  def nullableString[T <: AnyRef](readFun: String => T, writeFun: T => String): GenCodec[T] =
    nullableSimple(i => readFun(i.readString()), (o, v) => o.writeString(writeFun(v)))

  def createSimple[T](readFun: SimpleInput => T, writeFun: (SimpleOutput, T) => Any, allowNull: Boolean): GenCodec[T] =
    new SimpleCodec[T] {
      def nullable: Boolean = allowNull

      def readSimple(input: SimpleInput): T = readFun(input)

      def writeSimple(output: SimpleOutput, value: T): Unit = writeFun(output, value)
    }

  def nullableSimple[T <: AnyRef](readFun: SimpleInput => T, writeFun: (SimpleOutput, T) => Any): GenCodec[T] =
    createSimple(readFun, writeFun, allowNull = true)

  def nonNullSimple[T](readFun: SimpleInput => T, writeFun: (SimpleOutput, T) => Any): GenCodec[T] =
    createSimple(readFun, writeFun, allowNull = false)

  def createList[T](readFun: ListInput => T, writeFun: (ListOutput, T) => Any, allowNull: Boolean): GenCodec[T] =
    new ListCodec[T] {
      def nullable: Boolean = allowNull

      def readList(input: ListInput): T = readFun(input)

      def writeList(output: ListOutput, value: T): Unit = writeFun(output, value)
    }

  def nullableList[T <: AnyRef](readFun: ListInput => T, writeFun: (ListOutput, T) => Any): GenCodec[T] =
    createList(readFun, writeFun, allowNull = true)

  def nonNullList[T](readFun: ListInput => T, writeFun: (ListOutput, T) => Any): GenCodec[T] =
    createList(readFun, writeFun, allowNull = false)

  /**
   * Helper method to manually implement a `GenCodec` that writes an object. NOTE: in most cases the easiest way to have
   * a custom object codec is to manually implement `apply` and `unapply`/`unapplySeq` methods in companion object of
   * your type or use [[fromApplyUnapplyProvider]] if the type comes from a third party code and you can't modify its
   * companion object.
   */
  def createObject[T](
    readFun: ObjectInput => T,
    writeFun: (ObjectOutput, T) => Any,
    allowNull: Boolean,
  ): GenObjectCodec[T] =
    new ObjectCodec[T] {
      def nullable: Boolean = allowNull

      def readObject(input: ObjectInput): T = readFun(input)

      def writeObject(output: ObjectOutput, value: T): Unit = writeFun(output, value)
    }

  def nullableObject[T <: AnyRef](readFun: ObjectInput => T, writeFun: (ObjectOutput, T) => Any): GenObjectCodec[T] =
    createObject(readFun, writeFun, allowNull = true)

  def nonNullObject[T](readFun: ObjectInput => T, writeFun: (ObjectOutput, T) => Any): GenObjectCodec[T] =
    createObject(readFun, writeFun, allowNull = false)

  def fromKeyCodec[T](implicit keyCodec: GenKeyCodec[T]): GenCodec[T] = create(
    input => keyCodec.read(input.readSimple().readString()),
    (output, value) => output.writeSimple().writeString(keyCodec.write(value)),
  )

  def forSealedEnum[T]: GenCodec[T] = ???

  open class ReadFailure(msg: String, cause: Throwable) extends RuntimeException(msg, cause) {
    def this(msg: String) = this(msg, null)

    override def fillInStackTrace(): Throwable = if cause == null then super.fillInStackTrace() else this
  }

  final case class MissingField(typeRepr: String, fieldName: String)
    extends ReadFailure(s"Cannot read $typeRepr, field $fieldName is missing in decoded data")

  final case class UnknownCase(typeRepr: String, caseName: String)
    extends ReadFailure(s"Cannot read $typeRepr, unknown case: $caseName")

  final case class MissingCase(typeRepr: String, caseFieldName: String, fieldToRead: Opt[String])
    extends ReadFailure(fieldToRead match
      case Opt(fr) => s"Cannot read field $fr of $typeRepr before $caseFieldName field is read"
      case Opt.Empty => s"Cannot read $typeRepr, $caseFieldName field is missing",
    )

  final case class NotSingleField(typeRepr: String, empty: Boolean)
    extends ReadFailure(
      s"Cannot read $typeRepr, expected object with exactly one field but got " +
        (if empty then "empty object" else "more than one"),
    )

  final case class CaseReadFailed(typeRepr: String, caseName: String, cause: Throwable)
    extends ReadFailure(s"Failed to read case $caseName of $typeRepr", cause)

  final case class FieldReadFailed(typeRepr: String, fieldName: String, cause: Throwable)
    extends ReadFailure(s"Failed to read field $fieldName of $typeRepr", cause)

  final case class ListElementReadFailed(idx: Int, cause: Throwable)
    extends ReadFailure(s"Failed to read list element at index $idx", cause)

  final case class MapFieldReadFailed(fieldName: String, cause: Throwable)
    extends ReadFailure(s"Failed to read map field $fieldName", cause)

  open class WriteFailure(msg: String, cause: Throwable) extends RuntimeException(msg, cause) {
    def this(msg: String) = this(msg, null)

    override def fillInStackTrace(): Throwable = if cause == null then super.fillInStackTrace() else this
  }

  final case class UnknownWrittenCase[T](typeRepr: String, value: T)
    extends WriteFailure(s"Failed to write $typeRepr: value $value does not match any of known subtypes")

  final case class UnapplyFailed(typeRepr: String)
    extends WriteFailure(s"Could not write $typeRepr, unapply/unapplySeq returned false or empty value")

  final case class CaseWriteFailed(typeRepr: String, caseName: String, cause: Throwable)
    extends WriteFailure(s"Failed to write case $caseName of $typeRepr", cause)

  final case class FieldWriteFailed(typeRepr: String, fieldName: String, cause: Throwable)
    extends WriteFailure(s"Failed to write field $fieldName of $typeRepr", cause)

  final case class ListElementWriteFailed(idx: Int, cause: Throwable)
    extends WriteFailure(s"Failed to write list element at index $idx", cause)

  final case class MapFieldWriteFailed(fieldName: String, cause: Throwable)
    extends WriteFailure(s"Failed to write map field $fieldName", cause)

  final class Deferred[T] extends DeferredInstance[GenCodec[T]] with GenCodec[T] {
    def read(input: Input): T = underlying.read(input)

    def write(output: Output, value: T): Unit = underlying.write(output, value)
  }

  trait NullSafeCodec[T] extends GenCodec[T] {
    def nullable: Boolean

    def readNonNull(input: Input): T

    def writeNonNull(output: Output, value: T): Unit

    final override def write(output: Output, value: T): Unit =
      if value == null then if nullable then output.writeNull() else throw new WriteFailure("null")
      else writeNonNull(output, value)

    final override def read(input: Input): T =
      if input.readNull() then if nullable then null.asInstanceOf[T] else throw new ReadFailure("null")
      else readNonNull(input)
  }

  trait SimpleCodec[T] extends NullSafeCodec[T] {
    def readSimple(input: SimpleInput): T

    def writeSimple(output: SimpleOutput, value: T): Unit

    final def writeNonNull(output: Output, value: T): Unit =
      writeSimple(output.writeSimple(), value)

    final def readNonNull(input: Input): T =
      readSimple(input.readSimple())
  }

  trait ListCodec[T] extends NullSafeCodec[T] {
    def readList(input: ListInput): T

    def writeList(output: ListOutput, value: T): Unit

    final def writeNonNull(output: Output, value: T): Unit = {
      val lo = output.writeList()
      writeList(lo, value)
      lo.finish()
    }

    final def readNonNull(input: Input): T = {
      val li = input.readList()
      val result = readList(li)
      li.skipRemaining()
      result
    }
  }

  /**
   * Convenience base class for `GenCodec`s that serialize values as objects. NOTE: if you need to implement a custom
   * `GenCodec` that writes an object, the best way to do it is to have manually implemented `apply` and `unapply` in
   * companion object or by using [[GenCodec.fromApplyUnapplyProvider]].
   */
  trait ObjectCodec[T] extends GenObjectCodec[T] with NullSafeCodec[T] {
    def readObject(input: ObjectInput): T

    def writeObject(output: ObjectOutput, value: T): Unit

    final def writeNonNull(output: Output, value: T): Unit = {
      val oo = output.writeObject()
      writeObject(oo, value)
      oo.finish()
    }

    final def readNonNull(input: Input): T = {
      val oi = input.readObject()
      val result = readObject(oi)
      oi.skipRemaining()
      result
    }
  }

  trait SizedCodec[T] extends GenCodec[T] {
    def size(value: T): Int

    protected final def declareSizeFor(output: SequentialOutput, value: T): Unit =
      if output.sizePolicy != SizePolicy.Ignored then output.declareSize(size(value))
  }

  trait OOOFieldsObjectCodec[T] extends ObjectCodec[T] with SizedCodec[T] {
    def readObject(input: ObjectInput, outOfOrderFields: FieldValues): T

    def writeFields(output: ObjectOutput, value: T): Unit

    final def readObject(input: ObjectInput): T =
      readObject(input, FieldValues.Empty)

    final def writeObject(output: ObjectOutput, value: T): Unit = {
      declareSizeFor(output, value)
      writeFields(output, value)
    }
  }

  object OOOFieldsObjectCodec {
    // this was introduced so that transparent wrapper cases are possible in flat sealed hierarchies
    final class Transformed[A, B](val wrapped: OOOFieldsObjectCodec[B], onWrite: A => B, onRead: B => A)
      extends OOOFieldsObjectCodec[A] {
      def size(value: A): Int =
        wrapped.size(onWrite(value))

      def readObject(input: ObjectInput, outOfOrderFields: FieldValues): A =
        onRead(wrapped.readObject(input, outOfOrderFields))

      def writeFields(output: ObjectOutput, value: A): Unit =
        wrapped.writeFields(output, onWrite(value))

      def nullable: Boolean = wrapped.nullable
    }

    implicit def fromTransparentWrapping[R, T](implicit
      tw: TransparentWrapping[R, T],
      wrapped: OOOFieldsObjectCodec[R],
    ): OOOFieldsObjectCodec[T] =
      new Transformed(wrapped, tw.unwrap, tw.wrap)
  }

  final class Transformed[A, B](val wrapped: GenCodec[B], onWrite: A => B, onRead: B => A) extends GenCodec[A] {
    def read(input: Input): A = {
      val wrappedValue = wrapped.read(input)
      try onRead(wrappedValue)
      catch case NonFatal(cause) => throw new ReadFailure(s"onRead conversion failed", cause)
    }

    def write(output: Output, value: A): Unit = {
      val wrappedValue =
        try onWrite(value)
        catch case NonFatal(cause) => throw new WriteFailure(s"onWrite conversion failed", cause)
      wrapped.write(output, wrappedValue)
    }
  }

  def underlyingCodec(codec: GenCodec[?]): GenCodec[?] = codec match
    case tc: Transformed[?, ?] => underlyingCodec(tc.wrapped)
    case _ => codec

  class SubclassCodec[T: ClassTag, S >: T: GenCodec](val nullable: Boolean) extends NullSafeCodec[T] {
    override def readNonNull(input: Input): T = GenCodec.read[S](input) match
      case sub: T => sub
      case v => throw new ReadFailure(s"$v is not an instance of ${classTag[T].runtimeClass}")

    override def writeNonNull(output: Output, value: T): Unit = GenCodec.write[S](output, value)
  }

  final val DefaultCaseField = "_case"

  private def notNull: Nothing = throw new ReadFailure("not null")

  given NothingCodec: GenCodec[Nothing] =
    create[Nothing](_ => throw new ReadFailure("read Nothing"), (_, _) => throw new WriteFailure("write Nothing"))

  given NullCodec: GenCodec[Null] =
    create[Null](i => if i.readNull() then null else notNull, (o, _) => o.writeNull())

  given UnitCodec: GenCodec[Unit] =
    create[Unit](i => if i.readNull() then () else notNull, (o, _) => o.writeNull())

  given VoidCodec: GenCodec[Void] =
    create[Void](i => if i.readNull() then null else notNull, (o, _) => o.writeNull())

  given BooleanCodec: GenCodec[Boolean] = nonNullSimple(_.readBoolean(), _ writeBoolean _)

  given CharCodec: GenCodec[Char] = nonNullSimple(_.readChar(), _ writeChar _)

  given ByteCodec: GenCodec[Byte] = nonNullSimple(_.readByte(), _ writeByte _)

  given ShortCodec: GenCodec[Short] = nonNullSimple(_.readShort(), _ writeShort _)

  given IntCodec: GenCodec[Int] = nonNullSimple(_.readInt(), _ writeInt _)

  given LongCodec: GenCodec[Long] = nonNullSimple(_.readLong(), _ writeLong _)

  given FloatCodec: GenCodec[Float] = nonNullSimple(_.readFloat(), _ writeFloat _)

  given DoubleCodec: GenCodec[Double] = nonNullSimple(_.readDouble(), _ writeDouble _)

  given BigIntCodec: GenCodec[BigInt] = nullableSimple(_.readBigInt(), _ writeBigInt _)

  given BigDecimalCodec: GenCodec[BigDecimal] = nullableSimple(_.readBigDecimal(), _ writeBigDecimal _)

  given JBooleanCodec: GenCodec[JBoolean] = nullableSimple(_.readBoolean(), _ writeBoolean _)

  given JCharacterCodec: GenCodec[JCharacter] = nullableSimple(_.readChar(), _ writeChar _)

  given JByteCodec: GenCodec[JByte] = nullableSimple(_.readByte(), _ writeByte _)

  given JShortCodec: GenCodec[JShort] = nullableSimple(_.readShort(), _ writeShort _)

  given JIntegerCodec: GenCodec[JInteger] = nullableSimple(_.readInt(), _ writeInt _)

  given JLongCodec: GenCodec[JLong] = nullableSimple(_.readLong(), _ writeLong _)

  given JFloatCodec: GenCodec[JFloat] = nullableSimple(_.readFloat(), _ writeFloat _)

  given JDoubleCodec: GenCodec[JDouble] = nullableSimple(_.readDouble(), _ writeDouble _)

  given JBigIntegerCodec: GenCodec[JBigInteger] =
    nullableSimple(_.readBigInt().bigInteger, (o, v) => o.writeBigInt(BigInt(v)))

  given JBigDecimalCodec: GenCodec[JBigDecimal] =
    nullableSimple(_.readBigDecimal().bigDecimal, (o, v) => o.writeBigDecimal(BigDecimal(v)))

  given JDateCodec: GenCodec[JDate] =
    nullableSimple(i => new JDate(i.readTimestamp()), (o, d) => o.writeTimestamp(d.getTime))

  given StringCodec: GenCodec[String] =
    nullableSimple(_.readString(), _ writeString _)

  given SymbolCodec: GenCodec[Symbol] =
    nullableSimple(i => Symbol(i.readString()), (o, s) => o.writeString(s.name))

  given ByteArrayCodec: GenCodec[Array[Byte]] =
    nullableSimple(_.readBinary(), _ writeBinary _)

  given UuidCodec: GenCodec[UUID] =
    nullableSimple(i => UUID.fromString(i.readString()), (o, v) => o.writeString(v.toString))

  given TimestampCodec: GenCodec[Timestamp] =
    GenCodec.nonNullSimple(i => Timestamp(i.readTimestamp()), (o, t) => o.writeTimestamp(t.millis))

  given BytesCodec: GenCodec[Bytes] =
    GenCodec.nullableSimple(i => Bytes(i.readBinary()), (o, b) => o.writeBinary(b.bytes))

  extension [A](coll: BIterable[A]) {
    private def writeToList(lo: ListOutput)(using writer: GenCodec[A]): Unit = {
      lo.declareSizeOf(coll)
      coll.foreach(new (A => Unit) {
        private var idx = 0

        def apply(a: A): Unit = {
          try writer.write(lo.writeElement(), a)
          catch case NonFatal(e) => throw ListElementWriteFailed(idx, e)
          idx += 1
        }
      })
    }
  }

  extension [A, B](coll: BIterable[(A, B)]) {

    private def writeToObject(oo: ObjectOutput)(using keyWriter: GenKeyCodec[A], writer: GenCodec[B]): Unit = {
      oo.declareSizeOf(coll)
      coll.foreach { case (key, value) =>
        val fieldName = keyWriter.write(key)
        try writer.write(oo.writeField(fieldName), value)
        catch case NonFatal(e) => throw MapFieldWriteFailed(fieldName, e)
      }
    }
  }

  extension (li: ListInput) {

    private def collectTo[A: GenCodec, C](using fac: Factory[A, C]): C = {
      val b = fac.newBuilder
      li.knownSize match
        case -1 =>
        case size => b.sizeHint(size)
      var idx = 0
      while li.hasNext do {
        val a =
          try read[A](li.nextElement())
          catch case NonFatal(e) => throw ListElementReadFailed(idx, e)
        b += a
        idx += 1
      }
      b.result()
    }
  }

  extension (oi: ObjectInput) {

    private def collectTo[K: GenKeyCodec, V: GenCodec, C](using fac: Factory[(K, V), C]): C = {
      val b = fac.newBuilder
      oi.knownSize match
        case -1 =>
        case size => b.sizeHint(size)
      while oi.hasNext do {
        val fi = oi.nextField()
        val entry =
          try (GenKeyCodec.read[K](fi.fieldName), read[V](fi))
          catch case NonFatal(e) => throw MapFieldReadFailed(fi.fieldName, e)
        b += entry
      }
      b.result()
    }
  }

  given arrayCodec[T: ClassTag: GenCodec]: GenCodec[Array[T]] =
    nullableList[Array[T]](
      _.iterator(read[T]).toArray[T],
      (lo, arr) => {
        lo.declareSize(arr.length)

        @tailrec def loop(idx: Int): Unit =
          if idx < arr.length then {
            GenCodec.write(lo.writeElement(), arr(idx))
            loop(idx + 1)
          }

        loop(0)
      },
    )

  // these are covered by the generic `seqCodec` and `setCodec` but making them explicit may be easier
  // for the compiler and also make IntelliJ less confused
  given bseqCodec[T: GenCodec]: GenCodec[BSeq[T]] =
    seqCodec[BSeq, T](using GenCodec[T], implicitly[Factory[T, List[T]]])

  given iseqCodec[T: GenCodec]: GenCodec[ISeq[T]] =
    seqCodec[ISeq, T](using GenCodec[T], implicitly[Factory[T, List[T]]])

  given mseqCodec[T: GenCodec]: GenCodec[MSeq[T]] = seqCodec[MSeq, T]

  given bindexedSeqCodec[T: GenCodec]: GenCodec[BIndexedSeq[T]] = seqCodec[BIndexedSeq, T]

  given iindexedSeqCodec[T: GenCodec]: GenCodec[IIndexedSeq[T]] = seqCodec[IIndexedSeq, T]

  given mindexedSeqCodec[T: GenCodec]: GenCodec[MIndexedSeq[T]] = seqCodec[MIndexedSeq, T]

  given listCodec[T: GenCodec]: GenCodec[List[T]] = seqCodec[List, T]

  given vectorCodec[T: GenCodec]: GenCodec[Vector[T]] = seqCodec[Vector, T]

  given bsetCodec[T: GenCodec]: GenCodec[BSet[T]] = setCodec[BSet, T]

  given isetCodec[T: GenCodec]: GenCodec[ISet[T]] = setCodec[ISet, T]

  given msetCodec[T: GenCodec]: GenCodec[MSet[T]] = setCodec[MSet, T]

  given ihashSetCodec[T: GenCodec]: GenCodec[IHashSet[T]] = setCodec[IHashSet, T]

  given mhashSetCodec[T: GenCodec]: GenCodec[MHashSet[T]] = setCodec[MHashSet, T]

  implicit def seqCodec[C[X] <: BSeq[X], T: GenCodec](implicit fac: Factory[T, C[T]]): GenCodec[C[T]] =
    nullableList[C[T] & BSeq[T]](_.collectTo[T, C[T]], (lo, c) => c.writeToList(lo))

  implicit def setCodec[C[X] <: BSet[X], T: GenCodec](implicit fac: Factory[T, C[T]]): GenCodec[C[T]] =
    nullableList[C[T] & BSet[T]](_.collectTo[T, C[T]], (lo, c) => c.writeToList(lo))

  implicit def jCollectionCodec[C[X] <: JCollection[X], T: GenCodec](implicit cbf: JFactory[T, C[T]]): GenCodec[C[T]] =
    nullableList[C[T]](_.collectTo[T, C[T]], (lo, c) => c.asScala.writeToList(lo))

  implicit def mapCodec[M[X, Y] <: BMap[X, Y], K: GenKeyCodec, V: GenCodec](implicit
    fac: Factory[(K, V), M[K, V]],
  ): GenObjectCodec[M[K, V]] =
    nullableObject[M[K, V]](_.collectTo[K, V, M[K, V]], (oo, value) => value.writeToObject(oo))

  implicit def jMapCodec[M[X, Y] <: JMap[X, Y], K: GenKeyCodec, V: GenCodec](implicit
    cbf: JFactory[(K, V), M[K, V]],
  ): GenObjectCodec[M[K, V]] =
    nullableObject[M[K, V]](_.collectTo[K, V, M[K, V]], (oo, value) => value.asScala.writeToObject(oo))

  given optionCodec[T: GenCodec]: GenCodec[Option[T]] = create[Option[T]](
    input =>
      if input.legacyOptionEncoding then {
        val li = input.readList()
        val res = if li.hasNext then Some(read[T](li.nextElement())) else None
        li.skipRemaining()
        res
      } else if input.readNull() then None
      else Some(read[T](input)),
    (output, valueOption) =>
      if output.legacyOptionEncoding then {
        val lo = output.writeList()
        valueOption.foreach(v => write[T](lo.writeElement(), v))
        lo.finish()
      } else
        valueOption match
          case Some(v) => write[T](output, v)
          case None => output.writeNull(),
  )

  given nOptCodec[T: GenCodec]: GenCodec[NOpt[T]] =
    new Transformed[NOpt[T], Option[T]](optionCodec[T], _.toOption, _.toNOpt)

  given optCodec[T: GenCodec]: GenCodec[Opt[T]] =
    create[Opt[T]](
      i => if i.readNull() then Opt.Empty else Opt(read[T](i)),
      (o, vo) =>
        vo match
          case Opt(v) => write[T](o, v)
          case Opt.Empty => o.writeNull(),
    )

  given optArgCodec[T: GenCodec]: GenCodec[OptArg[T]] =
    new Transformed[OptArg[T], Opt[T]](optCodec[T], _.toOpt, _.toOptArg)

  given optRefCodec[T >: Null: GenCodec]: GenCodec[OptRef[T]] =
    new Transformed[OptRef[T], Opt[T]](optCodec[T], _.toOpt, _.toOptRef)

  given eitherCodec[A: GenCodec, B: GenCodec]: GenCodec[Either[A, B]] = nullableObject(
    oi => {
      val fi = oi.nextField()
      fi.fieldName match
        case "Left" => Left(read[A](fi))
        case "Right" => Right(read[B](fi))
        case name => throw new ReadFailure(s"Expected field 'Left' or 'Right', got $name")
    },
    (oo, v) => {
      oo.declareSize(1)
      v match
        case Left(a) => write[A](oo.writeField("Left"), a)
        case Right(b) => write[B](oo.writeField("Right"), b)
    },
  )

  given jEnumCodec[E <: Enum[E]: ClassTag]: GenCodec[E] = nullableSimple(
    in => Enum.valueOf(classTag[E].runtimeClass.asInstanceOf[Class[E]], in.readString()),
    (out, value) => out.writeString(value.name),
  )

  // Warning! Changing the order of implicit params of this method causes divergent implicit expansion (WTF?)
  implicit def fromTransparentWrapping[R, T](using
    tw: TransparentWrapping[R, T],
    wrappedCodec: GenCodec[R],
  ): GenCodec[T] =
    new Transformed(wrappedCodec, tw.unwrap, tw.wrap)

  implicit def fromFallback[T](using fallback: Fallback[GenCodec[T]]): GenCodec[T] =
    fallback.value
}

trait RecursiveAutoCodecs { this: GenCodec.type =>

  /**
   * Like `materialize`, but descends into types that `T` is made of (e.g. case class field types).
   */
  def materializeRecursively[T]: GenCodec[T] = ???

  /**
   * INTERNAL API. Should not be used directly.
   */
  implicit def materializeImplicitly[T](using allow: AllowImplicitMacro[GenCodec[T]]): GenCodec[T] = ???
}
