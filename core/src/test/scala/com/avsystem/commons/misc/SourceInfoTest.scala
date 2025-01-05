package com.avsystem.commons
package misc

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

final class SourceInfoTest extends AnyFunSuite with Matchers {
  val srcInfo: SourceInfo = SourceInfo.here

  test("simple") {
    srcInfo should matchPattern {
      case SourceInfo(_, "SourceInfoTest.scala", 223, 8, 29,
      "  val srcInfo: SourceInfo = SourceInfo.here",
      List("srcInfo", "SourceInfoTest", "misc", "commons", "avsystem", "com")) =>
    }
  }
}
