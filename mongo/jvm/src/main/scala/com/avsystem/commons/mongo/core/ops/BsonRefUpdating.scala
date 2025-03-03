package com.avsystem.commons
package mongo.core.ops

import com.avsystem.commons.mongo.BsonRef

class BsonRefUpdating[T](val bsonRef: BsonRef[?, T]) extends AnyVal with BaseUpdating[T] with BsonRefKeyValueHandling[T]
