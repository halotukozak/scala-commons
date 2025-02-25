package com.avsystem.commons
package rpc

import misc.{NamedEnum, NamedEnumCompanion}
import serialization.GenCodec

sealed abstract class Tag[T](using codec: GenCodec[T]) extends NamedEnum with Product {
  def name: String = productPrefix
}
object Tag extends NamedEnumCompanion[Tag[?]] {
  def apply[T](using tag: Tag[T]): Tag[T] = tag

  implicit case object String extends Tag[String]
  implicit case object Int extends Tag[Int]

  val values: ISeq[Tag[?]] = caseObjects

  given tagCodec[T]: GenCodec[Tag[T]] =
    codec.asInstanceOf[GenCodec[Tag[T]]]
}
