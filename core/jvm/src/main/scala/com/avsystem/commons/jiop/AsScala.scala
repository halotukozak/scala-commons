package com.avsystem.commons
package jiop

import scala.compiletime.summonInline

@java.lang.FunctionalInterface
abstract class AsScala[-JavaType, +ScalaType] extends (JavaType => ScalaType):
  override def apply(e: JavaType): ScalaType

extension [J, S](e: J)(using AsScala[J, S])
  inline def asScala: S = summon[AsScala[J, S]].apply(e)
