package com.avsystem.commons
package serialization

import org.scalatest.funsuite.AnyFunSuite

case class CodeSizeTester00(
  int: Int,
  string: String,
  double: Double,
  map: Map[String, List[Boolean]],
  people: Set[Person]
)
object CodeSizeTester00 {
  given codec: GenCodec[CodeSizeTester00] = GenCodec.materialize[CodeSizeTester00]
}

case class CodeSizeTester01(
  int: Int,
  string: String,
  double: Double,
  map: Map[String, List[Boolean]],
  people: Set[Person]
)
object CodeSizeTester01 {
  given codec: GenCodec[CodeSizeTester01] = GenCodec.materialize
}

case class CodeSizeTester02(
  int: Int,
  string: String,
  double: Double,
  map: Map[String, List[Boolean]],
  people: Set[Person]
)
object CodeSizeTester02 {
  given codec: GenCodec[CodeSizeTester02] = GenCodec.materialize
}

case class CodeSizeTester03(
  int: Int,
  string: String,
  double: Double,
  map: Map[String, List[Boolean]],
  people: Set[Person]
)
object CodeSizeTester03 {
  given codec: GenCodec[CodeSizeTester03] = GenCodec.materialize
}

case class CodeSizeTester04(
  int: Int,
  string: String,
  double: Double,
  map: Map[String, List[Boolean]],
  people: Set[Person]
)
object CodeSizeTester04 {
  given codec: GenCodec[CodeSizeTester04] = GenCodec.materialize
}

case class CodeSizeTester05(
  int: Int,
  string: String,
  double: Double,
  map: Map[String, List[Boolean]],
  people: Set[Person]
)
object CodeSizeTester05 {
  given codec: GenCodec[CodeSizeTester05] = GenCodec.materialize
}

case class CodeSizeTester06(
  int: Int,
  string: String,
  double: Double,
  map: Map[String, List[Boolean]],
  people: Set[Person]
)
object CodeSizeTester06 {
  given codec: GenCodec[CodeSizeTester06] = GenCodec.materialize
}

case class CodeSizeTester07(
  int: Int,
  string: String,
  double: Double,
  map: Map[String, List[Boolean]],
  people: Set[Person]
)
object CodeSizeTester07 {
  given codec: GenCodec[CodeSizeTester07] = GenCodec.materialize
}

case class CodeSizeTester08(
  int: Int,
  string: String,
  double: Double,
  map: Map[String, List[Boolean]],
  people: Set[Person]
)
object CodeSizeTester08 {
  given codec: GenCodec[CodeSizeTester08] = GenCodec.materialize
}

case class CodeSizeTester09(
  int: Int,
  string: String,
  double: Double,
  map: Map[String, List[Boolean]],
  people: Set[Person]
)
object CodeSizeTester09 {
  given codec: GenCodec[CodeSizeTester09] = GenCodec.materialize
}

case class Person(name: String, birthYear: Int, planet: String = "Earth")
object Person {
  given codec: GenCodec[Person] = GenCodec.materialize
}

class CodeSizeTester extends AnyFunSuite {
  ignore("fake test to see how much JS is generated") {
    println(CodeSizeTester00.codec.write(null, null))
//    println(CodeSizeTester01.codec.write(null, null))

    println(CodeSizeTester00.codec.read(null))
//    println(CodeSizeTester01.codec.read(null))
  }
}
