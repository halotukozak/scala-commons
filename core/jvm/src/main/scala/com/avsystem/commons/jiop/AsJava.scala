package com.avsystem.commons
package jiop

@java.lang.FunctionalInterface
abstract class AsJava[-ScalaType, +JavaType] extends (ScalaType => JavaType) {
  override def apply(e: ScalaType): JavaType
}

extension [S, J](e: S)(using AsJava[S, J]) {
  inline def asJava: J = summon[AsJava[S, J]].apply(e)
}
