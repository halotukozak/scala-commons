package com.avsystem.commons
package concurrent

import collection.CloseableIterator

import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable

import scala.concurrent.Await
import scala.concurrent.duration.*

abstract class BlockingUtils {
  inline def defaultTimeout: Duration = 60.seconds

  inline def defaultBufferSize: Int = 128

  /**
   * Default scheduler used to run `Task`s and `Observable`s. This scheduler is not meant for blocking code.
   */
  given scheduler: Scheduler

  /**
   * Scheduler used for running blocking code.
   */
  def ioScheduler: Scheduler

  /**
   * Wraps blocking code into a [[Future]], making sure that blocking happens on an unbounded thread pool meant
   * specifically for that purpose.
   */
  inline def asFuture[T](blockingCode: => T): Future[T] =
    Future(blockingCode)(ioScheduler)

  /**
   * Wraps blocking code into a `Task`, making sure that blocking happens on an unbounded thread pool meant specifically
   * for that purpose.
   */
  inline def asTask[T](blockingCode: => T): Task[T] =
    Task.eval(blockingCode).executeOn(ioScheduler, forceAsync = true)

  inline def await[T](future: Future[T]): T =
    await(future, defaultTimeout)

  inline def await[T](future: Future[T], timeout: Long, unit: TimeUnit): T =
    await(future, FiniteDuration(timeout, unit))

  inline def await[T](future: Future[T], timeout: Duration): T =
    Await.result(future, timeout)

  // overloading instead of using default value so that it's usable from Java
  inline def runAndAwait[T](task: Task[T]): T =
    runAndAwait(task, defaultTimeout)

  inline def runAndAwait[T](task: Task[T], timeout: Long, unit: TimeUnit): T =
    runAndAwait(task, FiniteDuration(timeout, unit))

  inline def runAndAwait[T](task: Task[T], timeout: Duration): T =
    task.executeAsync.runSyncUnsafe(timeout)

  // overloading instead of using default value so that it's usable from Java
  inline def toIterator[T](observable: Observable[T]): CloseableIterator[T] =
    new ObservableBlockingIterator[T](observable, defaultTimeout.length, defaultTimeout.unit, defaultBufferSize)

  inline def toIterator[T](observable: Observable[T], nextElementTimeout: Duration): CloseableIterator[T] =
    new ObservableBlockingIterator[T](observable, nextElementTimeout.length, nextElementTimeout.unit, defaultBufferSize)

  inline def toIterator[T](observable: Observable[T], nextElementTimeout: Long, unit: TimeUnit): CloseableIterator[T] =
    new ObservableBlockingIterator[T](observable, nextElementTimeout, unit, defaultBufferSize)

  inline def toIterator[T](
    observable: Observable[T],
    nextElementTimeout: Duration,
    bufferSize: Int,
  ): CloseableIterator[T] =
    new ObservableBlockingIterator[T](observable, nextElementTimeout.length, nextElementTimeout.unit, bufferSize)

  inline def toIterator[T](
    observable: Observable[T],
    nextElementTimeout: Long,
    unit: TimeUnit,
    bufferSize: Int,
  ): CloseableIterator[T] =
    new ObservableBlockingIterator[T](observable, nextElementTimeout, unit, bufferSize)
}

object DefaultBlocking extends BlockingUtils {
  given scheduler: Scheduler = Scheduler.global

  lazy val ioScheduler: Scheduler = Scheduler.io()
}
