//package com.avsystem.commons
//package misc.macros
//
//private def symbolImplicitNotFoundMsg(tpe: Type, sym: Symbol, tparams: List[Symbol], typeArgs: List[Type]): String =
//    rawAnnotations(sym).find(_.tree.tpe <:< ImplicitNotFoundAT)
//      .map(_.tree.children.tail.head)
//      .collect {
//        case StringLiteral(error) => error
//        case NamedArgTree(_, StringLiteral(error)) => error
//      }
//      .map { error =>
//        val tpNames = tparams.map(_.name.decodedName.toString)
//        (tpNames zip typeArgs).foldLeft(error) {
//          case (err, (tpName, tpArg)) => err.replace(s"$${$tpName}", tpArg.toString)
//        }
//      }
//      .getOrElse {
//        if (sym != tpe.typeSymbol) standardImplicitNotFoundMsg(tpe)
//        else s"no implicit value of type $tpe found"
//      }
//
//private def replaceArgs(stack: List[Type], error: String, params: List[Symbol], args: List[Tree]): String =
//  (params zip args)
//    .flatMap { case (param, arg) =>
//      arg.tpe.baseType(ImplicitNotFoundSym).typeArgs.headOption.map { delTpe =>
//        param.name.decodedName.toString -> implicitNotFoundMsg(stack, delTpe, arg)
//      }
//    }
//    .foldLeft(error) { case (err, (paramName, replacement)) =>
//      err.replace(s"#{$paramName}", replacement)
//    }
//
//private def implicitNotFoundMsg(stack: List[Type], tpe: Type, tree: Tree): String =
//  if (stack.exists(_ =:= tpe))
//    standardImplicitNotFoundMsg(tpe)
//  else tree match {
//    case MaybeApply(MaybeTypeApply(fun, typeArgs), args) =>
//      val sym = Option(fun.symbol).getOrElse(NoSymbol)
//      val sig = sym.typeSignature
//      val targs = typeArgs.map(_.tpe)
//      val baseMsg = symbolImplicitNotFoundMsg(tpe, sym, sig.typeParams, targs)
//      replaceArgs(tpe :: stack, baseMsg, sig.paramLists.headOption.getOrElse(Nil), args)
//  }
//
//def implicitNotFoundMsg(tpe: Type): String =
//  implicitNotFoundMsg(Nil, tpe, inferImplicitValue(getType(tq"$ImplicitNotFoundCls[$tpe]")))
//
//private def standardImplicitNotFoundMsg(tpe: Type): String =
//  symbolImplicitNotFoundMsg(tpe, tpe.typeSymbol, tpe.typeSymbol.typeSignature.typeParams, tpe.typeArgs)
