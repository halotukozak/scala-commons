package com.avsystem.commons
package macros

import scala.quoted.{Quotes, Type}

private[commons]
object TestMacros:
  //  import c.universe._
  //
  //  val TestObj = q"$CommonsPkg.macros.TypeClassDerivationTest"
  //  val SingletonTCObj = q"$TestObj.SingletonTC"
  //  val ApplyUnapplyTCObj = q"$TestObj.ApplyUnapplyTC"
  //  val SealedHierarchyTCObj = q"$TestObj.SealedHierarchyTC"
  //  val UnknownTCObj = q"$TestObj.UnknownTC"
  //  val DefValObj = q"$TestObj.DefVal"
  //
  //  def typeClassInstance(tpe: Type): Type = getType(tq"$TestObj.TC[$tpe]")
  //  def implementDeferredInstance(tpe: Type): Tree = q"new $TestObj.TC.Deferred[$tpe]"
  //
  //  def forSingleton(tpe: Type, singleValueTree: Tree): Tree =
  //    q"$SingletonTCObj[$tpe](${tpe.toString}, $singleValueTree)"
  //
  //  def forApplyUnapply(au: ApplyUnapply, params: List[ApplyParam]): Tree = {
  //    val deps = params.map { case ApplyParam(_, s, dv, t, _) =>
  //      val defaultValueOpt = if (dv == EmptyTree) q"$NoneObj" else q"$SomeObj($DefValObj($dv))"
  //      q"(${s.name.toString}, $t, $defaultValueOpt)"
  //    }
  //    q"$ApplyUnapplyTCObj[${au.ownerTpe}](${au.ownerTpe.toString}, $ListObj(..$deps))"
  //  }
  //
  //  def forSealedHierarchy(tpe: Type, subtypes: List[KnownSubtype]): Tree = {
  //    val deps = subtypes.map({ case KnownSubtype(_, st, tree) => q"(${st.typeSymbol.name.toString}, $tree)" })
  //    q"$SealedHierarchyTCObj[$tpe](${tpe.toString}, $ListObj(..$deps))"
  //  }
  //
  //  def forUnknown(tpe: Type): Tree =
  //    q"$UnknownTCObj[$tpe](${tpe.toString})"
  //
  //  def assertSameTypes(expected: Type, actual: Type): Unit = {
  //    if (!(expected =:= actual)) {
  //      abort(s"Types don't match, got $actual")
  //    }
  //  }
  //
  //  def testTreeForType(tpeRepr: Tree): Tree = {
  //    val Literal(Constant(repr)) = tpeRepr
  //
  //    val Typed(_, tpt) = c.parse(s"(??? : $repr)")
  //    val tpe = getType(tpt)
  //    val newTree = treeForType(tpe)
  //    val newTpe = getType(newTree)
  //
  //    assertSameTypes(tpe, newTpe)
  //    q"$PredefObj.???"
  //  }
  //
    
  //
  //
  //  private def stringLiteral(tree: Tree): String = tree match {
  //    case StringLiteral(str) => str
  //    case Select(StringLiteral(str), TermName("stripMargin")) => str.stripMargin
  //    case _ => abort(s"expected string literal, got $tree")
  //  }
  //

  //}
end TestMacros
