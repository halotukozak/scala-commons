package com.avsystem.commons
package annotation

import scala.quoted.Expr
import scala.quoted.Quotes
import com.avsystem.commons.annotation.AnnotationAggregate.reifyAggregatedImpl
import java.lang.annotation.Annotation
import macros._

/** Base trait for annotations which aggregate multiple other annotations. This way annotation aggregates work like
  * "annotation functions" - they are annotations that yield more annotations.
  *
  * In order to specify aggregated annotations, the class that extends `AnnotationAggregate` must implement the
  * `aggregated` method using `reifyAggregated` macro. Implementation must be `final`. Aggregated annotations themselves
  * must then be applied on that implementation method itself. `reifyAggregated` macro will copy them from signature
  * into implementation so that they can be accessed both in compile time and in runtime.
  *
  * {{{
  *   import scala.annotation._
  *   import com.avsystem.commons.serialization._
  *
  *   class mongoId extends AnnotationAggregate {
  *     @name("_id") @outOfOrder
  *     final def aggregated: List[StaticAnnotation] = reifyAggregated
  *   }
  *
  *   case class SomeMongoEntity(@mongoId id: String, data: String)
  * }}}
  *
  * In the above example, applying `@mongoId` annotation on the `id` field has the same effect as if annotations
  * `@name("_id") @outOfOrder` were applied directly on that field.
  *
  * NOTE: thanks to the fact that aggregated annotations are applied on a method inside the aggregate you can forward
  * arguments of the aggregate to each aggregated annotation, e.g.
  *
  * {{{
  *   class rpcNameWithDescription(name: String, description: String) extends AnnotationAggregate {
  *     @rpcName(name) // passing `name` to aggregated annotation
  *     final def aggregated: List[StaticAnnotation] = reifyAggregated
  *   }
  * }}}
  */
trait AnnotationAggregate extends StaticAnnotation {

  /** Returns aggregated annotations. Annotations themselves should be applied on implementation of this method and it
    * should be implemented with [[reifyAggregated]] macro which will extract them from signature into implementation.
    * This way aggregated annotations can be accessed both in compile time and in runtime.
    *
    * {{{
    *   class enclosingAnnot(param: String) extends AnnotationAggregate {
    *     @firstAnnot(param) @secondAnnot
    *     final def aggregated: List[StaticAnnotation] = reifyAggregated
    *   }
    * }}}
    */
  def aggregated: List[StaticAnnotation]

  protected inline def reifyAggregated: List[StaticAnnotation] = AnnotationAggregate.reifyAggregated
}

object AnnotationAggregate {
  private inline def reifyAggregated: List[StaticAnnotation] = ${ reifyAggregatedImpl }

  private def reifyAggregatedImpl(using quotes: Quotes): Expr[List[StaticAnnotation]] = {
    import quotes.reflect.*

    val invokingMethod = Symbol.spliceOwner.owner

    val methodSymbol =
      TypeRepr
        .of[AnnotationAggregate]
        .typeSymbol
        .methodMembers
        .find(_.name == "aggregated")
        .getOrElse { report.errorAndAbort("AnnotaztionAggregate.aggregated method not found") }

    if (!invokingMethod.allOverriddenSymbols.contains(methodSymbol)) {
      report.errorAndAbort("reifyAggregated macro must only be used to implement AnnotationAggregate.aggregated method")
    }

    if (methodSymbol.flags.is(Flags.FieldAccessor) || methodSymbol.flags.is(Flags.Final)) {
      report.errorAndAbort(
        "AnnotationAggregate.aggregated method implemented with reifyAggregated macro must be a final def"
      )
    }

    val annotTrees = invokingMethod.annotations
      .filter(_.tpe <:< TypeRepr.of[StaticAnnotation])
      .map(_.asExprOf[StaticAnnotation])

    if (annotTrees.isEmpty) {
      report.warning("no aggregated annotations found on enclosing method")
    }

    Expr.ofList(annotTrees)
  }
}
