package com.avsystem.commons
package mongo

import com.avsystem.commons.mongo.core.ops.{BsonRefFiltering, BsonRefIterableFiltering, BsonRefIterableUpdating, BsonRefSorting, BsonRefUpdating}
import com.avsystem.commons.serialization.RawRef.Field
import com.avsystem.commons.serialization.{GenCodec, GenRef}

case class BsonRef[S, T](path: String, codec: GenCodec[T], getter: S => T)
object BsonRef {
  val BsonKeySeparator = "."

  def create[S]: Creator[S] = new Creator[S] {}

  trait Creator[S] {
    type Ref[T] = BsonRef[S, T]

    // there is an implicit macro conversion between lambdas and GenRefs
    protected[this] def ref[T](fun: S => T): BsonRef[S, T] = macro macros.serialization.BsonRefMacros.bsonRef[S, T]
  }

  def apply[S, T](genRef: GenRef[S, T])(implicit codec: GenCodec[T]): BsonRef[S, T] = {
    val path = genRef.rawRef.normalize.map {
      case Field(name) => KeyEscaper.escape(name)
    }.mkString(BsonKeySeparator)

    BsonRef(path, codec, genRef.fun)
  }

  implicit def bsonRefIterableUpdating[S, E: GenCodec, C[T] <: Iterable[T]](bsonRef: BsonRef[S, C[E]]): BsonRefIterableUpdating[E, C] = {
    new BsonRefIterableUpdating[E, C](bsonRef)
  }
  implicit def bsonRefUpdating[S, T](bsonRef: BsonRef[S, T]): BsonRefUpdating[T] = new BsonRefUpdating(bsonRef)
  implicit def bsonRefSorting[S, T](bsonRef: BsonRef[S, T]): BsonRefSorting[T] = new BsonRefSorting(bsonRef)

  implicit def bsonRefIterableFiltering[S, E: GenCodec, C[T] <: Iterable[T]](bsonRef: BsonRef[S, C[E]]): BsonRefIterableFiltering[E, C] = {
    new BsonRefIterableFiltering[E, C](bsonRef)
  }
  implicit def bsonRefFiltering[S, T](bsonRef: BsonRef[S, T]): BsonRefFiltering[T] = new BsonRefFiltering(bsonRef)
}
