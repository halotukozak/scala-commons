package com.avsystem.commons
package macros

object TreeForTypeTest {
  def testTreeForType(tpeRepr: String): Nothing =
    ??? // macro com.avsystem.commons.macros.TestMacros.testTreeForType

  val x = "x"

  type A
  trait Higher[T[_]]
  type Id[T] = T

  testTreeForType("Int")
  testTreeForType("Integer")
  testTreeForType("A")
  testTreeForType("TreeForTypeTest#B")
  testTreeForType("x.type")
  testTreeForType("None.type")
  testTreeForType("this.type")
  testTreeForType("List[Int]")
  testTreeForType("Set[_]")
  testTreeForType("Map[T, T] forSome {type T <: String}")
  testTreeForType("AnyRef with Serializable")
  testTreeForType("Int => String")
  testTreeForType("(=> Int) => String")
  testTreeForType("Double => Int => String")
  testTreeForType("(Double => Int) => String")
  testTreeForType("Higher[Id]")
  testTreeForType(
    """AnyRef {
    type Lol[K, V] <: Map[K, V]
    def stuff[C[+X] <: Iterable[X]](c: C[Int]): C[String]
    val lulz: Map[String, Int]
  }""")
}

class TreeForTypeTest {

  import TreeForTypeTest._

  type B
  val y = "y"

  class Fuu {
    val z = "z"
    object bar {
      val q = "q"
    }
  }
  val fuu = new Fuu

  testTreeForType("Int")
  testTreeForType("Integer")
  testTreeForType("A")
  testTreeForType("TreeForTypeTest#B")
  testTreeForType("B")
  testTreeForType("x.type")
  testTreeForType("y.type")
  testTreeForType("fuu.bar.q.type")
  testTreeForType("None.type")
  testTreeForType("this.type")
  testTreeForType("List[Int]")
  testTreeForType("Set[_]")
  testTreeForType("Map[T, T] forSome {type T <: String}")
  testTreeForType("fu.z.type forSome {val fu: Fuu}")
  testTreeForType("fu.z.type forSome {val fu: Fuu with Singleton}")
  testTreeForType("fu.bar.q.type forSome {val fu: Fuu}")
  testTreeForType("AnyRef with Serializable")
  testTreeForType("Higher[Id]")
  testTreeForType(
    """AnyRef {
    type Lol[K, V] <: Map[K, V]
    type Stuff[K, V] = Map[K, V]
    def stuff[C[+X] <: Iterable[X]](c: C[Int]): C[String]
    val lulz: Map[String, Int]
  }""")

  class Inner {
    testTreeForType("Int")
    testTreeForType("Integer")
    testTreeForType("A")
    testTreeForType("TreeForTypeTest#B")
    testTreeForType("B")
    testTreeForType("x.type")
    testTreeForType("y.type")
    testTreeForType("fuu.z.type")
    testTreeForType("fuu.bar.q.type")
    testTreeForType("None.type")
    testTreeForType("this.type")
    testTreeForType("Inner.this.type")
    testTreeForType("TreeForTypeTest.this.type")
    testTreeForType("List[Int]")
    testTreeForType("Set[_]")
    testTreeForType("Map[T, T] forSome {type T <: String}")
    testTreeForType("fu.z.type forSome {val fu: Fuu}")
    testTreeForType("fu.bar.q.type forSome {val fu: Fuu}")
    testTreeForType("AnyRef with Serializable")
    testTreeForType("Higher[Id]")
    testTreeForType(
      """AnyRef {
      type Lol[K, V] <: Map[K, V]
      type Stuff[K, V] = Map[K, V]
      def stuff[C[+X] <: Iterable[X]](c: C[Int]): C[String]
      val lulz: Map[String, Int]
    }""")
  }
}

object UnrelatedTreeForType {

  import TreeForTypeTest._

  testTreeForType("Int")
  testTreeForType("Integer")
  testTreeForType("A")
  testTreeForType("TreeForTypeTest#B")
  testTreeForType("x.type")
  testTreeForType("None.type")
  testTreeForType("this.type")
  testTreeForType("List[Int]")
  testTreeForType("Set[_]")
  testTreeForType("Map[T, T] forSome {type T <: String}")
  testTreeForType("AnyRef with Serializable")
  testTreeForType("Higher[Id]")
  testTreeForType(
    """AnyRef {
    type Lol[K, V] <: Map[K, V]
    type Stuff[K, V] = Map[K, V]
    def stuff[C[+X] <: Iterable[X]](c: C[Int]): C[String]
    val lulz: Map[String, Int]
  }""")
}
