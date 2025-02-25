package com.avsystem.commons
package misc

import com.avsystem.commons.misc.macros.singleValueFor

import scala.quoted.{Expr, Quotes, Type}
import scala.annotation.implicitNotFound

object ValueOf:
  def apply[T](using vof: ValueOf[T]): T = vof.value

  inline given mkValueOf[T]: ValueOf[T] = singleValueFor[T].get
