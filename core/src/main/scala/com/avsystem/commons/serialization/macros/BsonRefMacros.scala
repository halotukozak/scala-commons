package com.avsystem.commons
package serialization.macros

import scala.quoted.Expr

class BsonRefMacros {
  def bsonRef[S: Type, T: Type](fun: Expr[S => T]): Expr[BsonRef] =
    '{ BsonRef(GenRef.create[S].ref[T]($fun)) }
}
