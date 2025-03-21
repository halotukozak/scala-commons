package com.avsystem.commons
package macros

import derivation.{AllowImplicitMacro, DeferredInstance}
import macros.TestMacros.materialize
import misc.TypeString
import misc.macros.{knownSubtypes, printTree}

import org.scalatest.funsuite.AnyFunSuite

object TypeClassDerivationTest {
  inline def typeRepr[T]: String = TypeString.of[T]

  class DefVal(v: => Any) {
    lazy val value: Any = v

    override def equals(other: Any): Boolean = other match
      case otherDef: DefVal => value == otherDef.value
      case _ => false

    override def hashCode() = value.hashCode
  }

  object DefVal {
    def apply(value: => Any) = new DefVal(value)

    def unapply(ddef: DefVal): Option[Any] = Some(ddef.value)
  }

  trait TC[T] {
    def tpe: String

    def matches(pf: PartialFunction[TC[T], Any]): Boolean =
      pf.isDefinedAt(this)
  }

  case class SingletonTC[T](tpe: String, value: T) extends TC[T]
  case class ApplyUnapplyTC[T](tpe: String, subs: List[(name: String, tc: TC[?], defaultValue: Option[DefVal])])
    extends TC[T]

  case class SealedHierarchyTC[T](tpe: String, subs: List[(String, TC[? <: T])]) extends TC[T]

  case class UnknownTC[T](tpe: String) extends TC[T]

  case class ForList[T](elementTc: TC[T]) extends TC[List[T]] {
    def tpe: String = s"List[${elementTc.tpe}]"
  }

  object TC extends ImplicitMaterializers {
    case class Auto[T](tc: TC[T]) extends AnyVal

    final class Deferred[T] extends DeferredInstance[TC[T]] with TC[T] {
      def tpe: String = underlying.tpe

      override def hashCode(): Int = System.identityHashCode(underlying)

      override def equals(obj: Any): Boolean = obj match
        case df: Deferred[?] => underlying eq df.underlying
        case _ => false
    }

    object Deferred {
      def apply[T](underlying: TC[T]): Deferred[T] = {
        val res = new Deferred[T]
        res.underlying = underlying
        res
      }
    }

    given forInt: TC[Int] = UnknownTC(typeRepr[Int])
    given forString: TC[String] = UnknownTC(typeRepr[String])
    given forList[T](using tct: TC[T]): TC[List[T]] = ForList(tct)
  }

  trait ImplicitMaterializers { this: TC.type =>
    given materializeImplicitly[T](using AllowImplicitMacro[TC[T]]): TC[T] = materialize
  }
}

final class TypeClassDerivationTest extends AnyFunSuite {

  import TypeClassDerivationTest.*

  test("unknown test") {
    assert(materialize[Int] == UnknownTC(typeRepr[Int]))
  }

  object SomeSingleton {
    given tc: TC[SomeSingleton.type] = materialize
  }

  test("singleton test") {
    assert(SomeSingleton.tc == SingletonTC(typeRepr[SomeSingleton.type], SomeSingleton))
  }

  case class Whatever(str: String, int: Int = 42)

  object Whatever {
    given tc: TC[Whatever] = materialize
  }

  test("case class test") {
    assert(
      Whatever.tc == ApplyUnapplyTC(
        typeRepr[Whatever],
        List(("str", TC.forString, None), ("int", TC.forInt, Some(DefVal(42)))),
      ),
    )
  }

  sealed trait SealedRoot

  case class SealedCase(i: Int) extends SealedRoot

  case object SealedObj extends SealedRoot

  sealed trait SubRoot extends SealedRoot

  case class SubSealedCase(i: Int, w: Whatever) extends SubRoot

  object SealedRoot {
    given tc: TC[SealedRoot] = materialize
  }

  test("sealed hierarchy test") {
    assert(
      SealedRoot.tc.asInstanceOf[SealedHierarchyTC[SealedRoot]].subs.size == SealedHierarchyTC(
        typeRepr[SealedRoot],
        List(
          ("SealedCase", ApplyUnapplyTC(typeRepr[SealedCase], List(("i", TC.forInt, None)))),
          ("SealedObj", SingletonTC(typeRepr[SealedObj.type], SealedObj)),
          (
            "SubSealedCase",
            ApplyUnapplyTC(typeRepr[SubSealedCase], List(("i", TC.forInt, None), ("w", Whatever.tc, None))),
          ),
        ),
      ).subs.size,
    )
    assert(
      SealedRoot.tc == SealedHierarchyTC(
        typeRepr[SealedRoot],
        List(
          ("SealedCase", ApplyUnapplyTC(typeRepr[SealedCase], List(("i", TC.forInt, None)))),
          ("SealedObj", SingletonTC(typeRepr[SealedObj.type], SealedObj)),
          (
            "SubSealedCase",
            ApplyUnapplyTC(typeRepr[SubSealedCase], List(("i", TC.forInt, None), ("w", Whatever.tc, None))),
          ),
        ),
      ),
    )
  }

  case class Recursive(str: String, next: Recursive)

  object Recursive {
    given tc: TC[Recursive] = materialize
  }

  test("recursive case class test") {
    assert(
      Recursive.tc == ApplyUnapplyTC(
        typeRepr[Recursive],
        List(("str", TC.forString, None), ("next", TC.Deferred(Recursive.tc), None)),
      ),
    )
  }

  case class IndiRec(children: List[IndiRec])

  object IndiRec {
    given tc: TC[IndiRec] = materialize
  }

  test("indirectly recursive case class test") {
    assert(IndiRec.tc == ApplyUnapplyTC(typeRepr[IndiRec], List(("children", ForList(TC.Deferred(IndiRec.tc)), None))))
  }

//  sealed trait Tree[T]
//
//  case class Leaf[T](value: T) extends Tree[T]
//
//  case class Branch[T](left: Tree[T], right: Tree[T]) extends Tree[T]
//
//  object Tree {
//    given tc[A: TC]: TC[Tree[A]] = materialize
//  }
//
//  test("recursive GADT test") {
//    def doTest[A](using tct: TC[A]): Unit = {
//      val tc = Tree.tc[A]
//      assert(
//        tc == SealedHierarchyTC(
//          typeRepr[Tree[A]],
//          List(
//            ("Leaf", ApplyUnapplyTC(typeRepr[Leaf[A]], List(("value", tct, None)))),
//            (
//              "Branch",
//              ApplyUnapplyTC(
//                typeRepr[Branch[A]],
//                List(("left", TC.Deferred(tc), None), ("right", TC.Deferred(tc), None)),
//              ),
//            ),
//          ),
//        ),
//      )
//    }
//
//    doTest[String]
//  }
}
