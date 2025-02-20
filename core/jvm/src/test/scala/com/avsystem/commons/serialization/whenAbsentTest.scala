package com.avsystem.commons
package serialization

import org.scalatest.funsuite.AnyFunSuite

final class whenAbsentTest extends AnyFunSuite:
  @whenAbsent("default")
  val str: String = whenAbsent.value

  final case class HasDefault(@whenAbsent("default1") str: String = whenAbsent.value)

  final class HasDefault2(@whenAbsent("default2") val str: String = whenAbsent.value)

  @whenAbsent("default3")
  def method1: String = whenAbsent.value

  def method2(@whenAbsent("default4") param: String = whenAbsent.value): String = param

  test("val") {
    assert(str == "default")
  }

  test("case class") {
    assert(HasDefault().str == "default1")
  }

  test("class with val") {
    assert(HasDefault2().str == "default2")
  }

  test("method") {
    assert(method1 == "default3")
    assert(method2() == "default4")
  }
