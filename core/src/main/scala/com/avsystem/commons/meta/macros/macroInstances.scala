//package com.avsystem.commons
//package meta.macros
//
//import MacroUtils.*
//import meta.MacroInstances
//import meta.MacroInstances.materializeWith
//
//import scala.quoted.*
//
//
//def macroInstancesImpl[Implicits, Instances](using quotes: Quotes): Expr[MacroInstances[Implicits, Instances]] = {
//  import quotes.reflect.*
//
//  lazy val MaterializeWithAT = TypeRepr.of[materializeWith].typeSymbol
//
//  lazy val enclosingClasses = {
//    def enclosingSym: Symbol = 
//      Symbol.spliceOwner match
//      case sym if sym.isPackageDef =>
//        Iterator.iterate(sym)(_.owner).find(m => m.isClassDef && m.flags.is(Flags.Module)).getOrElse(Symbol.noSymbol)
//      case sym => sym
//
//    Iterator.iterate(enclosingSym)(_.owner)
//      .takeWhile(sym => sym != Symbol.noSymbol)
//      .toList
//  }
//
//  val resultTpe = TypeRepr.of[MacroInstances[Implicits, Instances]]
//  val applySig = resultTpe.classSymbol.get.singleMethodMember("apply")
//  val implicitsTpe = applySig.paramSymss.head.head.typeRef
//  val instancesTpe = applySig.typeRef
//
//  val instTs = instancesTpe.classSymbol.filter(_.flags.isAnyOf(Flags.Trait, Flags.Abstract))
//    .getOrElse(report.errorAndAbort(s"Expected trait or abstract class type, got $instancesTpe"))
//
//  val instancesMethods = instTs.methodMembers.iterator
//    .filter(m => m.flags.is(Flags.Deferred) && !m.flags.is(Flags.FieldAccessor)).toList.reverse
//
//  def impl(singleMethod: Option[Symbol]): Tree = {
//    val impls = instancesMethods.map { m =>
//      val sig = m.tree.asInstanceOf[DefDef]
//      val resultTpe = sig.returnTpt.tpe.dealias
//
//      val materializer =
//        if (singleMethod.exists(_ != m))
//          '{ ??? }
//        else m.getAnnotation(MaterializeWithAT) match {
//          case Some(annot) =>
//            //            val errorPos = annot.errorPos.getOrElse(c.enclosingPosition)
//            val errorPos = annot.pos
//            annot match {
//              case Apply(_, List(prefix, macroNameTree)) =>
//                val macroName = macroNameTree match {
//                  case Literal(name: String) => name
//                  case t if t.symbol.flags.is(Flags.Synthetic) && t.symbol.name == "<init>$default$2" =>
//                    "materialize"
//                  case _ => report.errorAndAbort("expected string literal as second argument of @materializeWith", errorPos)
//                }
//                '{ macroName }
//              case _ =>
//                report.errorAndAbort("bad @materializeWith annotation", errorPos)
//            }
//          case None =>
//            def typedCompanionOf(tpe: TypeRepr): Option[Tree] = {
//              def singleValueFor(tpe: TypeRepr) =
//
//                tpe match {
//                  case ThisType(sym) if enclosingClasses.contains(sym) =>
//                    Some(This(sym))
//                  case ThisType(sym) if sym.isModuleClass =>
//                    singleValueFor(internal.thisType(sym.owner)).map(pre => Select(pre, tpe.termSymbol))
//                  case ThisType(sym) =>
//                    Some(This(sym))
//                  case SingleType(NoPrefix, sym) =>
//                    Some(Ident(sym))
//                  case SingleType(pre, sym) =>
//                    singleValueFor(pre).map(prefix => Select(prefix, sym))
//                  case ConstantType(value) =>
//                    Some(Literal(value))
//                  case TypeRef(pre, sym, Nil) if sym.isModuleClass =>
//                    singleValueFor(pre).map(prefix => Select(prefix, sym.asClass.module))
//                  case _ =>
//                    None
//                }
//
//              val result = tpe match
//                case TypeRef(pre, sym) if sym.companion != NoSymbol =>
//                  singleValueFor(pre).map(Select(_, sym.companion)) orElse singleValueFor(tpe.companion)
//                case TypeRef(NoPrefix, sym, _) =>
//                  // apparently, sym.companion returns NoSymbol for local classes, so we have to find the companion manually
//                  val companionRef = Ident(sym.name.toTermName)
//                  typecheck(companionRef, silent = true) match {
//                    case EmptyTree => None
//                    case tree if tree.symbol.isModule => Some(tree)
//                    case _ => None
//                  }
//                case _ =>
//                  singleValueFor(tpe.companion)
//              result.map(typecheck(_))
//            }
//
//            val resultCompanion = typedCompanionOf(resultTpe)
//              .getOrElse(abort(s"$resultTpe has no companion object with `materialize` macro"))
//            q"$resultCompanion.materialize"
//        }
//
//      val instTpeTree = treeForType(sig.finalResultType)
//      if (!m.isGetter) {
//        val tparamDefs = sig.typeParams.map(typeSymbolToTypeDef(_, forMethod = true))
//        val paramDefs = sig.paramLists.map(_.map(paramSymbolToValDef))
//        val argss = sig.paramLists match {
//          case List(Nil) => Nil
//          case paramss => paramss.filterNot(_.exists(_.isImplicit)).map(_.map(s => q"${s.name.toTermName}"))
//        }
//        q"def ${m.name}[..$tparamDefs](...$paramDefs): $instTpeTree = $materializer(...$argss)"
//      }
//      else if (m.isVar || m.setter != NoSymbol)
//        q"var ${m.name}: $instTpeTree = $materializer"
//      else
//        q"val ${m.name}: $instTpeTree = $materializer"
//    }
//
//    val implicitsName = c.freshName(TermName("implicits"))
//
//    def implicitImports(tpe: Type, expr: Tree): List[Tree] = {
//      val dtpe = tpe.dealias
//      if (dtpe =:= typeOf[Unit]) Nil
//      else if (definitions.TupleClass.seq.contains(dtpe.typeSymbol))
//        dtpe.typeArgs.zipWithIndex.flatMap {
//          case (ctpe, idx) => implicitImports(ctpe, q"$expr.${TermName(s"_${idx + 1}")}")
//        }
//      else List(q"import $expr._")
//    }
//
//    q"""
//        new $resultTpe {
//          def apply($implicitsName: $implicitsTpe, $CompanionParamName: Any): $instancesTpe = {
//            ..${implicitImports(implicitsTpe, Ident(implicitsName))}
//            new $instancesTpe { ..$impls; () }
//          }
//        }
//       """
//  }
//
//  //If full implementation doesn't typecheck, find the first problematic typeclass and limit
//  //compilation errors to that one in order to not overwhelm the user but rather report errors gradually
//  val fullImpl = impl(None)
//  debug(show(fullImpl))
//  val result = c.typecheck(fullImpl, silent = true) match {
//    case EmptyTree =>
//      instancesMethods.iterator.map(m => impl(Some(m)))
//        .find(t => c.typecheck(t, silent = true) == EmptyTree)
//        .getOrElse(fullImpl)
//    case t => t
//  }
//
//  enclosingConstructorCompanion match {
//    case NoSymbol => result
//    case companionSym =>
//      // Replace references to companion object being constructed with casted reference to
//      // `companion` parameter. All this horrible wiring is to workaround stupid overzealous Scala validation of
//      // self-reference being passed to super constructor parameter (https://github.com/scala/bug/issues/7666)
//      // We're going to replace some parts of already typechecked tree. This means we must insert already
//      // typechecked replacements.
//
//      val replacementDecl = result.find {
//        case ValDef(mods, CompanionParamName, _, EmptyTree) => mods.hasFlag(Flag.PARAM)
//        case _ => false
//      }
//      val replacementSym = replacementDecl.fold(NoSymbol)(_.symbol)
//
//      // must construct tree which is already fully typechecked
//      def replacementTree(orig: Tree): Tree = {
//        val replacementIdent = internal.setType(
//          internal.setSymbol(Ident(CompanionParamName), replacementSym),
//          internal.singleType(NoPrefix, replacementSym),
//        )
//        val asInstanceOfMethod = definitions.AnyTpe.member(TermName("asInstanceOf"))
//        val asInstanceOfSelect = internal.setType(
//          internal.setSymbol(Select(replacementIdent, asInstanceOfMethod), asInstanceOfMethod),
//          asInstanceOfMethod.info,
//        )
//        val typeAppliedCast = internal.setType(
//          internal.setSymbol(TypeApply(asInstanceOfSelect, List(TypeTree(orig.tpe))), asInstanceOfMethod),
//          orig.tpe,
//        )
//        typeAppliedCast
//      }
//
//      object replacer extends TreeMap {
//        override def transformTree(tree: Tree)(owner: Symbol): Tree = tree match {
//          case This(_) if tree.symbol == companionSym.asModule.moduleClass => replacementTree(tree)
//          case _ if tree.symbol == companionSym => replacementTree(tree)
//          case _ => super.transformTree(tree)(owner)
//        }
//      }
//
//      replacer.transform(result)
//  }
//}