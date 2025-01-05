package com.avsystem.commons
package annotation.macros

import annotation.AnnotationAggregate

import scala.quoted.{Expr, Quotes}

def aggregatedAnnots(using quotes: Quotes): Expr[List[StaticAnnotation]] = '{
  ???
}


//{
//  import quotes.utils.*
//
//  val aggregatedMethod: Symbol = Symbol.spliceOwner.owner
//  if !aggregatedMethod.allOverriddenSymbols.contains(TypeRepr.of[AnnotationAggregate].classSymbol.get.singleMethodMember("aggregated")) then
//    report.errorAndAbort("reifyAggregated macro must only be used to implement AnnotationAggregate.aggregated method")
//
//  if aggregatedMethod.flags.is(Flags.FieldAccessor) || !aggregatedMethod.flags.is(Flags.Final) then
//    report.errorAndAbort("AnnotationAggregate.aggregated method implemented with reifyAggregated macro must be a final def")
//
//  val origin = rawAnnotations(aggregatedMethod)
//  val annotTrees = origin.filter(_.tpe <:< TypeRepr.of[StaticAnnotation])
//
//  assert(origin == annotTrees)
//  if annotTrees.isEmpty then
//    report.warning("no aggregated annotations found on enclosing method")
//
//  Expr.ofList(annotTrees.map(_.asExprOf[StaticAnnotation]))
//}
