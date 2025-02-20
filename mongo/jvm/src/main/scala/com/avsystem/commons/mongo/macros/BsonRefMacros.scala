package com.avsystem.commons
package serialization.macros

import scala.quoted.Expr

import com.avsystem.commons.mongo.BsonRef

class BsonRefMacros {
  def bsonRef[S: Type, T: Type](fun: Expr[S => T]): Expr[BsonRef] =
    '{ BsonRef(GenRef.create[S].ref[T]($fun)) }
}
