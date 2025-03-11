package com.avsystem.commons

import annotation.positioned
import misc.macros.MacroCommons
import serialization.optionalParam

import scala.collection.mutable
import scala.quoted.*

trait HasMacroUtils extends MacroCommons {

  extension (using quotes: Quotes)(any: Any) {
    inline def info: any.type = {
      quotes.reflect.report.info(s"${any}")
      any
    }
  }

  extension (tpe: Type[?])(using Quotes) {
    def show = Type.show(using tpe)
  }

  inline final def printTree[T](inline x: T): Nothing = ${ PrintTree.printTreeImpl('{ x }) }

  final def printSymbolInfo(using quotes: Quotes)(symbol: quotes.reflect.Symbol): Unit = println {
    s"""
       |maybeOwner: ${symbol.maybeOwner}
       |flags: ${symbol.flags.show}
       |privateWithin: ${symbol.privateWithin.map(_.show)}
       |protectedWithin: ${symbol.protectedWithin.map(_.show)}
       |name: ${symbol.name}
       |fullName: ${symbol.fullName}
       |pos: ${symbol.pos}
       |docstring: ${symbol.docstring}
       |tree: ${Try(symbol.tree.show) getOrElse "no tree"}
       |annotations: ${symbol.annotations.map(_.show)}
       |isDefinedInCurrentRun: ${symbol.isDefinedInCurrentRun}
       |isLocalDummy: ${symbol.isLocalDummy}
       |isRefinementClass: ${symbol.isRefinementClass}
       |isAliasType: ${symbol.isAliasType}
       |isAnonymousClass: ${symbol.isAnonymousClass}
       |isAnonymousFunction: ${symbol.isAnonymousFunction}
       |isAbstractType: ${symbol.isAbstractType}
       |isClassConstructor: ${symbol.isClassConstructor}
       |isSuperAccessor: ${symbol.isSuperAccessor}
       |isType: ${symbol.isType}
       |isTerm: ${symbol.isTerm}
       |isPackageDef: ${symbol.isPackageDef}
       |isClassDef: ${symbol.isClassDef}
       |isTypeDef: ${symbol.isTypeDef}
       |isValDef: ${symbol.isValDef}
       |isDefDef: ${symbol.isDefDef}
       |isBind: ${symbol.isBind}
       |isNoSymbol: ${symbol.isNoSymbol}
       |exists: ${symbol.exists}
       |declaredFields: ${symbol.declaredFields}
       |fieldMembers: ${symbol.fieldMembers}
       |declaredMethods: ${symbol.declaredMethods}
       |methodMembers: ${symbol.methodMembers}
       |declaredTypes: ${symbol.declaredTypes}
       |typeMembers: ${symbol.typeMembers}
       |declarations: ${symbol.declarations}
       |paramSymss: ${symbol.paramSymss}
       |allOverriddenSymbols: ${symbol.allOverriddenSymbols}
       |primaryConstructor: ${symbol.primaryConstructor}
       |caseFields: ${symbol.caseFields}
       |isTypeParam: ${symbol.isTypeParam}
       |paramVariance: ${symbol.paramVariance.show}
       |signature: ${symbol.signature}
       |moduleClass: ${symbol.moduleClass}
       |companionClass: ${symbol.companionClass}
       |companionModule: ${symbol.companionModule}
       |children: ${symbol.children}
       |typeRef: ${Try(symbol.typeRef.show) getOrElse "no typeRef"}
       |termRef: ${Try(symbol.termRef.show) getOrElse "no termRef"}
       |""".stripMargin
  }

  final def printTypeReprInfo(using quotes: Quotes)(tpe: quotes.reflect.TypeRepr): Unit = println {
    s"""
       |type: ${tpe.show}
       |widen: ${tpe.widen}
       |widenTermRefByName: ${tpe.widenTermRefByName}
       |widenByName: ${tpe.widenByName}
       |dealias: ${tpe.dealias}
       |dealiasKeepOpaques: ${tpe.dealiasKeepOpaques}
       |simplified: ${tpe.simplified}
       |classSymbol: ${tpe.classSymbol}
       |typeSymbol: ${tpe.typeSymbol}
       |termSymbol: ${tpe.termSymbol}
       |isSingleton: ${tpe.isSingleton}
       |baseClasses: ${tpe.baseClasses}
       |isFunctionType: ${tpe.isFunctionType}
       |isContextFunctionType: ${tpe.isContextFunctionType}
       |isErasedFunctionType: ${tpe.isErasedFunctionType}
       |isDependentFunctionType: ${tpe.isDependentFunctionType}
       |isTupleN: ${tpe.isTupleN}
       |typeArgs: ${tpe.typeArgs}
       |""".stripMargin
  }

  final def printTreeInfo(using quotes: Quotes)(tree: quotes.reflect.Tree): Unit = {
    import quotes.reflect.*

    println("Structure " + tree.show(using Printer.TreeStructure))
    println("ShortCode " + tree.show(using Printer.TreeShortCode))
  }

  final def recognizeTree(using quotes: Quotes)(tree: quotes.reflect.Tree): Nothing = {
    import quotes.reflect.*

    val treeRecognizer: TreeAccumulator[List[String]] = new TreeAccumulator[List[String]] {
      override def foldTree(x: List[String], tree: Tree)(owner: Symbol): List[String] = {
        tree match {
          case Ident(_) =>
            foldOverTree("Ident" :: x, tree)(owner)
          case Select(qualifier, _) =>
            foldOverTree("Select" :: x, tree)(owner)
          case This(qual) =>
            foldOverTree("This" :: x, tree)(owner)
          case Super(qual, _) =>
            foldOverTree("Super" :: x, tree)(owner)
          case Apply(fun, args) =>
            foldOverTree("Apply" :: x, tree)(owner)
          case TypeApply(fun, args) =>
            foldOverTree("TypeApply" :: x, tree)(owner)
          case Literal(const) =>
            foldOverTree("Literal" :: x, tree)(owner)
          case New(tpt) =>
            foldOverTree("New" :: x, tree)(owner)
          case Typed(expr, tpt) =>
            foldOverTree("Typed" :: x, tree)(owner)
          case TypedOrTest(expr, tpt) =>
            foldOverTree("TypeOrTest" :: x, tree)(owner)
          case NamedArg(_, arg) =>
            foldOverTree("NamedArg" :: x, tree)(owner)
          case Assign(lhs, rhs) =>
            foldOverTree("Assign" :: x, tree)(owner)
          case Block(stats, expr) =>
            foldOverTree("Block" :: x, tree)(owner)
          case If(cond, thenp, elsep) =>
            foldOverTree("If" :: x, tree)(owner)
          case While(cond, body) =>
            foldOverTree("While" :: x, tree)(owner)
          case Closure(meth, tpt) =>
            foldOverTree("Closure" :: x, tree)(owner)
          case Match(selector, cases) =>
            foldOverTree("Match" :: x, tree)(owner)
          case Return(expr, _) =>
            foldOverTree("Return" :: x, tree)(owner)
          case Try(block, handler, finalizer) =>
            foldOverTree("Try" :: x, tree)(owner)
          case Repeated(elems, elemtpt) =>
            foldOverTree("Repeated" :: x, tree)(owner)
          case Inlined(call, bindings, expansion) =>
            foldOverTree("Inlined" :: x, tree)(owner)
          case vdef @ ValDef(_, tpt, rhs) =>
            foldOverTree("ValDef" :: x, tree)(owner)
          case ddef @ DefDef(_, paramss, tpt, rhs) =>
            foldOverTree("DefDef" :: x, tree)(owner)
          case tdef @ TypeDef(_, rhs) =>
            foldOverTree("TypeDef" :: x, tree)(owner)
          case cdef @ ClassDef(_, constr, parents, self, body) =>
            foldOverTree("ClassDef" :: x, tree)(owner)
          case Import(expr, _) =>
            foldOverTree("Import" :: x, tree)(owner)
          case Export(expr, _) =>
            foldOverTree("Export" :: x, tree)(owner)
          case clause @ PackageClause(pid, stats) =>
            foldOverTree("PackageClause" :: x, tree)(owner)
          case Inferred() =>
            foldOverTree("Inferred" :: x, tree)(owner)
          case TypeIdent(_) =>
            foldOverTree("TypeIdent" :: x, tree)(owner)
          case TypeSelect(qualifier, _) =>
            foldOverTree("TypeSelect" :: x, tree)(owner)
          case TypeProjection(qualifier, _) =>
            foldOverTree("TypeProjection" :: x, tree)(owner)
          case Singleton(ref) =>
            foldOverTree("Singleton" :: x, tree)(owner)
          case Refined(tpt, refinements) =>
            foldOverTree("Refined" :: x, tree)(owner)
          case Applied(tpt, args) =>
            foldOverTree("Applied" :: x, tree)(owner)
          case ByName(result) =>
            foldOverTree("ByName" :: x, tree)(owner)
          case Annotated(arg, annot) =>
            foldOverTree("Annotated" :: x, tree)(owner)
          case LambdaTypeTree(typedefs, arg) =>
            foldOverTree("LambdaTypeTree" :: x, tree)(owner)
          case TypeBind(_, tbt) =>
            foldOverTree("TypeBind" :: x, tree)(owner)
          case TypeBlock(typedefs, tpt) =>
            foldOverTree("TypeBlock" :: x, tree)(owner)
          case MatchTypeTree(boundopt, selector, cases) =>
            foldOverTree("MatchTypeTree" :: x, tree)(owner)
          case WildcardTypeTree() =>
            foldOverTree("WildcardTypeTree" :: x, tree)(owner)
          case TypeBoundsTree(lo, hi) =>
            foldOverTree("TypeBoundsTree" :: x, tree)(owner)
          case CaseDef(pat, guard, body) =>
            foldOverTree("CaseDef" :: x, tree)(owner)
          case TypeCaseDef(pat, body) =>
            foldOverTree("TypeCaseDef" :: x, tree)(owner)
          case Bind(_, body) =>
            foldOverTree("Bind" :: x, tree)(owner)
          case Unapply(fun, implicits, patterns) =>
            foldOverTree("Unapply" :: x, tree)(owner)
          case Alternatives(patterns) =>
            foldOverTree("Alternatives" :: x, tree)(owner)
          case SummonFrom(cases) =>
            foldOverTree("SummonFrom" :: x, tree)(owner)
          case _ => throw MatchError(tree.show(using Printer.TreeStructure))
        }
      }
    }
    report.errorAndAbort {
      treeRecognizer.foldTree(Nil, tree)(Symbol.spliceOwner).toString
    }
  }
}

object HasMacroUtils extends HasMacroUtils

object PrintTree {
  def printTreeImpl[T: Type](x: Expr[T])(using quotes: Quotes): Expr[Nothing] = {
    import quotes.reflect.*

    println("Structure " + x.asTerm.show(using Printer.TreeStructure))
    println("ShortCode " + x.asTerm.show(using Printer.TreeShortCode))
    report.errorAndAbort("here")
  }
}
