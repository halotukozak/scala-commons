package com.avsystem.commons
package mongo

import com.mongodb.client.model.{Updates as U}
import org.bson.conversions.Bson

/**
 * @author
 *   MKej
 */
object Update {
  def combine(updates: Bson*): Bson = U.combine(updates.asJava)

  def set[A](key: DocKey[A, ?], value: A): Bson = U.set(key.key, key.codec.toBson(value))
  def unset(key: DocKey[?, ?]): Bson = U.unset(key.key)

  def max[A](key: DocKey[A, ?], value: A): Bson = U.max(key.key, key.codec.toBson(value))
}
