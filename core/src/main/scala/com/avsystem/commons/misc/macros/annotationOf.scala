package com.avsystem.commons
package misc.macros

import misc.{AnnotationOf, SelfAnnotations}

import scala.quoted.*

def annotationOf[A: Type, T: Type](using quotes: Quotes): Expr[AnnotationOf[A, T]] = {
  import quotes.reflect.*
  val tSym: Symbol =
    TypeRepr.of[T].classSymbol getOrElse report.errorAndAbort(s"Type ${TypeRepr.of[T].show} is not a class")
  val annot: Expr[A] = tSym.annotations
    .find(_.tpe <:< TypeRepr.of[A])
    .getOrElse(report.errorAndAbort(s"No annotation of type ${TypeRepr.of[A].show} found on $tSym"))
    .asExprOf[A]

  '{ new AnnotationOf[A, T]($annot) }
}

def selfAnnotations[A: Type](using quotes: Quotes): Expr[SelfAnnotations[A]] = {
  import quotes.reflect.*
  val sym: Symbol = {
    val ownerConstr = Symbol.spliceOwner.owner
    if !ownerConstr.isClassConstructor then
      report.errorAndAbort(s"`selfAnnotations` can only be used as super constructor argument")
    else ownerConstr
  }
  val annots = sym.annotations.map { (annot: Term) =>
    if annot.tpe.classSymbol.get.containsInaccessibleThises then {
      report.errorAndAbort(s"Reified annotation ${annot.show} contains inaccessible this-references", annot.pos)
    }
    annot.asExprOf[A]
  }
  '{ new SelfAnnotations[A](${ Expr.ofList(annots) }) }
}
