package com.avsystem.commons
package di

import com.avsystem.commons.di.Component.DestroyFunction
import com.avsystem.commons.misc.{GraphUtils, SourceInfo}

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.unchecked.uncheckedVariance as uv

case class ComponentInitializationException(component: Component[?], cause: Throwable)
  extends Exception(s"failed to initialize component ${component.info}", cause)

case class DependencyCycleException(cyclePath: List[Component[?]])
  extends Exception(
    s"component dependency cycle detected:\n${cyclePath.iterator.map(_.info).map("  " + _).mkString(" ->\n")}"
  )

case class ComponentInfo(name: String, filePath: String, fileName: String, lineNumber: Int) {
  override def toString: String = s"$name($fileName:$lineNumber)"
}
object ComponentInfo {
  def apply(namePrefix: String, sourceInfo: SourceInfo): ComponentInfo =
    new ComponentInfo(
      namePrefix + sourceInfo.enclosingSymbols.head,
      sourceInfo.filePath,
      sourceInfo.fileName,
      sourceInfo.line
    )

  //  @compileTimeOnly("implicit ComponentInfo is only available inside code passed to component/singleton macro")
  implicit def info: ComponentInfo = sys.error("stub")
}

/** Represents a lazily initialized component in a dependency injection setting. The name "component" indicates that the
  * value is often an application building block like a database service, data access object, HTTP server etc. which is
  * associated with some side-effectful initialization code. However, [[Component]] can hold values of any type.
  *
  * You can think of [[Component]] as a high-level `lazy val` with more features: parallel initialization of
  * dependencies, dependency cycle detection, source code awareness.
  */
final class Component[+T](
  val info: ComponentInfo,
  deps: => IndexedSeq[Component[?]],
  creator: IndexedSeq[Any] => ExecutionContext ?=> Future[T],
  destroyer: DestroyFunction[T]

  /** = Component.emptyDestroy[T] */
  , // TODO: sth with +T, ?=>
  cachedStorage: Opt[AtomicReference[Future[T]]] = Opt.Empty
) {

  /** Name of the component. Usually this name is inferred from the method name that this component is defined by.
    */
  def name: String = info.name

  def isCached: Boolean = cachedStorage.isDefined

  /** Returns dependencies of this component extracted from the component definition. You can use this to inspect the
    * dependency graph without initializing any components.
    */
  lazy val dependencies: IndexedSeq[Component[?]] = deps

  private val storage: AtomicReference[Future[T @uv]] =
    cachedStorage.getOrElse(new AtomicReference)

  private def sameStorage(otherStorage: AtomicReference[?]): Boolean =
    storage eq otherStorage

  // equality based on storage identity is important for cycle detection with cached components
  override def hashCode(): Int = storage.hashCode()
  override def equals(obj: Any): Boolean = obj match {
    case c: Component[_] => c.sameStorage(storage)
    case _ => false
  }

  /** Phantom method that indicates an asynchronous reference to this component inside definition of some other
    * component. This method is rewritten in compile time by [[Components.component]] or [[Components.singleton]] macro.
    * The component being referred is extracted as a dependency and initialized before the component that refers to it.
    * This way multiple dependencies can be initialized in parallel.
    *
    * @example
    *   {{{
    *   class FooService
    *   class BarService
    *   class Application(foo: FooService, bar: BarService)
    *
    *   object MyComponents extends Components {
    *     def foo: Component[FooService] = singleton(new FooService)
    *     def bar: Component[BarService] = singleton(new BarService)
    *
    *     // before `app` is initialized, `foo` and `bar` can be initialized in parallel
    *     def app: Component[Application] = singleton(new Application(foo.ref, bar.ref))
    *   }
    *   }}}
    */
  //  @compileTimeOnly(".ref can only be used inside code passed to component/singleton(...) macro")
  def ref: T = sys.error("stub")

  /** Returns the initialized instance of this component, if it was already initialized.
    */
  def getIfReady: Option[T] =
    storage.get.option.flatMap(_.value.map(_.get))

  /** Forces a dependency on another component or components.
    */
  def dependsOn(moreDeps: Component[?]*): Component[T] =
    new Component(info, deps ++ moreDeps, creator, destroyer, cachedStorage)

  /** Specifies an asynchronous function that will be used to destroy this component, i.e. free up any resources that
    * this component allocated (threads, network connections, etc). See [[destroy]].
    */
  def asyncDestroyWith(destroyFun: DestroyFunction[T]): Component[T] = {
    val newDestroyer: DestroyFunction[T] =
      t => destroyer(t).flatMap(_ => destroyFun(t))
    new Component(info, deps, creator, newDestroyer, cachedStorage)
  }

  /** Specifies a function that will be used to destroy this component, i.e. free up any resources that this component
    * allocated (threads, network connections, etc). See [[destroy]].
    */
  def destroyWith(destroyFun: T => Unit): Component[T] =
    asyncDestroyWith(t => Future(destroyFun(t)))

  private[di] def cached(cachedStorage: AtomicReference[Future[T @uv]], info: ComponentInfo): Component[T] =
    new Component(info, deps, creator, destroyer, Opt(cachedStorage))

  /** Validates this component by checking its dependency graph for cycles. A [[DependencyCycleException]] is thrown
    * when a cycle is detected.
    */
  def validate(): Unit =
    Component.validateAll(List(this))

  /** Forces initialization of this component and its dependencies (in parallel, using given `ExecutionContext`).
    * Returns a `Future` containing the initialized component value. NOTE: the component is initialized only once and
    * its value is cached.
    */
  def init(using ExecutionContext): Future[T] =
    doInit(starting = true)

  /** Destroys this component and all its dependencies (in reverse initialization order, i.e. first the component and
    * then its dependencies. Destroying calls the function that was registered with [[destroyWith]] or
    * [[asyncDestroyWith]] and clears the cached component instance so that it is created anew if [[init]] is called
    * again. If possible, independent components are destroyed in parallel, using given `ExecutionContext`.
    */
  def destroy(using ExecutionContext): Future[Unit] =
    Component.destroyAll(List(this))

  private def doDestroy(using ExecutionContext): Future[Unit] =
    getIfReady.fold(Future.unit) { value =>
      storage.set(null)
      destroyer(value)
    }

  private def doInit(starting: Boolean)(using ExecutionContext): Future[T] =
    storage.getPlain match {
      case null =>
        val promise = Promise[T]()
        if (storage.compareAndSet(null, promise.future)) {
          if (starting) {
            validate()
          }
          val resultFuture =
            Future
              .traverse(dependencies)(_.doInit(starting = false))
              .flatMap(resolvedDeps => creator(resolvedDeps))
              .recoverNow { case NonFatal(cause) =>
                throw ComponentInitializationException(this, cause)
              }
          promise.completeWith(resultFuture)
        }
        storage.get()
      case future =>
        future
    }
}
object Component {
  type DestroyFunction[-T] = ExecutionContext ?=> T => Future[Unit]

  def emptyDestroy[T]: DestroyFunction[T] =
    reusableEmptyDestroy.asInstanceOf[DestroyFunction[T]]

  private val reusableEmptyDestroy: DestroyFunction[Any] =
    _ => Future.unit

  def async[T](definition: => T): ExecutionContext ?=> Future[T] =
    Future(definition)

  def validateAll(components: Seq[Component[?]]): Unit =
    GraphUtils.dfs(components)(
      _.dependencies.toList,
      onCycle = (node, stack) => {
        val cyclePath = node :: (node :: stack.map(_.node).takeWhile(_ != node)).reverse
        throw DependencyCycleException(cyclePath)
      }
    )

  /** Destroys all given components and their dependencies by calling their destroy function (registered with
    * [[Component.destroyWith()]] or [[Component.asyncDestroyWith()]]) and clearing up cached component instances. It is
    * ensured that a component is only destroyed after all components that depend on it are destroyed (reverse
    * initialization order). Independent components are destroyed in parallel, using given `ExecutionContext`.
    */
  def destroyAll(components: Seq[Component[?]])(using ExecutionContext): Future[Unit] = {
    val reverseGraph = new MHashMap[Component[?], MListBuffer[Component[?]]]
    val terminals = new MHashSet[Component[?]]
    GraphUtils.dfs(components)(
      _.dependencies.toList,
      onEnter = { (c, _) =>
        reverseGraph.getOrElseUpdate(c, new MListBuffer) // make sure there is entry for all nodes
        if (c.dependencies.nonEmpty)
          c.dependencies.foreach { dep =>
            reverseGraph.getOrElseUpdate(dep, new MListBuffer) += c
          }
        else
          terminals += c
      }
    )
    val destroyFutures = new MHashMap[Component[?], Future[Unit]]

    def doDestroy(c: Component[?]): Future[Unit] =
      destroyFutures.getOrElseUpdate(c, Future.traverse(reverseGraph(c))(doDestroy).flatMap(_ => c.doDestroy))

    Future.traverse(reverseGraph.keys)(doDestroy).toUnit
  }
}

/** A wrapper over [[Component]] that has an implicit conversion from arbitrary expression of type T to
  * [[AutoComponent]]. This is used when you need to accept a parameter that may contain other component references.
  *
  * Using [[AutoComponent]] avoids explicit wrapping of expressions passed as that parameter into [[Component]] (using
  * `component` macro).
  */
case class AutoComponent[+T](component: Component[T]) extends AnyVal
