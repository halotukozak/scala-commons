package com.avsystem.commons
package macros

import macros.TypeClassDerivationTest.{DefVal, TC}

import com.avsystem.commons.macros.TestMacros.nameFor
import com.avsystem.commons.misc.TypeString
import com.avsystem.commons.misc.macros.{ApplyUnapply, TypeClassDerivation, typeStringImpl}

import scala.quoted.{Expr, Quotes, ToExpr, Type}

private[commons] object TestMacros extends TypeClassDerivation[TypeClassDerivationTest.TC] {

  override def typeClassInstance[T: Type](using Quotes): Type[TC[T]] =
    Type.of[TC[T]]
  //    TypeRepr.of[TypeClassDerivationTest.TC[?]].appliedTo(tpe).asType.asInstanceOf[Type[TC[T]]]

  override def implementDeferredInstance[T: Type](using Quotes): Expr[TypeClassDerivationTest.TC.Deferred[T]] =
    '{ new TypeClassDerivationTest.TC.Deferred[T] }

  override def forSingleton[T: Type](singleValue: Expr[ValueOf[T]])(using
    Quotes,
  ): Expr[TypeClassDerivationTest.SingletonTC[T]] =
    '{ TypeClassDerivationTest.SingletonTC[T](nameFor[T], $singleValue.value) }

  override def forApplyUnapply[T: Type](
    au: ApplyUnapply[T],
  )(using Quotes): Expr[TypeClassDerivationTest.ApplyUnapplyTC[T]] = {
    val deps: Expr[List[(String, TC[?], Option[DefVal])]] = Expr.ofList {
      au.params.map { case ApplyUnapply.Param(label, index, tpe, repeated, dv) =>
        val name = Expr(label)
        val tc = tpe match
          case '[t] => materializeImpl[t]
        val defaultValueOpt = dv match
          case Some(value) => '{ Some(DefVal($value())) }
          case None => '{ None }
        '{ ($name, $tc, $defaultValueOpt) }
      }
    }
    val name = nameForExpr(using au.ownerTpe)
    '{ TypeClassDerivationTest.ApplyUnapplyTC[T]($name, $deps) }
  }

  override def forSealedHierarchy[T: Type](
    subtypes: List[KnownSubtype[T]],
  )(using Quotes): Expr[TypeClassDerivationTest.SealedHierarchyTC[T]] = {
    val deps = Expr.ofList {
      subtypes.map { case KnownSubtype(_, st, tree) =>
        val name = nameForExpr(using st)
        '{ ($name, $tree) }
      }
    }
    val name = typeStringImpl[T]
    '{ TypeClassDerivationTest.SealedHierarchyTC[T]($name.value, $deps) }
  }

  override def forUnknown[T: Type](using Quotes): Expr[TypeClassDerivationTest.UnknownTC[T]] =
    '{ TypeClassDerivationTest.UnknownTC[T](nameFor[T]) }

  //    assertSameTypes(weakTypeOf[R], computedResultType)
  //  }
  //
  //  def applierUnapplierImpl[T: Type F : Type](using quotes: Quotes) = ???
  //
  //  val au = applyUnapplyFor(ttpe)
  //    .getOrElse(reporter.abort(c.enclosingPosition,
  //      s"Could not find unambiguous, matching pair of apply/unapply methods for $ttpe"))
  //
  //  val companion = au.unapply.owner.asClass.module
  //
  //  val expectedTpe = au.params match {
  //    case Nil => typeOf[Unit]
  //    case List(single) => single.typeSignature
  //    case _ => getType(tq"(..${au.params.map(_.typeSignature)})")
  //      assertSameTypes(expectedTpe, ftpe)
  //
  //      val applyParams = au.params match {
  //        case List(_) => List(Ident(TermName("f")))
  //        case _ => au.params.indices.map(i => q"f.${TermName(s"_${i + 1}")}")
  //      }
  //
  //      val unapplyResult = au.params match {
  //        case Nil => q"()"
  //        case _ => q"$companion.unapply(t).get"
  //      }
  //
  //      q"""
  //         new $ApplierUnapplierCls[$ttpe,$ftpe] {
  //           def apply(f: $ftpe): $ttpe = $companion.apply(..$applyParams)
  //           def unapply(t: $ttpe): $ftpe = $unapplyResult
  //         }
  //       """
  //  }
  //
  //  private def stringLiteral(tree: Tree): String = tree match
  //    case StringLiteral(str) => str
  //    case Select(StringLiteral(str), TermName("stripMargin")) => str.stripMargin
  //    case _ => abort(s"expected string literal, got $tree")
  //
  //      def typeErrorImpl(code: Tree): Tree = {
  //        val codeTree = c.parse(stringLiteral(code))
  //        try {
  //          c.typecheck(codeTree)
  //          abort("expected typechecking error, none was raised")
  //        } catch {
  //          case TypecheckException(_, msg) => q"$msg"
  //        }
  //      }

  inline def materialize[T]: TC[T] = ${ materializeImpl[T] }

  inline def nameFor[T]: String = TypeString.of[T]

  def nameForExpr[T: Type](using Quotes): Expr[String] = typeStringImpl[T] match
    case '{ $ts: TypeString[T] } => '{ $ts.value }

  inline def testTreeForType(tpeRepr: String) = assert(scala.compiletime.testing.typeChecks(tpeRepr))
}
