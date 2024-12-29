//package com.avsystem.commons
//package macros
//
//import macros.misc.TypeString
//
//import org.scalactic.source.Position
//import org.scalatest.funsuite.AnyFunSuite
//
//object TypeStringTest {
//  val x = "x"
//
//  type A
//
//  type StaticAlias = Int
//
//  class OFuu {
//    val z = "z"
//
//    object bar:
//      val q = "q"
//  }
//
//  val fuu = new OFuu
//
//  def defineTests[T: TypeString](suite: TypeStringTest): Unit = {
//    type LocalAlias = Int
//
//    import suite.testTypeString
//    testTypeString[Int]("Int")
//    testTypeString[LocalAlias]("Int")
//    testTypeString[StaticAlias]("TypeStringTest.StaticAlias")
//    testTypeString[Integer]("Integer")
//    testTypeString[String => Int]("String => Int")
//    testTypeString[(=> String) => Int]("(=> String) => Int")
//    testTypeString[T => Int](s"(${TypeString.of[T]}) => Int")
//    testTypeString[String => Int => Double]("String => Int => Double")
//    testTypeString[(String => Int) => Double]("(String => Int) => Double")
//    testTypeString[() => Int]("() => Int")
//    testTypeString[(String, Int)]("(String, Int)")
//    testTypeString[A]("TypeStringTest.A")
//    testTypeString[T](TypeString.of[T])
//    testTypeString[List[T]](s"List[${TypeString.of[T]}]")
//    testTypeString[TypeStringTest#B]("TypeStringTest#B")
//    testTypeString[x.type]("TypeStringTest.x.type")
//    testTypeString[fuu.bar.q.type]("TypeStringTest.fuu.bar.q.type")
//    testTypeString[None.type]("None.type")
//    testTypeString[this.type]("TypeStringTest.type")
//    testTypeString[Set.type]("Set.type")
//    testTypeString[List[Int]]("List[Int]")
//    testTypeString[Set[?]]("Set[?]")
//    testTypeString[Set[? <: String]]("Set[? <: String]")
//    testTypeString[AnyRef & Serializable]("AnyRef & Serializable")
//    testTypeString[AnyRef & Serializable {type A <: String}]("AnyRef & Serializable {type A <: String}")
//    testTypeString[Serializable {type A <: String}]("Serializable {type A <: String}")
//    testTypeString[ {type A = String}]("{type A = String}")
//    testTypeString[ {type A}]("{type A}")
//    testTypeString[({type A})#A]("({type A})#A")
//    testTypeString[({type *})# *]("({type *})# *")
//    testTypeString[ {type A >: Null <: String}]("{type A >: Null <: String}")
//    testTypeString[ {type A[+X] = Int}]("{type A[+X] = Int}")
//    testTypeString[ {type A[X] = List[X]}]("{type A[X] = List[X]}")
//    //    testTypeString[ {type A[+X <: Set[X]] = List[X]}]("{type A[+X <: Set[X]] = List[X]}")
//    testTypeString[ {type A[F[+X] <: List[X]] = F[Int]}]("{type A[F[+X] <: List[X]] = F[Int]}")
//    //    testTypeString[ {type A[F[X <: List[X]]] = F[Nothing]}]("{type A[F[X <: List[X]]] = F[Nothing]}")
//    testTypeString[ {val lol: Int}]("{val lol: Int}")
//    testTypeString[ {var lol: Int}]("{var lol: Int}")
//    testTypeString[ {def lol: Int}]("{def lol: Int}")
//    testTypeString[ {def lol(): Int}]("{def lol(): Int}")
//    //    testTypeString[ {def lol[A]: A}]("{def lol[A]: A}")
//    //    testTypeString[ {def lol[A](x: Int): A}]("{def lol[A](x: Int): A}")
//    testTypeString[ {def lol(x: Int): String}]("{def lol(x: Int): String}")
//    testTypeString[ {def lol(x: => Int): String}]("{def lol(x: => Int): String}")
//    testTypeString[ {def lol(xs: Int*): String}]("{def lol(xs: Int*): String}")
//    testTypeString[ {def lol(xs: Int => String*): String}]("{def lol(xs: Int => String*): String}")
//    //    testTypeString[ {def lol[**](xs: ** *): String}]("{def lol[**](xs: ** *): String}")
//    testTypeString[ {def lol(using x: Int, y: String): String}](
//      "{def lol(using x: Int, y: String): String}")
//    testTypeString[ {def lol(x: String): x.type}]("{def lol(x: String): x.type}")
//    testTypeString[ {type T; def lol: T}]("{type T; def lol: T}")
//  }
//}
//
//class TypeStringTest extends AnyFunSuite {
//
//  def testTypeString[T: TypeString](expected: String)(using pos: Position): Unit =
//    test(s"${pos.lineNumber}:$expected") {
//      assert(TypeString.of[T].replace("com.avsystem.commons.macros.", "") == expected)
//    }
//
//  TypeStringTest.defineTests[Double](this)
//
//  type B
//  val y = "y"
//
//  class Fuu {
//    val z = "z"
//
//    object bar:
//      val q = "q"
//  }
//
//  val fuu = new Fuu
//
//  import TypeStringTest.{A, x}
//
//  testTypeString[Int]("Int")
//  testTypeString[Integer]("Integer")
//  testTypeString[TypeStringTest#B]("TypeStringTest#B")
//  testTypeString[A]("TypeStringTest.A")
//  //  testTypeString[B]("B")
//  testTypeString[x.type]("TypeStringTest.x.type")
//  //  testTypeString[y.type]("y.type")
//  //  testTypeString[fuu.bar.q.type]("fuu.bar.q.type")
//  testTypeString[None.type]("None.type")
//  //  testTypeString[this.type]("this.type")
//  testTypeString[List[Int]]("List[Int]")
//  testTypeString[Set[?]]("Set[?]")
//  testTypeString[Set[? <: String]]("Set[? <: String]")
//  //  testTypeString[AnyRef with Serializable]("AnyRef with Serializable")
//
//  UnrelatedTypeString.defineTests[String](this)
//}
//
//object UnrelatedTypeString {
//
//  import TypeStringTest.*
//
//  def defineTests[T: TypeString](suite: TypeStringTest): Unit = {
//    import suite.testTypeString
//    testTypeString[Int]("Int")
//    testTypeString[StaticAlias]("TypeStringTest.StaticAlias")
//    testTypeString[Integer]("Integer")
//    testTypeString[String => Int]("String => Int")
//    testTypeString[String => Int => Double]("String => Int => Double")
//    testTypeString[(String => Int) => Double]("(String => Int) => Double")
//    testTypeString[() => Int]("() => Int")
//    testTypeString[(String, Int)]("(String, Int)")
//    testTypeString[A]("TypeStringTest.A")
//    testTypeString[T](TypeString.of[T])
//    testTypeString[List[T]](s"List[${TypeString.of[T]}]")
//    testTypeString[TypeStringTest#B]("TypeStringTest#B")
//    testTypeString[x.type]("TypeStringTest.x.type")
//    testTypeString[fuu.bar.q.type]("TypeStringTest.fuu.bar.q.type")
//    testTypeString[None.type]("None.type")
//    testTypeString[this.type]("UnrelatedTypeString.type")
//    testTypeString[Set.type]("Set.type")
//    testTypeString[List[Int]]("List[Int]")
//    testTypeString[Set[?]]("Set[?]")
//    testTypeString[Set[? <: String]]("Set[? <: String]")
//    testTypeString[AnyRef & Serializable]("AnyRef & Serializable")
//    testTypeString[AnyRef & Serializable {type A <: String}]("AnyRef & Serializable {type A <: String}")
//  }
//
