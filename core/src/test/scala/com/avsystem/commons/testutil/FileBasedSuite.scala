package com.avsystem.commons
package testutil

import org.scalactic.source.Position
import org.scalatest.funsuite.AnyFunSuite

abstract class FileBasedSuite(testdir: String) extends AnyFunSuite:
  def updateTestFiles: Boolean =
    System.getProperty("updateTestFiles").opt.map(_.toBoolean).contains(true)

  def separator: String = "-----\n"

  def testFile(file: String)(process: String => String)(using Position): Unit = {
    val path = s"$testdir/$file"
    val contents = IO.readTestResource(path)
    val (input, expectedOutput) = contents.indexOf(separator) match
      case -1 => (contents, "")
      case idx => (contents.take(idx), contents.drop(idx + separator.length))

    val output = process(input)
    if updateTestFiles && output != expectedOutput then IO.writeTestResource(path, input + separator + output)

    assert(output == expectedOutput)
  }

  def assertContents(actual: String, expectedFile: String): Unit = {
    val filePath = s"$testdir/$expectedFile"
    val expected = IO.readTestResource(filePath)
    if updateTestFiles then IO.writeTestResource(filePath, actual)

    assert(actual == expected)
  }
