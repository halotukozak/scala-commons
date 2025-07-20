package com.avsystem.commons
package mongo.typed

import com.avsystem.commons.annotation.explicitGenerics
import com.avsystem.commons.meta.MacroInstances
import com.avsystem.commons.mongo.BsonGenCodecs
import com.avsystem.commons.serialization.GenObjectCodec

import scala.annotation.{compileTimeOnly, implicitNotFound}

trait MongoAdtInstances[T] {
  def codec: GenObjectCodec[T]
  def format: MongoAdtFormat[T]
}

trait MongoEntityInstances[IDType, E <: BaseMongoEntity.Aux[IDType]] extends MongoAdtInstances[E] {
  def meta: MongoEntityMeta[IDType, E]
}

/**
  * Provides additional static validation for `as`, `is` and `ref` macros from [[DataTypeDsl]].
  * Catches mistakes when someone forgets to use [[MongoDataCompanion]] or [[MongoEntityCompanion]] for its
  * case class or sealed hierarchy.
  */
@implicitNotFound("${T} is an opaque data type - does it have a companion that extends MongoDataCompanion?")
sealed trait IsMongoAdtOrSubtype[T]

sealed abstract class BaseMongoCompanion[T] extends DataTypeDsl[T] {
  implicit def codec: GenObjectCodec[T]
  implicit def format: MongoAdtFormat[T]

  implicit def isMongoAdtOrSubtype[C <: T]: IsMongoAdtOrSubtype[C] = null

  implicit class macroDslExtensions(value: T) {
    @explicitGenerics
    @compileTimeOnly("the .as[Subtype] construct can only be used inside lambda passed to .ref(...) macro")
    def as[C <: T]: C = sys.error("stub")
  }

  final lazy val SelfRef: MongoRef[T, T] = MongoRef.RootRef(format)
}

abstract class AbstractMongoDataCompanion[Implicits, E](implicits: Implicits)(
  implicit instances: MacroInstances[Implicits, MongoAdtInstances[E]]
) extends BaseMongoCompanion[E] {
  implicit val codec: GenObjectCodec[E] = instances(implicits, this).codec
  implicit val format: MongoAdtFormat[E] = instances(implicits, this).format
}

abstract class AbstractMongoEntityCompanion[Implicits, IDType, E <: BaseMongoEntity.Aux[IDType]](implicits: Implicits)(
  implicit instances: MacroInstances[Implicits, MongoEntityInstances[IDType, E]]
) extends BaseMongoCompanion[E] {
  implicit val codec: GenObjectCodec[E] = instances(implicits, this).codec
  implicit val format: MongoAdtFormat[E] = instances(implicits, this).format
  implicit val meta: MongoEntityMeta[IDType, E] = instances(implicits, this).meta

  type ID = IDType

  final val IdRef: Ref[ID] = meta.idRef
}

/**
  * Base class for companion objects of types that represent inner documents of MongoDB entities.
  * Just like entities, inner documents may be case classes or sealed hierarchies with `@flatten` annotation.
  *
  * NOTE: It is enough for a MongoDB field type to have just `GenCodec` defined (i.e. you can get away with
  * using `HasGenCodec` instead of `MongoDataCompanion`). However, data type which only has codec will be considered
  * opaque and you won't be able to reference its inner fields in filters, updates, indices, etc.
  */
abstract class MongoDataCompanion[E](
  implicit instances: MacroInstances[BsonGenCodecs.type, MongoAdtInstances[E]]
) extends AbstractMongoDataCompanion[BsonGenCodecs.type, E](BsonGenCodecs)

/**
  * Base class for companion objects of types representing MongoDB entities.
  * Entities may be case classes or sealed hierarchies with `@flatten` annotation.
  * They must extend [[MongoEntity]].
  */
abstract class MongoEntityCompanion[IDType, E <: BaseMongoEntity.Aux[IDType]](
  implicit instances: MacroInstances[BsonGenCodecs.type, MongoEntityInstances[IDType, E]]
) extends AbstractMongoEntityCompanion[BsonGenCodecs.type, IDType, E](BsonGenCodecs)
