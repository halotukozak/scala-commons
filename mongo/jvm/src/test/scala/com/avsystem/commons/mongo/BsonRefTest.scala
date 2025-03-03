package com.avsystem.commons
package mongo

import com.avsystem.commons.serialization.{GenCodec, name, transparent}
import org.scalatest.funsuite.AnyFunSuite

case class InnerClass(map: Map[String, String])
object InnerClass extends BsonRef.Creator[InnerClass] {
  implicit val codec: GenCodec[InnerClass] = GenCodec.materialize

  final val MapRef = ref(_.map)
}

@transparent
case class Wrapper(s: String) extends AnyVal
object Wrapper {
  implicit val codec: GenCodec[Wrapper] = GenCodec.materialize
}

case class TestEntity(`$special.field`: String, wrapper: Wrapper, @name("inner") innerClass: InnerClass)
object TestEntity extends BsonRef.Creator[TestEntity] {
  implicit val codec: GenCodec[TestEntity] = GenCodec.materialize
}

class BsonRefTest extends AnyFunSuite with BsonRef.Creator[TestEntity] {
  test("basic test") {
    assert(ref(_.wrapper).path == "wrapper")
    assert(ref(_.wrapper.s).path == "wrapper")
    assert(ref(_.innerClass).path == "inner")
    assert(ref(_.innerClass.map).path == "inner.map")
    assert(ref(_.innerClass).andThen(InnerClass.MapRef).path == "inner.map")
    assert(ref(_.innerClass.map("key")).path == "inner.map.key")
    assert(ref(_.`$special.field`).path == "\\$special\\_field")
  }
}
