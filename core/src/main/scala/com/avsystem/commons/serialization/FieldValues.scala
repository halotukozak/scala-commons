package com.avsystem.commons
package serialization

import meta.OptionLike
import serialization.GenCodec.{FieldReadFailed, ReadFailure}

import scala.annotation.tailrec

object FieldValues {
  final val Empty = new FieldValues(new Array(0), new Array(0))

  private object NullMarker
}

final class FieldValues(
  private val fieldNames: Array[String],
  codecs: Array[GenCodec[?]],
  ofWhat: OptArg[String] = OptArg.Empty,
) {

  @tailrec private def fieldIndex(fieldName: String, idx: Int): Int =
    if idx >= fieldNames.length then -1
    else if fieldNames(idx) == fieldName then idx
    else fieldIndex(fieldName, idx + 1)

  private val values = new Array[Any](fieldNames.length)

  import FieldValues.*

  def readField(input: FieldInput): Unit = if !tryReadField(input) then input.skip()

  def tryReadField(input: FieldInput): Boolean = fieldIndex(input.fieldName, 0) match
    case -1 => false
    case idx =>
      val value =
        try codecs(idx).read(input)
        catch
          case NonFatal(e) =>
            ofWhat match
              case OptArg(typeRepr) => throw FieldReadFailed(typeRepr, input.fieldName, e)
              case OptArg.Empty => throw new ReadFailure(s"Failed to read field ${input.fieldName}", e)
      values(idx) = if value == null then NullMarker else value
      true

  def getOrElse[T](idx: Int, default: => T): T = {
    val res = values(idx)
    if res == null then default
    else if res.asInstanceOf[AnyRef] eq NullMarker then null.asInstanceOf[T]
    else res.asInstanceOf[T]
  }

  def getOpt[O, T](idx: Int, optionLike: OptionLike.Aux[O, T]): O = {
    val res = values(idx)
    if res == null then optionLike.none
    else if res.asInstanceOf[AnyRef] eq NullMarker then optionLike.some(null.asInstanceOf[T])
    else optionLike.some(res.asInstanceOf[T])
  }

  def rewriteFrom(other: FieldValues): Unit = {
    var oi = 0
    while oi < other.fieldNames.length do {
      other.values(oi) match
        case null =>
        case value =>
          fieldIndex(other.fieldNames(oi), 0) match
            case -1 =>
            case i => values(i) = value
      oi += 1
    }
  }
}
