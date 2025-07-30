package com.avsystem.commons
package spring

import com.typesafe.config._

trait HoconType[T] {

  protected def requireNonNull(value: ConfigValue): ConfigValue = {
    require(value != null, s"No value found")
    value
  }

  protected def requireType(requiredType: ConfigValueType, value: ConfigValue): Unit = {
    requireNonNull(value)
    require(
      value.valueType == requiredType,
      s"Value at ${value.origin.description} has type, ${value.valueType}, required $requiredType"
    )
  }

  def get(value: ConfigValue): T
}

object HoconType {

  import com.typesafe.config.ConfigValueType._

  given anyHoconType: HoconType[Any] = new HoconType[Any] {
    def get(value: ConfigValue) =
      requireNonNull(value).unwrapped
  }

  given anyRefHoconType: HoconType[AnyRef] = new HoconType[AnyRef] {
    def get(value: ConfigValue) =
      requireNonNull(value).unwrapped
  }

  given nullHoconType: HoconType[Null] = new HoconType[Null] {
    def get(value: ConfigValue) = {
      requireType(NULL, value)
      null
    }
  }

  given stringHoconType: HoconType[String] = new HoconType[String] {
    def get(value: ConfigValue) = {
      requireType(STRING, value)
      value.unwrapped.asInstanceOf[String]
    }
  }

  given booleanHoconType: HoconType[Boolean] = new HoconType[Boolean] {
    def get(value: ConfigValue) = {
      requireType(BOOLEAN, value)
      value.unwrapped.asInstanceOf[Boolean]
    }
  }

  given numberHoconType: HoconType[JNumber] = new HoconType[JNumber] {
    def get(value: ConfigValue) = {
      requireType(NUMBER, value)
      value.unwrapped.asInstanceOf[JNumber]
    }
  }

  given intHoconType: HoconType[Int] = new HoconType[Int] {
    def get(value: ConfigValue) = {
      requireType(NUMBER, value)
      value.unwrapped.asInstanceOf[JNumber].intValue
    }
  }

  given longHoconType: HoconType[Long] = new HoconType[Long] {
    def get(value: ConfigValue) = {
      requireType(NUMBER, value)
      value.unwrapped.asInstanceOf[JNumber].longValue
    }
  }

  given configHoconType: HoconType[Config] = new HoconType[Config] {
    def get(value: ConfigValue) = {
      requireType(OBJECT, value)
      value.asInstanceOf[ConfigObject].toConfig
    }
  }

  given configValueHoconType: HoconType[ConfigValue] = new HoconType[ConfigValue] {
    def get(value: ConfigValue) = value
  }

  given configObjectHoconType: HoconType[ConfigObject] = new HoconType[ConfigObject] {
    def get(value: ConfigValue) = {
      requireType(OBJECT, value)
      value.asInstanceOf[ConfigObject]
    }
  }

  given configListHoconType: HoconType[ConfigList] = new HoconType[ConfigList] {
    def get(value: ConfigValue) = {
      requireType(LIST, value)
      value.asInstanceOf[ConfigList]
    }
  }

  given listHoconType[T: HoconType]: HoconType[JList[T]] = new HoconType[JList[T]] {
    def get(value: ConfigValue) = {
      requireType(LIST, value)
      val elementHoconType = implicitly[HoconType[T]]
      value.asInstanceOf[ConfigList].asScala.map(elementHoconType.get).asJava
    }
  }

  given mapHoconType[T: HoconType]: HoconType[JMap[String, T]] = new HoconType[JMap[String, T]] {
    def get(value: ConfigValue) = {
      requireType(OBJECT, value)
      val elementHoconType = implicitly[HoconType[T]]
      value
        .asInstanceOf[ConfigObject]
        .asScala
        .map { case (k, v) =>
          (k, elementHoconType.get(v))
        }
        .asJava
    }
  }

  given optionHoconType[T: HoconType]: HoconType[Option[T]] = new HoconType[Option[T]] {
    def get(value: ConfigValue): Option[T] =
      if (value == null || value.valueType == NULL) None
      else Some(implicitly[HoconType[T]].get(value))
  }

  given eitherHoconType[A: HoconType, B: HoconType]: HoconType[Either[A, B]] = new HoconType[Either[A, B]] {
    def get(value: ConfigValue): Either[A, B] = {
      val leftTry = Try(implicitly[HoconType[A]].get(value))
      val rightTry = Try(implicitly[HoconType[B]].get(value))

      (leftTry, rightTry) match {
        case (Failure(left), Failure(right)) =>
          throw new IllegalArgumentException(
            "Could not parse config value as one of two types:\n" +
              left.getMessage + "\n" + right.getMessage
          )
        case (Success(left), _) => Left(left)
        case (_, Success(right)) => Right(right)
      }
    }
  }

}
