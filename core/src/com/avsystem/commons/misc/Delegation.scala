package com.avsystem.commons
package misc

import scala.quoted._
import macros._

/** A typeclass which witnesses that type `A` can be wrapped into trait or abstract class `B`
  */
trait Delegation[A, B] {
  def delegate(a: A): B
}

object Delegation {
  inline given materializeDelegation[A, B]: Delegation[A, B] = ${ materializeDelegationImpl[A, B] }

  def materializeDelegationImpl[A: Type, B: Type](using Quotes): Expr[Delegation[A, B]] =
    '{ new Delegation[A, B] { def delegate(a: A): B = delegate(a) } }

  private inline def delegate[B](inline source: Any): B = ${ delegateImpl[B]('{ source }) }

  def delegateImpl[B: Type](source: Expr[Any])(using Quotes): Expr[B] = {
    import quotes.reflect._

    return '{ ??? } // todo: some problem with vals

    val targetType = TypeRepr.of[B].widen

    val targetSymbol = targetType.classSymbol
      .filter(sym => sym.flags.is(Flags.Abstract) || sym.flags.is(Flags.Trait))
      .getOrElse {
        report.errorAndAbort(s"${targetType.show} is not a trait or abstract class")
      }

    val methods = targetType.typeSymbol.methodMembers
      .filter(_.flags.is(Flags.Deferred))
      .map {
        case m if m.isPublic && (m.flags.is(Flags.Method)) /* && !m.isSetter */ => m
        case m => report.errorAndAbort(s"Can't delegate ${m.name} - only public defs can be delegated")
      }

    val fields = targetType.typeSymbol.fieldMembers
      .filter(_.flags.is(Flags.Deferred))
      .map {
        case m if m.isPublic => m
        case m => report.errorAndAbort(s"Can't delegate ${m.name} - only public vals can be delegated")
      }

    val methodDefs =
      methods
        .mkMap(
          _.name,
          m =>
            (rhsFn: List[List[Tree]]) =>
              Some {
                val termArgs = m.paramSymss.filter(_.forall(_.isTerm)).map(_.map(_.tree.asInstanceOf[Term]))

                val typeArgs = targetType.memberType(m).typeArgs

                val select = Select.unique(source.asTerm, m.name)

                select
                  .appliedToTypes(typeArgs)
                  .appliedToArgss(termArgs)
              }
        )
        .toMap

    val fieldDefs =
      fields.mkMap(_.name, { f => (rhsFn: List[List[Tree]]) => Some(Select.unique(source.asTerm, f.name)) })

    ClassDef.newAnon[B](methodDefs ++ fieldDefs)
  }

  inline def apply[B](inline source: Any) = delegate[B](source)
}
