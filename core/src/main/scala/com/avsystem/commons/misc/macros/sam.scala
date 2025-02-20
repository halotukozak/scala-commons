package com.avsystem.commons
package misc.macros

import scala.languageFeature.experimental.macros
import scala.quoted.*

def createSamImpl[Target: Type, Fun: Type](fun: Expr[Fun])(using quotes: Quotes): Expr[Target] = {
  import quotes.reflect.*
  return '{ ??? }

  val byName = validateSamImpl[Target, Fun].valueOrAbort
  val targetTpe = TypeRepr.of[Target].dealias
  val (m: Symbol, mDefDef: DefDef) = targetTpe.classSymbol.get.methodMembers.collectFirst {
    case m if m.flags.is(Flags.Deferred) => (m, m.tree)
  }.get: @unchecked

  if byName then {
    val name: String = Symbol.freshName("$anon")
    val parents = List(TypeTree.of[Object], TypeTree.of[Target])

    def decls(cls: Symbol): List[Symbol] = Symbol.newMethod(
      parent = cls,
      name = m.name,
      tpe = MethodType(Nil)(_ => Nil, _ => mDefDef.returnTpt.tpe),
      flags = Flags.Override,
      privateWithin = Symbol.noSymbol,
    ) :: Nil

    val cls = Symbol.newClass(
      parent = Symbol.spliceOwner,
      name = name,
      parents = parents.map(_.tpe),
      decls = decls,
      selfType = None,
    )

    val mSym = cls.declaredMethod(m.name).head
    val defDef = DefDef(mSym, _ => Some(fun.asTerm))
    val clsDef = ClassDef(cls, parents, body = List(defDef))
    val newCls = Typed(Apply(Select(New(TypeIdent(cls)), cls.primaryConstructor), Nil), TypeTree.of[Target])
    Block(List(clsDef), newCls).asExprOf[Target]
  } else {
    '{
      ???
    }
  }

  //      '{ new Target { def ${m.name.toTermName} = $fun } }
  //      q"""
  //            new $targetTpe {
  //              def ${m.name.toTermName} = $fun
  //            }
  //  } else {
  //    val mDefDef = m.typeSignatureIn(targetTpe)
  //
  //    val resultType = mDefDef.finalResultType
  //    val defParamss = mDefDef.paramLists.map(_.map { ps =>
  //      val implicitFlag = if ps.isImplicit then Flag.IMPLICIT else NoFlags
  //      ValDef(Modifiers(Flag.PARAM | implicitFlag), ps.name.toTermName, TypeTree(ps.typeSignature), EmptyTree)
  //    })
  //
  //    val unimplemented = q"???"
  //    val baseResult = typecheck(q"""
  //          new $targetTpe {
  //            def ${m.name.toTermName}(...$defParamss): $resultType = $unimplemented
  //          }
  //         """)
  //
  //    val typedDefParamss = baseResult.collect { case dd: DefDef if dd.symbol.overrides.contains(m) => dd.vparamss }.head
  //
  //    def rewriteParams(function: Tree, defParamss: List[List[ValDef]]): Tree =
  //      (function, defParamss) match {
  //        case (Function(funParams, body), defParams :: dpTail) =>
  //          val defParamByFunParam = (funParams.map(_.symbol) zip defParams).toMap
  //          object transformer extends Transformer {
  //            override def transform(tree: Tree) = tree match {
  //              case id: Ident if defParamByFunParam.contains(id.symbol) =>
  //                val defParam = defParamByFunParam(id.symbol)
  //                internal.setSymbol(treeCopy.Ident(id, defParam.name), defParam.symbol)
  //              case _ => super.transform(tree)
  //            }
  //          }
  //          rewriteParams(transformer.transform(body), dpTail)
  //        case (body, _) =>
  //          val paramss = defParamss.map(_.map(vd => Ident(vd.symbol)))
  //          typecheck(q"$body(...$paramss)")
  //      }
  //
  //    val defBody = rewriteParams(fun, typedDefParamss)
  //
  //    object transformer extends Transformer {
  //      override def transform(tree: Tree) = tree match {
  //        case DefDef(mods, name, tparams, vparamss, resultTpe, _) if tree.symbol.overrides.contains(m) =>
  //          treeCopy.DefDef(tree, mods, name, tparams, vparamss, resultTpe, defBody)
  //        case _ => super.transform(tree)
  //      }
  //    }
  //
  //    transformer.transform(baseResult)
  //  }
}

def validateSamImpl[Target: Type, Fun: Type](using quotes: Quotes): Expr[Boolean] = {
  val macroUtils = new HasMacroUtils {}
  import macroUtils.*
  import quotes.reflect.*

  val targetTpe = TypeRepr.of[Target].widen
  val funTpe = TypeRepr.of[Fun].widen

  val targetSymbol =
    targetTpe.dealias.classSymbol getOrElse report.errorAndAbort(s"${targetTpe.show} is not a class or trait")
  if !targetSymbol.flags.isAnyOf(Flags.Abstract, Flags.Trait) then
    report.errorAndAbort(s"${targetTpe.show} is not a trait or abstract class")
  val excludeMethods = defn.ObjectClass.methodMembers.map(_.name) // todo: replace with a more precise filter

  val (m: Symbol, sig: DefDef) =
    targetSymbol.methodMembers
      .filterNot(m => excludeMethods.contains(m.name))
      .filter(m => m.flags.is(Flags.Deferred))
      .map(m => (m, m.tree)) match
      case (m: Symbol, sig: DefDef) :: Nil
          if m.flags.is(Flags.Method) &&
            m.isPublic &&
            !m.flags.is(Flags.FieldAccessor) &&
            !m.flags.is(Flags.AbsOverride) &&
            sig.paramss.collectFirst { case _: TypeParamClause => () }.isEmpty =>
        (m, sig)
      case _ =>
        report.errorAndAbort("Target trait/class must have exactly one public, abstract, non-generic method")

  val argTypess = sig.paramss.collect { case termParamClause: TermParamClause => termParamClause.params.map(_.tpt.tpe) }
  val finalResultType = if sig.returnTpt.tpe =:= TypeRepr.of[Unit] then TypeRepr.of[Any] else sig.returnTpt.tpe

  val requiredFunTpe = argTypess.foldRight(finalResultType) { (argTypes: List[TypeRepr], resultType: TypeRepr) =>
    val funSym: Symbol = defn.FunctionClass(argTypess.size)
    val (typeRepr: TypeRepr, _) = TypeRef.unapply(funSym.typeRef)
    typeRepr.appliedTo(argTypes :+ resultType)
  }

  val emptyList = argTypess == List(Nil)
  val byName = emptyList && funTpe <:< finalResultType
  if !byName && !(funTpe <:< requiredFunTpe) then {
    val requiredMsg = s"${if emptyList then "" else s"${finalResultType.show} or "}${requiredFunTpe.show}"
    report.errorAndAbort(
      s"${funTpe.show} does not match signature of $m in ${targetTpe.show}: expected $requiredMsg",
    ) // todo
  }
  Expr(byName)
}
