package com.avsystem.commons
package jiop

import macros.specializedOptionAsJava

import java.util as ju
import scala.quoted.Type

trait JOptionalUtils {

  type JOptional[T] = ju.Optional[T]
  type JOptionalDouble = ju.OptionalDouble
  type JOptionalInt = ju.OptionalInt
  type JOptionalLong = ju.OptionalLong

  type ReturnOfOptionalAsJava[T] = T match
    case Int => JOptionalInt
    case Long => JOptionalLong
    case Double => JOptionalDouble
    case _ => JOptional[T]

  given [T]: AsScala[JOptional[T], Option[T]] = _.toOption

  inline given [T]: AsJava[Option[T], ReturnOfOptionalAsJava[T]] = specializedOptionAsJava[T]

  given AsScala[JOptionalDouble, Option[Double]] = _.toOption

  given AsScala[JOptionalInt, Option[Int]] = _.toOption

  given AsScala[JOptionalLong, Option[Long]] = _.toOption

  object JOptional {
    inline def apply[T](nullable: T): JOptional[T] = ju.Optional.ofNullable(nullable)

    inline def empty[T]: JOptional[T] = ju.Optional.empty[T]()
  }

  object JOptionalDouble {
    inline def apply(value: Double): JOptionalDouble = ju.OptionalDouble.of(value)

    inline def empty: JOptionalDouble = ju.OptionalDouble.empty()
  }

  object JOptionalInt {
    inline def apply(value: Int): JOptionalInt = ju.OptionalInt.of(value)

    inline def empty: JOptionalInt = ju.OptionalInt.empty()
  }

  object JOptionalLong {
    inline def apply(value: Long): JOptionalLong = ju.OptionalLong.of(value)

    inline def empty: JOptionalLong = ju.OptionalLong.empty()
  }

  extension [T](optional: JOptional[T]) {
    inline def toOption: Option[T] =
      if optional.isPresent then Some(optional.get) else None

    inline def toOpt: Opt[T] =
      if optional.isPresent then Opt(optional.get) else Opt.Empty
  }

  extension (optional: JOptionalDouble) {
    inline def toOption: Option[Double] =
      if optional.isPresent then Some(optional.getAsDouble) else None

    inline def toOpt: Opt[Double] =
      if optional.isPresent then Opt(optional.getAsDouble) else Opt.Empty
  }

  extension (optional: JOptionalInt) {
    inline def toOption: Option[Int] =
      if optional.isPresent then Some(optional.getAsInt) else None

    inline def toOpt: Opt[Int] =
      if optional.isPresent then Opt(optional.getAsInt) else Opt.Empty
  }

  extension (optional: JOptionalLong) {
    inline def toOption: Option[Long] =
      if optional.isPresent then Some(optional.getAsLong) else None

    inline def toOpt: Opt[Long] =
      if optional.isPresent then Opt(optional.getAsLong) else Opt.Empty
  }

  extension [T](option: Option[T]) {
    /**
     * Note that in scala Some(null) is valid value. It will throw an exception in such case, because java Optional
     * is not able to hold null
     */
    inline def toJOptional: JOptional[T] =
      if option.isDefined then ju.Optional.of(option.get) else ju.Optional.empty()
  }
  extension [T](opt: Opt[T] | NOpt[T] | OptArg[T]) {
    /**
     * Note that in scala Some(null) is valid value. It will throw an exception in such case, because java Optional
     * is not able to hold null
     */

    inline def toJOptional: JOptional[T] =
      if opt.isDefined then ju.Optional.of {
        opt match
          case Opt(value) => value
          case OptArg(value) => value
          case NOpt(value) => value
      } else ju.Optional.empty()
  }

  extension (option: Option[Double]) {
    inline def toJOptionalDouble: JOptionalDouble =
      if option.isDefined then ju.OptionalDouble.of(option.get) else ju.OptionalDouble.empty()

    inline def asJavaDouble: JOptionalDouble = toJOptionalDouble
  }

  extension (option: Option[Int]) {
    inline def toJOptionalInt: JOptionalInt =
      if option.isDefined then ju.OptionalInt.of(option.get) else ju.OptionalInt.empty()

    inline def asJavaInt: JOptionalInt = toJOptionalInt
  }

  extension (option: Option[Long]) {
    inline def toJOptionalLong: JOptionalLong =
      if option.isDefined then ju.OptionalLong.of(option.get) else ju.OptionalLong.empty()

    inline def asJavaLong: JOptionalLong = toJOptionalLong
  }
}

object JOptionalUtils extends JOptionalUtils
