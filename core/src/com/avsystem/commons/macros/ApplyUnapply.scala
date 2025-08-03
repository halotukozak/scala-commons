package com.avsystem.commons
package macros

import scala.quoted.*

import SharedExtensions.*

case class ApplyUnapply[T](
  ownerTpe: Type[T],
  typedCompanion: Expr[?],
  apply: Expr[Seq[Any] => T],
  unapply: Expr[Any => Option[T]],
  params: List[Expr[T => Any]]
) {
  def standardCaseClass: Boolean = ???
  // apply.isConstructor

  def synthetic: Boolean = ??? // (apply.isConstructor || apply.isSynthetic) && unapply.isSynthetic

  def defaultValueFor(param: Symbol): Expr[?] =
    ??? //  defaultValueFor(param, params.indexOf(param))

  def defaultValueFor(param: Symbol, idx: Int): Expr[?] =
    ??? //   if (param.asTerm.isParamWithDefault) {
    //   val methodEncodedName = param.owner.name.encodedName.toString
    //   q"$typedCompanion.${TermName(s"$methodEncodedName$$default$$${idx + 1}")}[..${ownerTpe.typeArgs}]"
    // } else EmptyTree

  def mkApply[T: ToExpr](args: Seq[T]): Expr[?] =
    ??? //  if (standardCaseClass) q"new $ownerTpe(..$args)"
  //  else q"$typedCompanion.apply[..${ownerTpe.typeArgs}](..$args)"
}

def applyUnapplyFor[T: Type](using quotes: Quotes): Option[ApplyUnapply[T]] =
  companionOf[T].flatMap(applyUnapplyFor[T])

def applyUnapplyFor[T: Type](companion: Expr[?])(using quotes: Quotes): Option[ApplyUnapply[T]] = {
  import quotes.reflect.*

  val dtpe = TypeRepr.of[T].dealias
  val caseClass = dtpe.classSymbol.exists(_.flags.is(Flags.Case))

  def params(methodSig: Symbol): List[Expr[T => Any]] =
    methodSig.paramSymss.head.map(Ref(_).asExprOf[T => Any])

  // Seq is a weird corner case where technically an apply/unapplySeq pair exists but is recursive
  val applyUnapplyPairs =
    if (companion.asTerm.symbol == TypeRepr.of[Seq].typeSymbol.companionClass) Nil
    else
      for {
        apply <- companion.asTerm.symbol.methodMembers.filter(_.name == "apply")
        _ = apply.dbg
        unapplyName = if (isFirstListVarargs(apply)) "unapplySeq" else "unapply"
        unapply <- companion.asTerm.symbol.methodMembers.filter(_.name == unapplyName)
      } yield (apply, unapply)

  def setTypeArgs(sig: TypeRepr) = sig match
    case PolyType(_, params, resultType) => resultType.substituteTypes(params.map(_.termSymbol), dtpe.typeArgs)
    case _ => sig

  def typeParamsMatch(apply: Symbol, unapply: Symbol) = {
    val expected = dtpe.typeArgs.length
    apply.typeRef.typeArgs.lengthIs == expected && unapply.typeRef.typeArgs.lengthIs == expected
  }

  if (caseClass && applyUnapplyPairs.isEmpty) { // case classes with more than 22 fields
    report.errorAndAbort(s"tutaj")
    // todo
    // val constructor = primaryConstructorOf(dtpe)
    // Some(ApplyUnapply(dtpe, typedCompanion, constructor, NoSymbol, params(constructor.typeSignatureIn(dtpe))))
  } else {
    val applicableResults = applyUnapplyPairs.flatMap {
      case (apply, unapply) if caseClass && apply.flags.is(Flags.Synthetic) && unapply.flags.is(Flags.Synthetic) =>
        val constructor = dtpe.typeSymbol.primaryConstructor
        Some(
          ApplyUnapply(
            Type.of[T],
            companion,
            Ref(constructor).asExprOf[Any => T],
            Ref(unapply).asExprOf[Any => Option[T]],
            params(constructor)
          )
        )
      case (apply, unapply) if typeParamsMatch(apply, unapply) =>
        val applySig =
          setTypeArgs(apply.typeRef) match
            case mt: MethodType => mt
            case _ => ???

        val unapplySig =
          setTypeArgs(unapply.typeRef) match
            case mt: MethodType => mt
            case _ => ???
        if (matchingApplyUnapply(dtpe, applySig, unapplySig))
          Some(
            ApplyUnapply(
              Type.of[T],
              companion,
              Ref(apply).asExprOf[Any => T],
              Ref(unapply).asExprOf[Any => Option[T]],
              params(applySig.typeSymbol)
            )
          )
        else None
      case _ => None
    }

    def choose(results: List[ApplyUnapply[T]]): Option[ApplyUnapply[T]] = results match {
      case Nil => None
      case List(result) => Some(result)
      case multiple if multiple.exists(_.synthetic) =>
        // prioritize non-synthetic apply/unapply pairs
        choose(multiple.filterNot(_.synthetic))
      case _ => None
    }

    choose(applicableResults)
  }
}
