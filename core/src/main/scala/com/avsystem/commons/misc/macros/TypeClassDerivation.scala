package com.avsystem.commons
package misc.macros

import meta.OptionLike

import com.avsystem.commons.derivation.DeferredInstance

import scala.compiletime.summonInline
import scala.deriving.Mirror
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

//  /**
//   * Contains metadata extracted from `apply` method of companion object of some record (case-class like) type.
//   *
//   * @param sym
//   * symbol of the `apply` method parameter or case class constructor parameter (if `apply` is auto-generated for case
//   * class companion object)
//   * @param defaultValue
//   * tree that evaluates to default value of the `apply` parameter or `EmptyTree`
//   * @param instance
//   * tree that evaluates to type class instance for type of this parameter
//   * @param optionLike
//   * if the parameter is annotated as optional, an instance of `OptionLike` for its type
//   */
//  case class ApplyParam[T](
//    idx: Int,
//    sym: Symbol,
//    defaultValue: Option[Expr[TC[T]]],
//    instance: Expr[TC[T]],
//    optionLike: Option[? /*CachedImplicit*/ ],
//  ) {
//    val repeated: Boolean = sym.isRepeatedParam
//
//    def valueType = sym.actualParamType
//
//    def asArgument(tree: Tree): Tree = ???
//    //      if (repeated) '{ $tree _: *} else tree
//  }

  /**
   * Contains metadata extracted from one of the case subtypes in a sealed hierarchy.
   *
   * @param tpe
   *   the case subtype itself
   * @param instance
   *   tree that evaluates to type class instance for this subtype
   */
  case class KnownSubtype[T](idx: Int, tpe: Type[T], instance: Expr[TC[T]]) {
    //    def sym: Symbol = TypeRepr.of(using tpe).typeSymbol
  }

  /**
   * Derives type class instance for singleton type (i.e. an `object` or `this`)
   *
   * @param tpe
   *   the singleton type
   * @param singleValueTree
   *   a tree that evaluates to the sole value of the singleton type
   */
  def forSingleton[T: Type](singleValue: Expr[ValueOf[T]])(using Quotes): Expr[TC[T]]

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
  def forSealedHierarchy[T: Type](subtypes: List[KnownSubtype[T]])(using Quotes): Expr[TC[T]]

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

//  def applyParams[T: Type](au: ApplyUnapply[T]): List[ApplyUnapply.Param] = au.params.zipWithIndex.map { case (s, idx) =>
//    val defaultValue = au.defaultValueFor(s, idx)
//    val paramType = s.actualParamType
//    val optionLike = getOptionLike(s)
//    val nonOptionalType = getNonOptionalType(s, optionLike)
//    ApplyParam(idx, s, defaultValue, dependency[T](nonOptionalType, s), optionLike)
//  }

  def materializeFor[T: Type](using quotes: Quotes, tpe: Type[TC]): Expr[TC[T]] = {
    import quotes.reflect.*
    def singleTypeTc: Option[Expr[TC[T]]] =
      singleValueForImpl[T] match
        case '{ Some($singleValue: ValueOf[T]) } => Some(forSingleton[T](singleValue))
        case _ => None

    def applyUnapplyTc: Option[Expr[TC[T]]] =
      applyUnapplyFor[T].map(forApplyUnapply)

    def sealedHierarchyTc: Option[Expr[TC[T]]] = knownSubtypes[T]().map {
      case Nil => report.errorAndAbort(s"Could not find any subtypes for ${Type.show[T]}")
      case subtypes =>
        val dependencies = subtypes.zipWithIndex.map { case (depTpe, idx) =>
          val depTree = Implicits.search(TypeRepr.of(using dependencyType(using depTpe))) match
            case failure: ImplicitSearchFailure => materializeImpl[T]
            case success: ImplicitSearchSuccess => success.tree.asExprOf[TC[T]]
          KnownSubtype[T](idx, depTpe, depTree)
        }
        forSealedHierarchy[T](dependencies)
    }

    singleTypeTc orElse applyUnapplyTc orElse sealedHierarchyTc getOrElse forUnknown[T]
  }

  def withRecursiveImplicitGuard[T: Type](using quotes: Quotes, tpe: Type[TC])(unguarded: Expr[TC[T]]): Expr[TC[T]] = {
    import quotes.reflect.*
    val dtpe = TypeRepr.of[T].dealias
    val tcTpe = typeClassInstance[T]

    val withDummyImplicit =
      '{
        given (DeferredInstance[TC[T]] & TC[T]) = ???
        $unguarded
      }

    def guarded: Expr[TC[T]] = '{
      given deferred: (DeferredInstance[TC[T]] & TC[T]) = ${ implementDeferredInstance[T] }
      val underlying = $unguarded
      deferred.underlying = underlying
      underlying
    }

    withDummyImplicit.asTerm match
      case Block(deferredDef :: Nil, typedUnguarded)
          if !typedUnguarded.symbol.children.exists(_ == deferredDef.symbol) => {
        printTypeReprInfo(typedUnguarded.tpe)
        typedUnguarded.asExprOf[TC[T]]
      }
      case _ => guarded
  }

  def materializeImpl[T: Type](using Quotes, Type[TC]): Expr[TC[T]] = withRecursiveImplicitGuard[T](materializeFor[T])

//  def materializeImplicitly[T: Type](allow: Tree): Expr[TC[T]] = materialize[T]
}
