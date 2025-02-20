//package com.avsystem.commons
//package misc.macros
//
//  def infer[T: Type](quotes: Quotes): Tree =
//    (inferTpe(weakTypeOf[T], "", NoPosition, withMacrosDisabled = false))
//
////  def clueInfer[T: WeakTypeTag](clue: Tree): Tree =
////    (inferTpe(weakTypeOf[T], clueStr(clue), clue.pos, withMacrosDisabled = false))
////
////  def inferNonMacro[T: WeakTypeTag](clue: Tree): Tree =
////    (inferTpe(weakTypeOf[T], clueStr(clue), clue.pos, withMacrosDisabled = true))
////
////  private def clueStr(clue: Tree): String = clue match {
////    case StringLiteral(str) => str
////    case _ => abort(s"clue must be a String literal, $clue is not")
////  }
////
//  private def inferTpe[T :Type](using quotes: Quotes)( clue: String, pos: quotes.reflect.Position, withMacrosDisabled: Boolean): Tree =
//    inferImplicitValue[T]
//
//  def inferImplicitValue[T:Type](using quotes : Quotes): Expr[T] = {
//    import quotes.reflect.*
//
//    Implicits.search(TypeRepr.of[T]) match
//      case result: ImplicitSearchSuccess =>
//        result.tree.asExprOf[T]
//      case _=>
//        ???
////        implicitNotFoundMsg(tpe)
//    }
