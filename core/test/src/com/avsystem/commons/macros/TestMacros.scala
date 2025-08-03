package com.avsystem.commons.macros

import com.avsystem.commons.macros.ApplierUnapplier
import scala.quoted.Quotes

import com.avsystem.commons.macros._

private[commons] object TestMacros
// extends TypeClassDerivation
{

//   def typeClassInstance(tpe: Type): Type = getType(tq"$TestObj.TC[$tpe]")
//   def implementDeferredInstance(tpe: Type): Tree = q"new $TestObj.TC.Deferred[$tpe]"

//   def forSingleton(tpe: Type, singleValueTree: Tree): Tree =
//     q"$SingletonTCObj[$tpe](${tpe.toString}, $singleValueTree)"

//   def forApplyUnapply(au: ApplyUnapply, params: List[ApplyParam]): Tree = {
//     val deps = params.map { case ApplyParam(_, s, dv, t, _) =>
//       val defaultValueOpt = if (dv == EmptyTree) q"$NoneObj" else q"$SomeObj($DefValObj($dv))"
//       q"(${s.name.toString}, $t, $defaultValueOpt)"
//     }
//     q"$ApplyUnapplyTCObj[${au.ownerTpe}](${au.ownerTpe.toString}, $ListObj(..$deps))"
//   }

//   def forSealedHierarchy(tpe: Type, subtypes: List[KnownSubtype]): Tree = {
//     val deps = subtypes.map({ case KnownSubtype(_, st, tree) => q"(${st.typeSymbol.name.toString}, $tree)" })
//     q"$SealedHierarchyTCObj[$tpe](${tpe.toString}, $ListObj(..$deps))"
//   }

//   def forUnknown(tpe: Type): Tree =
//     q"$UnknownTCObj[$tpe](${tpe.toString})"

//   def assertSameTypes(expected: Type, actual: Type): Unit = {
//     if (!(expected =:= actual)) {
//       abort(s"Types don't match, got $actual")
//     }
//   }

//   def testTreeForType(tpeRepr: Tree): Tree = {
//     val Literal(Constant(repr)) = tpeRepr: @unchecked

//     val Typed(_, tpt) = c.parse(s"(??? : $repr)"): @unchecked
//     val tpe = getType(tpt)
//     val newTree = treeForType(tpe)
//     val newTpe = getType(newTree)

//     assertSameTypes(tpe, newTpe)
//     q"$PredefObj.???"
//   }

//   def testKnownSubtypes[T: WeakTypeTag, R: WeakTypeTag]: Tree = instrument {
//     val computedResultType = knownSubtypes(weakTypeOf[T])
//       .map(types => getType(tq"(..$types)"))
//       .getOrElse(typeOf[Nothing])

//     assertSameTypes(weakTypeOf[R], computedResultType)
//     q"$PredefObj.???"
//   }

  inline def applierUnapplier[T, F]: ApplierUnapplier[T, F] = ${ applierUnapplierImpl[T, F] }

  private def applierUnapplierImpl[T: Type, F: Type](using quotes: Quotes): Expr[ApplierUnapplier[T, F]] = {
    import quotes.reflect._

    val au = applyUnapplyFor[T].getOrElse(
      report.errorAndAbort(s"Could not find unambiguous, matching pair of apply/unapply methods for ${Type.show[T]}")
    )

    val expectedTpe = au.params.map(_.asTerm.tpe) match
      case Nil => TypeRepr.of[Unit]
      case List(single) => single
      case params => defn.TupleClass(params.length).typeRef.appliedTo(params)

    assert(expectedTpe =:= TypeRepr.of[F], s"Expected type $expectedTpe, got ${Type.show[F]}")

    val unapplyResult: Expr[T => F] = au.params match
      case Nil => '{ (_: T) => ().asInstanceOf[F] } // don't understand this case
      case _ => '{ (t: T) => ${ au.unapply }(t).get.asInstanceOf[F] }

    val applyResult: Expr[F => T] = au.params match
      case Nil => '{ (f: F) => f.asInstanceOf[T] }
      case _ =>
        '{ (f: F) =>
          val params = ${ Expr.ofList(au.params) }.map(_.apply(f.asInstanceOf[T]))
          ${ au.apply }(params)
        }
    '{
      new ApplierUnapplier[T, F] {
        def apply(f: F): T = ${ applyResult }(f)
        def unapply(t: T): F = ${ unapplyResult }(t)
      }
    }
  }

//   private def stringLiteral(tree: Tree): String = tree match {
//     case StringLiteral(str) => str
//     case Select(StringLiteral(str), TermName("stripMargin")) => str.stripMargin
//     case _ => abort(s"expected string literal, got $tree")
//   }

//   def typeErrorImpl(code: Tree): Tree = {
//     val codeTree = c.parse(stringLiteral(code))
//     try {
//       c.typecheck(codeTree)
//       abort("expected typechecking error, none was raised")
//     } catch {
//       case TypecheckException(_, msg) => q"$msg"
//     }
//   }
}
