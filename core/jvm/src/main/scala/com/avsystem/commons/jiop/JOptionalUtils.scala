package com.avsystem.commons
package jiop

import com.avsystem.commons.meta.OptionLike

import java.util as ju

trait JOptionalUtils {

  type JOptional[T] = ju.Optional[T]
  type JOptionalDouble = ju.OptionalDouble
  type JOptionalInt = ju.OptionalInt
  type JOptionalLong = ju.OptionalLong

  object JOptional {
    inline def apply[T](inline nullable: T): JOptional[T] = ju.Optional.ofNullable(nullable)
    inline def empty[T]: JOptional[T] = ju.Optional.empty[T]()
  }

  object JOptionalDouble {
    inline def apply(inline value: Double): JOptionalDouble = ju.OptionalDouble.of(value)

    inline def empty: JOptionalDouble = ju.OptionalDouble.empty()
  }

  object JOptionalInt {
    inline def apply(inline value: Int): JOptionalInt = ju.OptionalInt.of(value)

    inline def empty: JOptionalInt = ju.OptionalInt.empty()
  }

  object JOptionalLong {
    inline def apply(inline value: Long): JOptionalLong = ju.OptionalLong.of(value)

    inline def empty: JOptionalLong = ju.OptionalLong.empty()
  }

  extension [T](optional: JOptional[T]) {
    inline def toOption: Option[T] =
      if (optional.isPresent) Some(optional.get) else None

    inline def toOpt: Opt[T] =
      if (optional.isPresent) Opt(optional.get) else Opt.Empty

    inline def asScala: Option[T] = toOption
  }

  extension (optional: JOptionalDouble) {
    inline def toOption: Option[Double] =
      if (optional.isPresent) Some(optional.getAsDouble) else None

    inline def toOpt: Opt[Double] =
      if (optional.isPresent) Opt(optional.getAsDouble) else Opt.Empty

    inline def asScala: Option[Double] = toOption
  }

  extension (optional: JOptionalInt) {
    inline def toOption: Option[Int] =
      if (optional.isPresent) Some(optional.getAsInt) else None

    inline def toOpt: Opt[Int] =
      if (optional.isPresent) Opt(optional.getAsInt) else Opt.Empty

    inline def asScala: Option[Int] = toOption
  }

  extension (optional: JOptionalLong) {
    inline def toOption: Option[Long] =
      if (optional.isPresent) Some(optional.getAsLong) else None

    inline def toOpt: Opt[Long] =
      if (optional.isPresent) Opt(optional.getAsLong) else Opt.Empty

    inline def asScala: Option[Long] = toOption
  }

  extension [T](option: Option[T]) {
    /**
      * Note that in scala Some(null) is valid value. It will throw an exception in such case, because java Optional
      * is not able to hold null
      */
    inline def toJOptional: JOptional[T] =
      if (option.isDefined) ju.Optional.of(option.get) else ju.Optional.empty()

    inline def asJava: JOptional[T] = toJOptional
  }

  extension [T, O](opt: O)(using optionLike: OptionLike.Aux[O, T]) {
    /**
      * Note that in scala Some(null) is valid value. It will throw an exception in such case, because java Optional
      * is not able to hold null
      */
    inline def toJOptional: JOptional[T] =
      if optionLike.isDefined(opt) then
        ju.Optional.of(optionLike.get(opt)) else ju.Optional.empty()

    inline def asJava: JOptional[T] = toJOptional
  }


  extension (option: Option[Double]) {
    inline def toJOptionalDouble: JOptionalDouble =
      if (option.isDefined) ju.OptionalDouble.of(option.get) else ju.OptionalDouble.empty()

    inline def asJavaDouble: JOptionalDouble = toJOptionalDouble
  }

  extension (option: Option[Int]) {
    inline def toJOptionalInt: JOptionalInt =
      if (option.isDefined) ju.OptionalInt.of(option.get) else ju.OptionalInt.empty()

    inline def asJavaInt: JOptionalInt = toJOptionalInt
  }

  extension (option: Option[Long]) {
    inline def toJOptionalLong: JOptionalLong =
      if (option.isDefined) ju.OptionalLong.of(option.get) else ju.OptionalLong.empty()

    inline def asJavaLong: JOptionalLong = toJOptionalLong
  }

}
