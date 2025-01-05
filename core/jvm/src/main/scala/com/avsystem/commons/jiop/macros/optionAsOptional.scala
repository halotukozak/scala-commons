package com.avsystem.commons
package jiop
package macros

import jiop.AsJava
import jiop.macros.specializedMacro

inline def specializedOptionAsJava[T]: AsJava[Option[T], ReturnOfOptionalAsJava[T]] = ${
  specializedMacro[T, [U] =>> AsJava[Option[U], ReturnOfOptionalAsJava[U]]](
    '{ specializedOptionAsJavaInt },
    '{ specializedOptionAsJavaLong },
    '{ specializedOptionAsJavaDouble },
    '{ specializedOptionAsJavaAny[T] },
  )
}

private val specializedOptionAsJavaInt: AsJava[Option[Int], JOptionalInt] = _.toJOptionalInt
private val specializedOptionAsJavaLong: AsJava[Option[Long], JOptionalLong] = _.toJOptionalLong
private val specializedOptionAsJavaDouble: AsJava[Option[Double], JOptionalDouble] = _.toJOptionalDouble
private def specializedOptionAsJavaAny[T]: AsJava[Option[T], ReturnOfOptionalAsJava[T]] = _.toJOptional.asInstanceOf[ReturnOfOptionalAsJava[T]]

