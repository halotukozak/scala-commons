// package com.avsystem.commons
// package macros

// import scala.quoted.*
// import derivation.*

// trait TypeClassDerivation[TC[_]] {

//   /** Returns tree that instantiates a "deferred instance" of this type class. Deferred instance is a special
//     * implementation of the type class which implements the `com.avsystem.commons.derivation.DeferredInstance` trait and
//     * wraps an another, actual instance of the type class and delegates all operations to that wrapped instance. The
//     * wrapped instance itself is supplied later, by assigning a var available on the deferred instance.
//     *
//     * This is all necessary to handle automatic derivation for recursively-defined types like:
//     * {{{
//     *   case class Tree(children: List[Tree])
//     * }}}
//     *
//     * EXAMPLE:
//     *
//     * Let's assume a type class `Traverser` defined like this:
//     * {{{
//     *   trait Traverser[T] {
//     *     def traverse(value: T): Unit
//     *   }
//     *   object Traverser {
//     *     class Deferred[T] extends DeferredInstance[Traverser[T]] with Traverser[T] {
//     *       def traverse(value: T) = underlying.traverse(value)
//     *     }
//     *
//     *     implicit def forList[T](implicit forElement: Traverser[T]): Traverser[List[T]] =
//     *       new Traverser[List[T]] {
//     *         def traverse(value: List[T]) = value.foreach(forElement.traverse)
//     *       }
//     *   }
//     * }}}
//     *
//     * Automatically derived type class instance for `Tree` would then look somewhat like this:
//     *
//     * {{{
//     *   val tcTree: Traverser[Tree] = {
//     *     val deferred: DeferredInstance[Traverser[Tree]] with Traverser[Tree] = new Traverser.Deferred[T]
//     *     deferred.underlying = new Traverser[Tree] {
//     *       val forChildren = Traverser.forList[Tree](deferred)
//     *       def traverse(value: Tree) = value.children.foreach(forChildren.traverse)
//     *     }
//     *     deferred.underlying
//     *   }
//     * }}}
//     */
//   def implementDeferredInstance[T: Type]: Expr[DeferredInstance[TC[T]] & TC[T]]

//   def allowOptionalParams: Boolean = false

//   /** Contains metadata extracted from `apply` method of companion object of some record (case-class like) type.
//     *
//     * @param sym
//     *   symbol of the `apply` method parameter or case class constructor parameter (if `apply` is auto-generated for
//     *   case class companion object)
//     * @param defaultValue
//     *   tree that evaluates to default value of the `apply` parameter or `EmptyTree`
//     * @param instance
//     *   tree that evaluates to type class instance for type of this parameter
//     * @param optionLike
//     *   if the parameter is annotated as optional, an instance of `OptionLike` for its type
//     */
//   case class ApplyParam(
//     idx: Int,
//     sym: Symbol,
//     defaultValue: Expr[?],
//     instance: Expr[?],
//     optionLike: Option[
//       /** CachedImplicit */
//       ?
//     ]
//   ) {
//     val repeated: Boolean = ??? // isRepeated(sym)
//     def valueType: Type[?] = ??? // actualParamType(sym)
//     // def asArgument(tree: Tree): Tree = if (repeated) q"$tree: _*" else tree
//   }

//   /** Contains metadata extracted from one of the case subtypes in a sealed hierarchy.
//     *
//     * @param tpe
//     *   the case subtype itself
//     * @param instance
//     *   tree that evaluates to type class instance for this subtype
//     */
//   case class KnownSubtype(idx: Int, tpe: Type[?], instance: Expr[?]) {
//     // def sym: Symbol = tpe.typeSymbol
//   }

//   /** Derives type class instance for singleton type (i.e. an `object` or `this`)
//     *
//     * @param tpe
//     *   the singleton type
//     * @param singleValueTree
//     *   a tree that evaluates to the sole value of the singleton type
//     */
//   def forSingleton[T: Type](singleValueTree: Expr[T]): Expr[TC[T]]

//   /** Derives type class instance for record type. Record type is a class/trait whose companion object has matching
//     * `apply` and `unapply` methods. In particular, every case class is a proper record type.
//     *
//     * @param applyUnapply
//     *   info about case class or case class like type
//     * @param params
//     *   metadata for parameters of `apply` method
//     */
//   def forApplyUnapply(applyUnapply: ApplyUnapply, params: List[ApplyParam]): Expr[?]

//   /** Derives type class instance for union type (sealed hierarchy in which every non-abstract subtype has the type
//     * class instance of its own or can also be automatically derived).
//     *
//     * @param tpe
//     *   type of the sealed class/trait
//     * @param subtypes
//     *   metadata for all direct non-abstract subtypes of this sealed class/trait
//     */
//   def forSealedHierarchy[T: Type](subtypes: List[KnownSubtype]): Expr[?]

//   /** Derives type class instance for arbitrary type which is neither a singleton, record nor union type. Usually, you
//     * want to throw a `TypecheckException` to indicate that type class instance cannot be derived for this type. You can
//     * use [[typecheckException]] method for this.
//     */
//   def forUnknown[T: Type]: Expr[?]

//   def typeClassInstance[T: Type]: Type[?]
//   def dependencyType[T: Type]: Type[?] = typeClassInstance[T]

// //   def getOptionLike(sym: Symbol, tpe: Type[?]): Option[CachedImplicit] =
//   // if (allowOptionalParams && isOptionalParam(sym, tpe))
//   //   Some(
//   //     inferCachedImplicit(
//   //       getType(tq"$CommonsPkg.meta.OptionLike[$tpe]"),
//   //       ErrorCtx("not an option-like type", sym.pos)
//   //     )
//   //   )
//   // else
//   // None

// //   def getNonOptionalType(sym: Symbol, tpe: Type, optionLike: Option[CachedImplicit]): Type =
// //     optionLike.fold(tpe) { ol =>
// //       val optionLikeType = ol.actualType
// //       val valueMember = optionLikeType.member(TypeName("Value"))
// //       if (valueMember.isAbstract)
// //         abortAt(
// //           "could not determine actual value of optional parameter type;" +
// //             "optional parameters must be typed as Option/Opt/OptArg etc.",
// //           sym.pos
// //         )
// //       else
// //         valueMember.typeSignatureIn(optionLikeType)
//   // }

//   def dependency(depTpe: Type[?], tcTpe: Type[?], param: Expr[?]): Expr[?] = {
//     // val clue = s"Cannot materialize $tcTpe because of problem with parameter ${param.name}:\n"
//     // val depTcTpe = dependencyType(depTpe)
//     // q"""$ImplicitsObj.infer[$depTcTpe](${internal.setPos(StringLiteral(clue), param.pos)})"""
//     ???
//   }

//   def applyParams(au: ApplyUnapply, tcTpe: Type[?]): List[ApplyParam] = au.params.zipWithIndex.map { case (s, idx) =>
//     val defaultValue = au.defaultValueFor(s, idx)
//     val paramType = actualParamType(s)
//     val optionLike = getOptionLike(s, paramType)
//     val nonOptionalType = getNonOptionalType(s, paramType, optionLike)
//     ApplyParam(idx, s, defaultValue, dependency(nonOptionalType, tcTpe, s), optionLike)
//   }

//   def materializeFor[T: Type](using Quotes): Expr[TC[T]] = {
//     val dtpe = tpe.dealias
//     val tcTpe = typeClassInstance(dtpe)

//     def singleTypeTc: Option[Expr[TC[T]]] =
//       singleValueFor[T].map(forSingleton[T])

//     def applyUnapplyTc: Option[Expr[TC[T]]] = applyUnapplyFor[T].map { au =>
//       forApplyUnapply(au, applyParams(au, tcTpe))
//     }

//     def sealedHierarchyTc: Option[Expr[TC[T]]] = knownSubtypes(dtpe).map { subtypes =>
//       if (subtypes.isEmpty) {
//         abort(s"Could not find any subtypes for $dtpe")
//       }
//       val dependencies = subtypes.zipWithIndex.map { case (depTpe, idx) =>
//         val depTree = c.inferImplicitValue(dependencyType(depTpe), withMacrosDisabled = true) match {
//           case EmptyTree => q"${c.prefix}.materialize[$depTpe]"
//           case t => t
//         }
//         KnownSubtype(idx, depTpe, depTree)
//       }
//       forSealedHierarchy(dtpe, dependencies)
//     }

//     singleTypeTc orElse applyUnapplyTc orElse sealedHierarchyTc getOrElse forUnknown(dtpe)
//   }

//   def withRecursiveImplicitGuard[T: Type](unguarded: Expr[TC[T]])(using quotes: Quotes): Expr[TC[T]] = {
//     import quotes.reflect.*

//     // deferred instance is necessary to handle recursively defined types
//     // introducing intermediate val to make sure exact type of materialized instance is not lost
//     // registerImplicit(tcTpe, deferredName) todo: ??

//     val withDummyImplicit: Expr[TC[T]] = '{
//       given (DeferredInstance[TC[T]] & TC[T]) = ???
//       $unguarded
//     }

//     def guarded: Expr[TC[T]] = '{
//       given deffered: (DeferredInstance[TC[T]] & TC[T]) = ${ implementDeferredInstance[T] }
//       val underlying = $unguarded
//       deffered.underlying = underlying
//       underlying
//     }

//     withDummyImplicit.asTerm match
//       case Block(List(deferredDef), typedUnguarded)
//           if !typedUnguarded.symbol.children.exists(_ == deferredDef.symbol) =>
//         typedUnguarded.asExprOf[TC[T]]
//       case _ => guarded
//   }

//   def materialize[T: Type](using quotes: Quotes): Expr[TC[T]] =
//     withRecursiveImplicitGuard[T](materializeFor[T])

//   def materializeImplicitly[T: Type](allow: Expr[AllowImplicitMacro[TC[T]]])(using Quotes): Expr[TC[T]] = materialize[T]
// }

// abstract class AbstractTypeClassDerivation extends TypeClassDerivation
