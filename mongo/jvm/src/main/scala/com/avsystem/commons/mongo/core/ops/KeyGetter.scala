package com.avsystem.commons
package mongo.core.ops

import com.avsystem.commons.mongo.{BsonRef, DocKey}

trait KeyGetter[-T] {
  def keyOf(t: T): String
}

object KeyGetter {
  implicit object bsonRefKeyGetter extends KeyGetter[BsonRef[_, _]] {
    override def keyOf(t: BsonRef[?, ?]): String = t.path
  }

  implicit object docKeyKeyGetter extends KeyGetter[DocKey[_, _]] {
    override def keyOf(t: DocKey[?, ?]): String = t.key
  }
}
