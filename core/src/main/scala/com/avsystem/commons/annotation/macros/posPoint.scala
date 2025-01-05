package com.avsystem.commons
package annotation.macros

import scala.quoted.{Expr, Quotes}

def posPointImpl(using quotes: Quotes): Expr[Int] = Expr(quotes.reflect.Position.ofMacroExpansion.start)
