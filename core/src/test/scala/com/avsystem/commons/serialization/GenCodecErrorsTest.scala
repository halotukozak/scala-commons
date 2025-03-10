package com.avsystem.commons
package serialization

import serialization.GenCodec.{ReadFailure, WriteFailure}

import org.scalatest.funsuite.AnyFunSuite

import scala.collection.immutable.ListMap

case class Inner(int: Int)
object Inner extends HasGenCodec[Inner]

class Unwritable
object Unwritable {
  given codec: GenCodec[Unwritable] =
    GenCodec.create(_ => throw new ReadFailure("cannot"), (_, _) => throw new WriteFailure("cannot"))
}

sealed trait Base
case class Outer(inner: Inner) extends Base
case class Other(unwritable: Unwritable) extends Base
object Base extends HasGenCodec[Base]

class GenCodecErrorsTest extends AnyFunSuite {
  def causeChain(t: Throwable): List[Throwable] =
    if t == null then Nil
    else t :: causeChain(t.getCause)

  test("deep reading failure test") {
    val failure = intercept[ReadFailure] {
      SimpleValueInput.read[Base](ListMap("Outer" -> ListMap("inner" -> ListMap("int" -> "NOT INT"))))
    }
    assert(causeChain(failure).size == 4)
  }

  test("deep writing failure test") {
    val failure = intercept[WriteFailure] {
      SimpleValueOutput.write[Base](Other(new Unwritable))
    }
    assert(causeChain(failure).size == 3)
  }
}
