package com.avsystem.commons
package jiop

import com.avsystem.commons.annotation.MayBeReplacedWith

import java.util.concurrent.{Executor, TimeUnit}
import com.avsystem.commons.misc.Sam
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture, SettableFuture}
import com.google.common.base as gbase

import scala.annotation.unchecked.uncheckedVariance
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, CanAwait, ExecutionException, TimeoutException}

trait GuavaInterop {
  type GFunction[F, T] = gbase.Function[F, T]
  type GSupplier[T] = gbase.Supplier[T]
  type GPredicate[T] = gbase.Predicate[T]

  @MayBeReplacedWith("fun(_,_)")
  def gFunction[F, T](fun: F => T) = Sam[GFunction[F, T]](fun)
  @MayBeReplacedWith("() => expr")
  def gSupplier[T](expr: => T) = Sam[GSupplier[T]](expr)
  @MayBeReplacedWith("pred(_)")
  def gPredicate[T](pred: T => Boolean) = Sam[GPredicate[T]](pred)

  extension [T](gfut: ListenableFuture[T]) {
    def asScala: Future[T] = gfut match {
      case FutureAsListenableFuture(fut) => fut
      case _ => ListenableFutureAsScala(gfut)
    }

    def asScalaUnit: Future[Unit] =
      asScala.toUnit
  }

  extension [T](gfut: SettableFuture[T]) {
    def asScalaPromise: Promise[T] = new SettableFutureAsPromise(gfut)
  }

  extension [T](fut: Future[T]) {
    def asGuava: ListenableFuture[T] = fut match {
      case ListenableFutureAsScala(gfut) => gfut
      case _ => FutureAsListenableFuture(fut)
    }

    def asGuavaVoid: ListenableFuture[Void] =
      fut.toVoid.asGuava
  }

  private case class ListenableFutureAsScala[+T](gfut: ListenableFuture[T @uncheckedVariance]) extends Future[T] {
    def isCompleted: Boolean =
      gfut.isDone

    def onComplete[U](f: Try[T] => U)(using ec: ExecutionContext): Unit = {
      val callback = new FutureCallback[T] {
        def onFailure(t: Throwable): Unit = f(Failure(t))
        def onSuccess(result: T): Unit = f(Success(result))
      }
      val executor = ec match {
        case executor: Executor => executor
        case _ => new Executor {
          def execute(command: Runnable): Unit =
            ec.execute(command)
        }
      }
      Futures.addCallback(gfut, callback, executor)
    }

    def transform[S](f: Try[T] => Try[S])(using ExecutionContext): Future[S] = {
      val p = Promise[S]()
      onComplete { r =>
        p.complete(try f(r) catch {
          case NonFatal(t) => Failure(t)
        })
      }
      p.future
    }

    def transformWith[S](f: Try[T] => Future[S])(using ExecutionContext): Future[S] = {
      val p = Promise[S]()
      onComplete { r =>
        try p.completeWith(f(r)) catch {
          case NonFatal(t) => p.failure(t)
        }
      }
      p.future
    }

    private def unwrapFailures(expr: => T @uncheckedVariance): T =
      try expr catch {
        case ee: ExecutionException => throw ee.getCause
      }

    def value: Option[Try[T]] =
      if (gfut.isDone) Some(Try(unwrapFailures(gfut.get))) else None

    @throws(classOf[Exception])
    def result(atMost: Duration)(implicit permit: CanAwait): T =
      if (atMost.isFinite)
        unwrapFailures(gfut.get(atMost.length, atMost.unit))
      else
        unwrapFailures(gfut.get())

    @throws(classOf[InterruptedException])
    @throws(classOf[TimeoutException])
    def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
      try result(atMost) catch {
        case NonFatal(_) =>
      }
      this
    }
  }

  private case class FutureAsListenableFuture[T](fut: Future[T]) extends ListenableFuture[T] {
    def addListener(listener: Runnable, executor: Executor): Unit = {
      listener.checkNotNull("listener is null")
      val ec = executor match {
        case ec: ExecutionContext => ec
        case _ => new ExecutionContext {
          def reportFailure(cause: Throwable): Unit =
            cause.printStackTrace()
          def execute(runnable: Runnable): Unit =
            executor.execute(runnable)
        }
      }
      fut.onComplete(_ => listener.run())(using ec)
    }

    def isCancelled: Boolean =
      false

    private def wrapFailures(expr: => T): T =
      try expr catch {
        case NonFatal(e) => throw new ExecutionException(e)
      }

    def get(): T =
      wrapFailures(Await.result(fut, Duration.Inf))

    def get(timeout: Long, unit: TimeUnit): T =
      wrapFailures(Await.result(fut, Duration(timeout, unit)))

    def cancel(mayInterruptIfRunning: Boolean): Boolean =
      throw new UnsupportedOperationException

    def isDone: Boolean =
      fut.isCompleted
  }

  private class SettableFutureAsPromise[T](fut: SettableFuture[T])
    extends ListenableFutureAsScala[T](fut) with Promise[T] {

    def future: Future[T] = this
    def tryComplete(result: Try[T]): Boolean = result match {
      case Success(value) => fut.set(value)
      case Failure(cause) => fut.setException(cause)
    }
  }
}
