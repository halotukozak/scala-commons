package com.avsystem.commons
package testutil

import macros.TestMacros

import org.scalactic.source.Position
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, Assertions, Inspectors}

import scala.compiletime.testing
import scala.quoted.{Expr, Quotes}

trait CompilationErrorAssertions extends Assertions {
  inline def typeErrorFor(inline code: String)(using Position): String =
    scala.compiletime.testing.typeCheckErrors(code).head.message
}

