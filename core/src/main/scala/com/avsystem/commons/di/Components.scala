package com.avsystem.commons
package di

import di.macros.ComponentMacros
import di.macros.ComponentMacros.SingletonCache
import misc.SourceInfo

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import scala.quoted.Type

/**
 * Base trait for classes that define collections of interdependent [[Component]]s.
 */

trait Components extends ComponentsLowPrio {
  //  inline implicit def autoComponent[T](definition: => T)(using si: SourceInfo): AutoComponent[T] = ${ ComponentMacros.autoComponent[T]('{ definition }, '{ componentInfo(si) }) }

  private lazy val singletonsCache = new SingletonCache

  // avoids divergent implicit expansion involving `inject`
  // this is not strictly necessary but makes compiler error messages nicer
  // i.e. the compiler will emit "could not find givenue" instead of "divergent implicit expansion"
//  given ambiguousArbitraryComponent1[T]: Component[T] = null
//
//  given ambiguousArbitraryComponent2[T]: Component[T] = null

  protected def componentNamePrefix: String = ""

  protected def componentInfo(sourceInfo: SourceInfo): ComponentInfo = ComponentInfo(componentNamePrefix, sourceInfo)

  /**
   * Creates a [[Component]] based on a definition (i.e. a constructor invocation). The definition may refer to other
   * components as dependencies using `.ref`. This macro will transform the definition by extracting dependencies in a
   * way that allows them to be initialized in parallel, before initializing the current component itself.
   */
  //  protected inline def component[T](definition: => T)(using si: SourceInfo): Component[T] = ${ ComponentMacros.component[T]('{ definition }, '{ componentInfo(si) }, '{ Option(singletonsCache) }) }
  protected inline def component[T](inline definition: => T)(using si: SourceInfo): Component[T] =
    ${ ComponentMacros.mkComponent[T]('{ definition }, '{ componentInfo(si) }, '{ None }, async = false) }

  /**
   * Asynchronous version of [[component]] macro.
   */
  protected inline def asyncComponent[T](definition: ExecutionContext => Future[T])(using
    si: SourceInfo,
  ): Component[T] = ???
  //  ${ ComponentMacros.asyncComponent[T]('{ definition }, '{ componentInfo(si) }) }

  /**
   * This is the same as [[component]] except that the created [[Component]] is cached inside an outer instance that
   * implements [[Components]]. This way you can implement your components using `def`s rather than `val`s (`val`s can
   * be problematic in traits) but caching will make sure that your `def` always returns the same, cached [[Component]]
   * instance. The cache key is based on source position so overriding a method that returns `singleton` will create
   * separate [[Component]] with different cache key.
   */
  protected inline def singleton[T](inline definition: => T)(using si: SourceInfo): Component[T] =
    ${
      ComponentMacros.mkComponent[T]('{ definition }, '{ componentInfo(si) }, '{ Some(singletonsCache) }, async = false)
    }

  /**
   * Asynchronous version of [[singleton]] macro.
   */
  protected inline def asyncSingleton[T](definition: ExecutionContext => Future[T])(using
    si: SourceInfo,
  ): Component[T] = ???
  //  ${ ComponentMacros.asyncSingleton[T]('{ definition }, '{ componentInfo(si) }) }

  //  protected def reifyAllSingletons: List[Component[?]] = ComponentMacros.reifyAllSingletons

  protected def optEmptyComponent: Component[Opt[Nothing]] =
    singleton(Opt.Empty)

  protected def noneComponent: Component[Option[Nothing]] =
    singleton(None)

  //  protected def sequenceOpt[T](componentOpt: Opt[Component[T]]): Component[Opt[T]] =
  //    componentOpt.mapOr(optEmptyComponent, c => component(c.ref.opt))
  //
  //  protected def sequenceOption[T](componentOpt: Option[Component[T]]): Component[Option[T]] =
  //    componentOpt.mapOr(noneComponent, c => component(c.ref.option))
}

trait ComponentsLowPrio {
  //  @compileTimeOnly("implicit Component[T] => implicit T inference only works inside code passed to component/singleton macro")
  //  given inject[T]: Conversion[Component[T], T] = sys.error("stub")
  implicit def inject[T](using Component[T]): T = sys.error("stub")
}
