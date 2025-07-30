package com.avsystem.commons
package rpc

import com.avsystem.commons.misc.{NamedEnum, NamedEnumCompanion}
import com.avsystem.commons.serialization.GenCodec

sealed abstract class Tag[T](using val codec: GenCodec[T]) extends NamedEnum with Product {
  def name: String = productPrefix
}
object Tag extends NamedEnumCompanion[Tag[?]] {
  def apply[T](using tag: Tag[T]): Tag[T] = tag

  case object String extends Tag[String]
  given String.type = String
  case object Int extends Tag[Int]
  given Int.type = Int

  val values: ISeq[Tag[?]] = caseObjects

  given tagCodec[T]: GenCodec[Tag[T]] =
    codec.asInstanceOf[GenCodec[Tag[T]]]
}
