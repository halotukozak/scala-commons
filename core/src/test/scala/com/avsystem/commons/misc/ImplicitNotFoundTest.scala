package com.avsystem.commons
package misc

import testutil.CompilationErrorAssertions

import org.scalatest.funsuite.AnyFunSuite

import scala.annotation.implicitNotFound

sealed trait Stuff

object Stuff {
  @implicitNotFound("no stuff available")
  given inf: ImplicitNotFound[Stuff] = ImplicitNotFound()
}

sealed trait OtherStuff

object OtherStuff {
  @implicitNotFound("no other stuff available because: #{forStuff}")
  implicit def inf(using ImplicitNotFound[Stuff]): ImplicitNotFound[OtherStuff] = ImplicitNotFound()
}

class ImplicitNotFoundTest extends AnyFunSuite with CompilationErrorAssertions:
  test("simple") {
    assert(typeErrorFor("Implicits.infer[Stuff]") == "no stuff available")
  }

  test("with dependencies") {
    assert(typeErrorFor("Implicits.infer[OtherStuff]") == "no other stuff available because: no stuff available")
  }
