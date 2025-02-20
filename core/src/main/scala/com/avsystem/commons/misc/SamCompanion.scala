package com.avsystem.commons
package misc

import misc.SamCompanion.ValidSam
import misc.macros.createSamImpl

abstract class SamCompanion[T, F](using ValidSam[T, F]):
  inline def apply(inline fun: F): T = ${ createSamImpl[T, F]('{ fun }) }

object SamCompanion {
  sealed trait ValidSam[T, F]

  object ValidSam:

    private final val instance = new ValidSam[Any, Any] {}

    inline given isValidSam[T, F]: ValidSam[T, F] = {
      Sam.validateSam[T, F]
      instance.asInstanceOf[ValidSam[T, F]]
    }
}
