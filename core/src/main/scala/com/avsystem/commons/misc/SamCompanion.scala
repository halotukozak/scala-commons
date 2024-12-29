package com.avsystem.commons
package misc

import misc.SamCompanion.ValidSam

abstract class SamCompanion[T, F](using ValidSam[T, F]):
  def apply(fun: F): T = Sam[T](fun)

object SamCompanion {
  sealed trait ValidSam[T, F]

  object ValidSam:

    private final val instance = new ValidSam[Any, Any] {}

    inline given isValidSam[T, F]: ValidSam[T, F] =
      if Sam.validateSam[T, F] then instance.asInstanceOf[ValidSam[T, F]]
      else compiletime.error("Function type does not match the SAM type")
}