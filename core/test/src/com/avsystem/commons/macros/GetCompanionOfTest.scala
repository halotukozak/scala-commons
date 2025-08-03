package com.avsystem.commons.macros

import org.scalatest.funsuite.AnyFunSuite

class GetCompanionOfTest extends AnyFunSuite {

  test("getCompanionOf should retrieve case class companion") {
    assert(getCompanionOf[TestClass] == TestClass)
  }

  test("getCompanionOf should retrieve trait companion") {
    assert(getCompanionOf[TestTrait] == TestTrait)
  }

  test("getCompanionOf should retrieve regular class companion") {
    assert(getCompanionOf[SimpleClass] == SimpleClass)
  }

  test("getCompanionOf should retrieve companion of local class") {
    class LocalClass
    object LocalClass
    assert(getCompanionOf[LocalClass] == LocalClass)
  }

  test("getCompanionOf should retrieve companion of generic class") {
    assert(getCompanionOf[GenericClass[String]] == GenericClass)
  }

  test("getCompanionOf should retrieve companion of generic case class") {
    assert(getCompanionOf[GenericCaseClass[String]] == GenericCaseClass)
  }

  test("getCompanionOf should retrieve companion of generic trait") {
    assert(getCompanionOf[GenericTrait[String]] == GenericTrait)
  }

  test("getCompanionOf should fail for types without companion") {
    assertDoesNotCompile("""
        |class Without
        |
        |getCompanionOf[Without
        |""".stripMargin)
  }

}

case class TestClass(value: String)

trait TestTrait

object TestTrait

class SimpleClass

object SimpleClass

class GenericClass[T](value: T)

object GenericClass

case class GenericCaseClass[T](value: T)

trait GenericTrait[T]
object GenericTrait
