package com.avsystem.commons
package analyzer

import org.scalatest.funsuite.AnyFunSuite

class ImportJavaUtilTest extends AnyFunSuite with AnalyzerTest {
  test("import java.util should be rejected") {
    assertErrors("""
        |import java.util
        |
        |object whatever
      """.stripMargin)
  }
}
