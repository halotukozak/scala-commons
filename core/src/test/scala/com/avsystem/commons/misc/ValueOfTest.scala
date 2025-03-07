package com.avsystem.commons
package misc

import org.scalatest.funsuite.AnyFunSuite
import ValueOf.mkValueOf

object Obj {
  val x: String = "fuu"

  class Inner {
    val x: String = "fuu"

    def valueOfX: x.type = ValueOf[x.type]

    def valueOfThis: this.type = ValueOf[this.type]
  }

  case object CaseObject

  sealed trait Sealed
  case object SealedObj extends Sealed
}

final class ValueOfTest extends AnyFunSuite:

  test("object") {
    assert(ValueOf[Obj.type] == Obj)
  }

  test("static val") {
    assert(ValueOf[Obj.x.type] == Obj.x)
  }

  test("inner val of local") {
    val i = new Obj.Inner
    assert(ValueOf[i.x.type] == i.x)
    assert(i.valueOfX == i.x)
  }

  test("this") {
    val i = new Obj.Inner
    assert(i.valueOfThis == i)
  }

  test("case object") {
    assert(ValueOf[Obj.CaseObject.type] == Obj.CaseObject)
  }

  test("sealed object") {
    assert(ValueOf[Obj.SealedObj.type] == Obj.SealedObj)
  }

  test("wrong") {
    assertDoesNotCompile("ValueOf[Obj.Sealed]")
  }
