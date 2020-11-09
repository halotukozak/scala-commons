package com.avsystem.commons
package mongo.typed

import org.bson.{BsonDocument, BsonValue}

sealed trait MongoOrder[T] {
  def toBson: BsonValue
}
object MongoOrder {
  def empty[E]: MongoDocumentOrder[E] = MongoDocumentOrder.empty[E]

  def ascending[T]: MongoOrder[T] = Simple(true)
  def descending[T]: MongoOrder[T] = Simple(false)

  def simple[T](ascending: Boolean): MongoOrder[T] = Simple(ascending)

  def apply[E](refs: (MongoPropertyRef[E, _], Boolean)*): MongoDocumentOrder[E] =
    MongoDocumentOrder(refs: _*)

  final case class Simple[T](ascending: Boolean) extends MongoOrder[T] {
    def toBson: BsonValue = Bson.int(if (ascending) 1 else -1)
  }
}

case class MongoDocumentOrder[E](refs: Vector[(MongoPropertyRef[E, _], Boolean)]) extends MongoOrder[E] {
  def andThen(other: MongoDocumentOrder[E]): MongoDocumentOrder[E] =
    MongoDocumentOrder(refs ++ other.refs)

  def andThenBy(ref: MongoPropertyRef[E, _], ascending: Boolean): MongoDocumentOrder[E] =
    MongoDocumentOrder(refs :+ (ref -> ascending))

  def andThenAscendingBy(ref: MongoPropertyRef[E, _]): MongoDocumentOrder[E] =
    andThenBy(ref, ascending = true)

  def andThenDescendingBy(ref: MongoPropertyRef[E, _]): MongoDocumentOrder[E] =
    andThenBy(ref, ascending = false)

  //TODO: lambda-macro versions of andThenBy

  def toBson: BsonDocument =
    Bson.document(refs.iterator.map { case (ref, asc) => (ref.filterPath, Bson.int(if (asc) 1 else -1)) })
}
object MongoDocumentOrder {
  def empty[E]: MongoDocumentOrder[E] = MongoDocumentOrder(Vector.empty)

  def apply[E](refs: (MongoPropertyRef[E, _], Boolean)*): MongoDocumentOrder[E] =
    MongoDocumentOrder(refs.toVector)
}
