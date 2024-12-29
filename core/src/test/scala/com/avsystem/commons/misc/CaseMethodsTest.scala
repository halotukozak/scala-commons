package com.avsystem.commons
package misc

import org.scalatest.funsuite.AnyFunSuite

final case class Whatever(str: String, int: Int) extends AbstractCase

//noinspection TypeAnnotation
final class CaseMethodsTest extends AnyFunSuite:
  val first = Whatever("lol", 42)
  val second = Whatever("lol", 42)
  val other = Whatever("lol", 41)

  test("equality") {
    assert(first canEqual second)
    assert(first == second)
    assert(first.equals(second))
  }

  test("inequality") {
    assert(first != other)
    assert(!first.equals(other))
  }

  test("hashCode") {
    assert(first.hashCode == second.hashCode)
    assert(first.## == second.##)
  }

  test("toString") {
    assert(first.toString == "Whatever(lol,42)")
  }
