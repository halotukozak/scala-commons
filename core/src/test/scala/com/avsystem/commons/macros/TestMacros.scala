package com.avsystem.commons
package macros

import macros.TestMacros.nameFor
import macros.TypeClassDerivationTest.{DefVal, TC}
import misc.TypeString
import misc.macros.{ApplyUnapply, TypeClassDerivation}

import scala.compiletime.summonInline
import scala.quoted.*

private[commons] object TestMacros extends TypeClassDerivation[TypeClassDerivationTest.TC] {

  override def typeClassInstance[T: Type](using Quotes): Type[TC[T]] =
    Type.of[TC[T]]

  override def implementDeferredInstance[T: Type](using Quotes): Expr[TypeClassDerivationTest.TC.Deferred[T]] =
    '{ new TypeClassDerivationTest.TC.Deferred[T] }

  override def forSingleton[T: Type](singleValue: Expr[ValueOf[T]])(using
    Quotes,
  ): Expr[TypeClassDerivationTest.SingletonTC[T]] =
    '{ TypeClassDerivationTest.SingletonTC[T](nameFor[T], $singleValue.value) }

  override def forApplyUnapply[T: Type](
    au: ApplyUnapply[T],
  )(using Quotes): Expr[TypeClassDerivationTest.ApplyUnapplyTC[T]] = {
    val deps = Expr.ofList {
      au.params.map { case ApplyUnapply.Param(label, index, tpe, repeated, dv) =>
        val name = Expr(label)
        val tc = tpe match
          case '[t] => '{ summonInline[TC[t]] }
        val defaultValueOpt = dv match
          case Some(value) => '{ Some(DefVal($value())) }
          case None => '{ None }
        '{ ($name, $tc, $defaultValueOpt) }
      }
    }
    au.ownerTpe match
      case '[t] =>
        '{ TypeClassDerivationTest.ApplyUnapplyTC[T](nameFor[t], $deps) }
  }

  override def forSealedHierarchy[T: Type](
    subtypes: List[KnownSubtype[?]],
  )(using Quotes): Expr[TypeClassDerivationTest.SealedHierarchyTC[T]] = {
    val deps: Expr[List[(String, TC[? <: T])]] = Expr.ofList {
      subtypes.map { case KnownSubtype(_, st, tree) =>
        st match
          case '[TC[t]] =>
            Expr.ofTuple(('{ nameFor[t] }, tree.asExprOf[TC[? <: T]]))
      }
    }
    '{ TypeClassDerivationTest.SealedHierarchyTC[T](nameFor[T], $deps) }
  }

  override def forUnknown[T: Type](using Quotes): Expr[TypeClassDerivationTest.UnknownTC[T]] =
    '{ TypeClassDerivationTest.UnknownTC[T](nameFor[T]) }

  inline def materialize[T]: TC[T] = ${ materializeImpl[T] }

  inline def nameFor[T]: String = TypeString.of[T]

  inline def testTreeForType(tpeRepr: String) = assert(scala.compiletime.testing.typeChecks(tpeRepr))
}
