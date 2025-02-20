package com.avsystem.commons
package macros

import scala.quoted.*

def typeReprImpl[T: Type](using Quotes): Expr[String] = Expr(Type.show[T])
