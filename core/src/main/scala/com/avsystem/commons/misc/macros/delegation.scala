//package com.avsystem.commons
//package misc.macros
//
//import misc.Delegation
//
//import scala.quoted.{Expr, Quotes, Type}
//
//def materializeDelegationImpl[A: Type, B: Type](using quotes: Quotes): Expr[Delegation[A, B]] = '{
//  new Delegation[A, B] {
//    def delegate(source: A): B = ${ delegateImpl[A, B]('{ source }) }
//  }
//}
//
//def delegate[A: Type, Target: Type](using quotes: Quotes) = {
//  import quotes.reflect.*
//
//  val targetTpe = TypeRepr.of[Target].dealias
//
//  val targetSymbol: Symbol = targetTpe.classSymbol getOrElse
//    //  if (targetSymbol.isEmpty && !targetSymbol.get.flags.is(Flags.Trait)) { 
//    report.errorAndAbort(s"$targetTpe is not a trait or abstract class")
//  //    val wrappedName = c.freshName(TermName("w"))
//
//  val methodDelegations = targetSymbol.declarations.iterator
//    .collect { case m if m.flags.is(Flags.Deferred) => (m, m.tree) } //Abstract?
//    .collect { case (m: Symbol, tree: DefDef) => (m, tree) }
//    .flatMap { case (m: Symbol, ms: DefDef) =>
//      if !m.flags.isAnyOf(Flags.Private, Flags.PrivateLocal, Flags.Protected) &&
//        m.isDefDef && !m.flags.is(Flags.FieldAccessor) then {
//        val name = m.name
//        //          val mtpe = m.typeSignatureIn(targetTpe)
//        //          val typeNames = mtpe.typeParams.map(_.name.toTypeName)
//        //          val typeDefs = mtpe.typeParams.map(typeSymbolToTypeDef(_))
//        //          val paramNames =
//        //            mtpe.paramLists.map(_.map(p => if (isRepeated(p)) q"${p.name.toTermName}: _*" else q"${p.name.toTermName}"))
//        //          val paramLists = mtpe.paramLists.map(_.map(paramSymbolToValDef))
//        //          val resultTpeTree = treeForType(mtpe.finalResultType)
//        //
//            val result = if (ms.isGetter)
//                q"val $name: $resultTpeTree = $wrappedName.$name"
//              else
//              q"def $name[..$typeDefs](...$paramLists): $resultTpeTree = $wrappedName.$name[..$typeNames](...$paramNames)"
//
//        Some(result)
//      } else {
//        report.error(s"Can't delegate ${m.name} - only public defs and vals can be delegated")
//        None
//      }
//    }.toList
//  //
//  //    q"""
//  //      val $wrappedName = $source
//  //      new $targetTpe {
//  //        ..$methodDelegations
//  //      }
//  //     """
//
//}