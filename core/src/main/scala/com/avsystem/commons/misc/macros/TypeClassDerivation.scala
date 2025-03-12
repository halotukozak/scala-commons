package com.avsystem.commons
package misc.macros

import derivation.DeferredInstance
import meta.OptionLike

import scala.compiletime.summonInline
import scala.quoted.*

trait TypeClassDerivation[TC[_]] extends HasMacroUtils {

  /**
   * Returns tree that instantiates a "deferred instance" of this type class. Deferred instance is a special
   * implementation of the type class which implements the `com.avsystem.commons.derivation.DeferredInstance` trait and
   * wraps an another, actual instance of the type class and delegates all operations to that wrapped instance. The
   * wrapped instance itself is supplied later, by assigning a var available on the deferred instance.
   *
   * This is all necessary to handle automatic derivation for recursively-defined types like:
   * {{{
   *   case class Tree(children: List[Tree])
   * }}}
   *
   * EXAMPLE:
   *
   * Let's assume a type class `Traverser` defined like this:
   * {{{
   *   trait Traverser[T] {
   *     def traverse(value: T): Unit
   *   }
   *   object Traverser {
   *     class Deferred[T] extends DeferredInstance[Traverser[T]] with Traverser[T] {
   *       def traverse(value: T) = underlying.traverse(value)
   *     }
   *
   *     implicit def forList[T](implicit forElement: Traverser[T]): Traverser[List[T]] =
   *       new Traverser[List[T]] {
   *         def traverse(value: List[T]) = value.foreach(forElement.traverse)
   *       }
   *   }
   * }}}
   *
   * Automatically derived type class instance for `Tree` would then look somewhat like this:
   *
   * {{{
   *   val tcTree: Traverser[Tree] = {
   *     val deferred: DeferredInstance[Traverser[Tree]] with Traverser[Tree] = new Traverser.Deferred[T]
   *     deferred.underlying = new Traverser[Tree] {
   *       val forChildren = Traverser.forList[Tree](deferred)
   *       def traverse(value: Tree) = value.children.foreach(forChildren.traverse)
   *     }
   *     deferred.underlying
   *   }
   * }}}
   */
  def implementDeferredInstance[T: Type](using Quotes): Expr[DeferredInstance[TC[T]] & TC[T]]

  def allowOptionalParams: Boolean = false

  /**
   * Contains metadata extracted from one of the case subtypes in a sealed hierarchy.
   *
   * @param tpe
   *   the case subtype itself
   * @param instance
   *   tree that evaluates to type class instance for this subtype
   */
  case class KnownSubtype[T](idx: Int, tpe: Type[TC[T]], instance: Expr[TC[T]])

  /**
   * Derives type class instance for singleton type (i.e. an `object` or `this`)
   *
   * @param tpe
   *   the singleton type
   * @param singleValueTree
   *   a tree that evaluates to the sole value of the singleton type
   */
  def forSingleton[T: Type](singleValue: Expr[T])(using Quotes): Expr[TC[T]]

  /**
   * Derives type class instance for record type. Record type is a class/trait whose companion object has matching
   * `apply` and `unapply` methods. In particular, every case class is a proper record type.
   *
   * @param applyUnapply
   *   info about case class or case class like type
   * @param params
   *   metadata for parameters of `apply` method
   */
  def forApplyUnapply[T: Type](applyUnapply: ApplyUnapply[T])(using Quotes): Expr[TC[T]]

  /**
   * Derives type class instance for union type (sealed hierarchy in which every non-abstract subtype has the type class
   * instance of its own or can also be automatically derived).
   *
   * @param tpe
   *   type of the sealed class/trait
   * @param subtypes
   *   metadata for all direct non-abstract subtypes of this sealed class/trait
   */
  def forSealedHierarchy[T: Type](subtypes: List[KnownSubtype[?]])(using Quotes): Expr[TC[T]]

  /**
   * Derives type class instance for arbitrary type which is neither a singleton, record nor union type. Usually, you
   * want to throw a `TypecheckException` to indicate that type class instance cannot be derived for this type. You can
   * use [[typecheckException]] method for this.
   */
  def forUnknown[T: Type](using Quotes): Expr[TC[T]]

  def typeClassInstance[T: Type](using Quotes): Type[TC[T]]

  def dependencyType[T: Type](using Quotes): Type[TC[T]] = typeClassInstance[T]

  def getOptionLike[T: Type](using quotes: Quotes)(sym: quotes.reflect.Symbol): Option[Expr[OptionLike[T]]] =
    import quotes.reflect.*
    Option.when(allowOptionalParams && sym.isOptionalParam) {
      Implicits.search(TypeRepr.of[OptionLike[T]]) match
        case success: ImplicitSearchSuccess => success.tree.asExprOf[OptionLike[T]]
        case _ =>
          report.errorAndAbort(
            s"Could not find implicit instance of OptionLike for ${sym.actualParamType}",
            sym.pos.get,
          )
    }

  def getNonOptionalType[T: Type](using
    quotes: Quotes,
  )(sym: quotes.reflect.Symbol, optionLike: Option[Expr[OptionLike[T]]]): quotes.reflect.TypeRepr =
    import quotes.reflect.*
    optionLike.fold(TypeRepr.of[T]) { ol =>
      val valueMember = ol.asTerm.symbol.typeMember("Value")
      if valueMember.isAbstract then
        report.errorAndAbort(
          "could not determine actual value of optional parameter type;" +
            "optional parameters must be typed as Option/Opt/OptArg etc.",
          sym.pos.get,
        )
      else valueMember.typeRef
    }

  def dependency[T: Type](using
    quotes: Quotes,
    tpe: Type[TC],
  )(depTpe: quotes.reflect.TypeRepr, param: Symbol): Expr[TC[T]] = {
    import quotes.reflect.*
    val tcTpe = TypeRepr.of[TC].appliedTo(depTpe)
    Implicits.search(tcTpe) match
      case success: ImplicitSearchSuccess => success.tree.asExprOf[TC[T]]
      case _ =>
        report.errorAndAbort(s"Cannot materialize ${tcTpe.show} because of problem with parameter ${param.name}:\n")
  }

  private def materializeFor[T: Type](using quotes: Quotes, tpe: Type[TC]): Expr[TC[T]] = {
    import quotes.reflect.*
    def singleTypeTc: Option[Expr[TC[T]]] =
      singleValueForImpl[T] match
        case '{ Some($singleValue: T) } => Some(forSingleton[T](singleValue))
        case '{ None } => None

    def applyUnapplyTc: Option[Expr[TC[T]]] =
      applyUnapplyForImpl[T].map(forApplyUnapply)

    def sealedHierarchyTc: Option[Expr[TC[T]]] = knownSubtypes[T]().map {
      case Nil => report.errorAndAbort(s"Could not find any subtypes for ${Type.show[T]}")
      case subtypes =>
        val dependencies = subtypes.zipWithIndex.map { case (depTpe, idx) =>
          depTpe match
            case '[sub] =>
              val depTree = materializeImpl[sub]
              KnownSubtype[sub](idx, Type.of[TC[sub]], depTree)
        }
        forSealedHierarchy[T](dependencies)
    }

    singleTypeTc orElse applyUnapplyTc orElse sealedHierarchyTc getOrElse forUnknown
  }

  private def withRecursiveImplicitGuard[T: Type](using quotes: Quotes, tpe: Type[TC])(
    unguarded: Expr[TC[T]],
  ): Expr[TC[T]] = RecursiveImplicitMarker.mark[TC[T]] {
    case true =>
      '{
        given deferred: (DeferredInstance[TC[T]] & TC[T]) = ${ implementDeferredInstance[T] }
        val underlying = $unguarded
        deferred.underlying = underlying
        underlying
      }
    case false => unguarded
  }

  def materializeImpl[T: Type](using Quotes, Type[TC]): Expr[TC[T]] =
    withRecursiveImplicitGuard[T](materializeFor[T])
}
