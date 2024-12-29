package com.avsystem.commons
package misc

import misc.macros.misc.SamMacros


object Sam {

  /** Implements a single abstract method trait/class `T` using passed function or expression as implementation of the
   * sole abstract method. The argument passed may be either a function that must match the signature of the abstract
   * method or - in case the method does not take any arguments - an expression which will be returned in the
   * implementation of abstract method (as if the expression was passed as by-name parameter).
   */
  inline def apply[T](fun: Any): T = ${ SamMacros.createSam[T, fun.type]('{ fun }) }

  inline def validateSam[T, F]: Boolean = ${ SamMacros.validateSam[T, F] }
}
