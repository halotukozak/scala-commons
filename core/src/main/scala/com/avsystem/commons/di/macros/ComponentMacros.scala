package com.avsystem.commons
package di.macros

import di.{Component, ComponentInfo, Components}

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import scala.collection.mutable.ListBuffer
import scala.quoted.*

object ComponentMacros extends MacroUtils {
  type SingletonCache = ConcurrentHashMap[ComponentInfo, AtomicReference[Future[?]]]

  //  def component[T: Type](definition: Expr[T], componentInfo: Expr[ComponentInfo], singletonCache: Expr[Option[SingletonCache]])(using Quotes): Expr[Component[T]] =
  //    mkComponent[T](definition, componentInfo, singletonCache, async = false)

  //  def asyncComponent[T: Type](definition: Expr[T], componentInfo: Expr[ComponentInfo])(using quotes: Quotes): Expr[Component[T]] = {
  //    ensureRangePositions()
  //    mkComponent[T](definition, componentInfo, singleton = false, async = true)
  //  }
  //
  //  def asyncSingleton[T: Type](definition: Expr[T], componentInfo: Expr[ComponentInfo])(using quotes: Quotes): Expr[Component[T]] = {
  //    ensureRangePositions()
  //    mkComponent[T](definition, componentInfo, singleton = true, async = true)
  //  }

  //  def autoComponent[T: Type](definition: Expr[T], componentInfo: Expr[ComponentInfo])(using Quotes): Expr[AutoComponent[T]] =
  //    val component = mkComponent[T](definition, componentInfo, Expr(None), async = false)
  //    '{ AutoComponent($component) }

  //  def reifyAllSingletons(using quotes: Quotes): Tree = {
  //    val prefixName = c.freshName(TermName("prefix"))
  //    val bufName = c.freshName(TermName("buf"))
  //
  //    val componentMethods =
  //      c.prefix.actualType.members.iterator
  //        .filter(s => s.isMethod && !s.isSynthetic).map(_.asMethod)
  //        .filter { m =>
  //          m.typeParams.isEmpty && m.paramLists.isEmpty &&
  //            m.typeSignatureIn(c.prefix.actualType).resultType <:< ComponentTpe
  //        }
  //        .toList
  //
  //    q"""
  //         val $prefixName = ${c.prefix}
  //         val $bufName = new $CollectionPkg.mutable.ListBuffer[$ComponentTpe]
  //         def addIfCached(_c: $ComponentTpe): Unit =
  //           if(_c.isCached) $bufName += _c
  //         ..${componentMethods.map(m => q"addIfCached($prefixName.$m)")}
  //         $bufName.result()
  //         """
  //  }

  def mkComponent[T: Type](definition: Expr[T], componentInfo: Expr[ComponentInfo], singletonsCache: Expr[Option[SingletonCache]], async: Boolean)(using quotes: Quotes): Expr[Component[T]] = {
    import quotes.reflect.*
    object ComponentRef {
      lazy val ComponentRefSym: Symbol = TypeRepr.of[Component].classSymbol.get.singleMethodMember("ref")
      lazy val InjectSym: Symbol = TypeRepr.of[Components].classSymbol.get.fieldMember("inject")

      def unapply(tree: Term): Option[(Term, TypeRepr)] = tree match
        case Select(component, "ref") if tree.symbol == ComponentRefSym =>
          Some((component, tree.tpe))
        case Apply(conversion, List(component)) if conversion.symbol == InjectSym =>
          Some((component, tree.tpe))
        case _ =>
          None
    }

    object LocalSymbolsCollector extends TreeTraverser {
      private val symsBuilder = Set.newBuilder[Symbol]

      def symbolsFound: Set[Symbol] = symsBuilder.result()

      override def traverseTree(tree: Tree)(owner: Symbol): Unit = tree match
        case ComponentRef(_) => // stop
        case t@(_: Definition | Lambda(_) | _: Bind) if t.symbol != Symbol.noSymbol =>
          symsBuilder += t.symbol
          super.traverseTree(tree)(owner)
        case _ =>
          super.traverseTree(tree)(owner)
    }

    LocalSymbolsCollector.traverseTree(definition.asTerm)(Symbol.noSymbol)
    val componentDefLocals = LocalSymbolsCollector.symbolsFound

    object DependencyValidator extends TreeTraverser:
      override def traverseTree(tree: Tree)(owner: Symbol): Unit = tree match
        case t@ComponentRef(_) =>
          report.errorAndAbort(s"illegal nested component reference inside expression representing component dependency", t.pos)
        case t if t.symbol != Symbol.noSymbol && componentDefLocals.contains(t.symbol) =>
          report.errorAndAbort(s"illegal local value or method reference inside expression representing component dependency", t.pos)
        case _ =>

    DependencyValidator.traverseTree(definition.asTerm)(Symbol.noSymbol)
    final class DependencyExtractor(depArray: Expr[IndexedSeq[Any]])(depsBuf: Expr[ListBuffer[Component[?]]]) extends TreeMap {
      lazy val ComponentInfoSym: Symbol = TypeRepr.of[ComponentInfo.type].classSymbol.get.fieldMember("info")

      override def transformTerm(term: Term)(owner: Symbol): Term = term match
        case ComponentRef((component, componentTpe)) =>
          DependencyValidator.traverseTree(component)(component.symbol.owner)
          componentTpe.asType match
            case '[t] => '{
              val dep = ${ component.asExprOf[Component[?]] }
              $depsBuf += dep
              $depArray($depsBuf.size - 1).asInstanceOf[t]
            }.asTerm
        case t if t.symbol == ComponentInfoSym =>
          componentInfo.asTerm
        case _ =>
          super.transformTerm(term)(owner)
    }

    def asyncDefinition(depArray: Expr[IndexedSeq[Any]])(depsBuf: Expr[ListBuffer[Component[?]]])(using Quotes): Expr[ExecutionContext => Future[T]] = {
      val transformedDefinition = DependencyExtractor(depArray)(depsBuf).transformTree(definition.asTerm)(depArray.asTerm.symbol.owner).asExprOf[T]
      if async then '{ _ => Future.successful($transformedDefinition) }
      else '{ Component.async($transformedDefinition) }
    }

    val result = '{
      val depsBuf = new ListBuffer[Component[?]]

      new Component[T](
        $componentInfo,
        IndexedSeq(depsBuf.result() *),
        (depArray: IndexedSeq[Any]) => ${ asyncDefinition('{ depArray })('{ depsBuf }) },
      )
    }

    singletonsCache match
      case '{ None } => result
      case _ => '{
        val res = $result
        $singletonsCache match
          case Some(singletonsCache) =>
            val cacheStorage = singletonsCache
              .computeIfAbsent($componentInfo, _ => new AtomicReference)
              .asInstanceOf[AtomicReference[Future[T]]]
            res.cached(cacheStorage, $componentInfo)
          case _ => res
      }
  }
}
