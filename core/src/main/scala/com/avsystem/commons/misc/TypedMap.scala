package com.avsystem.commons
package misc

import SharedExtensions.*
import misc.TypedMap.{GenCodecMapping, KWrapper}
import serialization.*

import scala.collection.IterableOps

/**
 * A map whose keys are parameterized with value type. This makes it possible to associate different value type with
 * each key, in a type-safe way.
 *
 * [[TypedMap[K]]] has a [[GenCodec]] instance as long as there is a `GenCodec[K[_]]` instance for the key type and a
 * [[GenCodecMapping[K]]] instance that determines the codec for the value type associated with given key.
 *
 * Example:
 * {{{
 *   sealed abstract class AttributeKey[T](implicit val valueCodec: GenCodec[T])
 *     extends TypedKey[T] with AutoNamedEnum
 *
 *   object AttributeKey extends NamedEnumCompanion[AttributeKey[_]] {
 *     object StringKey extends AttributeKey[String]
 *     object IntKey extends AttributeKey[Int]
 *
 *     val values: List[AttributeKey[_]] = caseObjects
 *   }
 *
 *   val attributes = TypedMap[AttributeKey](
 *     AttributeKey.StringKey -> "foo",
 *     AttributeKey.IntKey -> 42,
 *   )
 * }}}
 *
 * Note that since all keys and value types are known statically, the map above is somewhat equivalent to a case class:
 *
 * {{{
 *   case class Attributes(
 *     string: Opt[String],
 *     int: Opt[Int]
 *   )
 * }}}
 *
 * [[TypedMap]] might be a good choice if there is a lot of attribute keys, they aren't statically known or some
 * collection-like behaviour is necessary (e.g. computing the size, iterating over all elements). A [[TypedMap]] is also
 * easier to evolve than a case class (e.g. because of binary compatibility issues).
 */

class TypedMap[K[_]](val raw: Map[KWrapper[K, ?], Any]) extends AnyVal {
  def apply[T](key: K[T]): T =
    raw(key).asInstanceOf[T]

  def get[T](key: K[T]): Option[T] =
    raw.get(key).asInstanceOf[Option[T]]

  def getOpt[T](key: K[T]): Opt[T] =
    raw.getOpt(key).asInstanceOf[Opt[T]]

  def getOrElse[T](key: K[T], defaultValue: => T): T =
    get(key).getOrElse(defaultValue)

  def updated[T](key: K[T], value: T): TypedMap[K] =
    new TypedMap[K](raw.updated(key, value))

  def ++(other: TypedMap[K]): TypedMap[K] =
    new TypedMap[K](raw ++ other.raw)

  def keys: Iterable[K[Any]] = raw.keys.map(_.value.asInstanceOf[K[Any]])

  def keysIterator: Iterator[K[Any]] = raw.keysIterator.map(_.value.asInstanceOf[K[Any]])

  def keySet: Set[K[Any]] = raw.keySet.map(_.value.asInstanceOf[K[Any]])

  def size: Int = raw.size

  override def toString = s"TypedMap($raw)"
}

object TypedMap {
  implicit final class KWrapper[K[_], T](val value: K[T] & Any) extends AnyVal

  given [K[_]]: Conversion[Map[K[Any] & Any, Any], Map[KWrapper[K, ?], Any]] = _.map[KWrapper[K, ?], Any] {
    case (k: (K[?] & Any), v) =>
      (k, v)
  }

  final case class Entry[K[_], T](pair: (KWrapper[K, T], T)) extends AnyVal

  object Entry:
    given pairToEntry[K[_], T]: Conversion[(KWrapper[K, T], T), Entry[K, T]] = Entry.apply

  def empty[K[_]]: TypedMap[K] =
    new TypedMap[K](Map.empty)

  def apply[K[_]](entries: Entry[K, ?]*): TypedMap[K] = {
    val raw = Map.newBuilder[KWrapper[K, ?], Any]
    entries.foreach(e => raw += e.pair)
    new TypedMap[K](raw.result())
  }

  trait GenCodecMapping[K[_]]:
    def valueCodec[T](key: K[T]): GenCodec[T]

  given typedMapCodec[K[_]](using
    keyCodec: GenKeyCodec[KWrapper[K, ?]],
    codecMapping: GenCodecMapping[K],
  ): GenObjectCodec[TypedMap[K]] =
    new GenCodec.ObjectCodec[TypedMap[K]] {
      def nullable = false

      def readObject(input: ObjectInput): TypedMap[K] = {
        val rawBuilder = Map.newBuilder[KWrapper[K, ?], Any]
        input.knownSize match {
          case -1 =>
          case size => rawBuilder.sizeHint(size)
        }
        while input.hasNext do {
          val fieldInput = input.nextField()
          val key = keyCodec.read(fieldInput.fieldName)
          rawBuilder += ((key, codecMapping.valueCodec(key.asInstanceOf[K[Nothing]]).read(fieldInput)))
        }
        new TypedMap[K](rawBuilder.result())
      }

      def writeObject(output: ObjectOutput, typedMap: TypedMap[K]): Unit = {
        output.declareSizeOf(typedMap.raw)
        typedMap.raw.foreach { case (key, value) =>
          val valueCodec = codecMapping.valueCodec(key.asInstanceOf[K[Any]])
          valueCodec.write(output.writeField(keyCodec.write(key)), value)
        }
      }
    }
}

/**
 * Base class for key types of [[TypedMap]] (typically enums parameterized by value type). Provides an instance of
 * [[GenCodecMapping]] which is necessary for [[GenCodec]] instance for [[TypedMap]] that uses this key type.
 */
trait TypedKey[T]:
  def valueCodec: GenCodec[T]

object TypedKey:
  given codecMapping[K[X] <: TypedKey[X]]: GenCodecMapping[K] =
    new GenCodecMapping[K] {
      def valueCodec[T](key: K[T]): GenCodec[T] = key.valueCodec
    }
