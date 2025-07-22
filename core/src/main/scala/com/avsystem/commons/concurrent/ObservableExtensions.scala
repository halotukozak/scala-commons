package com.avsystem.commons
package concurrent

import monix.eval.Task
import monix.reactive.Observable

import scala.collection.Factory
import scala.util.Sorting

trait ObservableExtensions {
  extension [T](obs: Observable[T]) {

    /** Creates a [[monix.eval.Task Task]] that upon execution
      * will signal the first generated element of the source observable.
      *
      * Returns an `Opt.empty` for either empty source or null first element.
      */
    inline def headOptL: Task[Opt[T]] = obs.headOptionL.map(_.toOpt)

    /**
      * Returns a [[monix.eval.Task Task]] which emits the first <b>non-null</b> item for which the predicate holds.
      */
    inline def findOptL(inline p: T => Boolean): Task[Opt[T]] = obs.findL(e => e != null && p(e)).map(_.toOpt)

    /** Suppress the duplicate elements emitted by the source Observable.
      *
      * WARNING: this requires unbounded buffering.
      */
    inline def distinct: Observable[T] = distinctBy[T](identity)

    /** Given a function that returns a key for each element emitted by
      * the source Observable, suppress duplicates items.
      *
      * WARNING: this requires unbounded buffering.
      */
    inline def distinctBy[K](inline key: T => K): Observable[T] =
      obs
        .scan((MSet.empty[K], Opt.empty[T])) { case ((seenElems, _), elem) =>
          (seenElems, elem.optIf(seenElems.add(key(elem))))
        }
        .collect { case (_, Opt(elem)) => elem }
    /** Given a function that returns a key for each element emitted by the source Observable,
      * returns a [[monix.eval.Task Task]] that upon evaluation will collect all items from the source sorted by that key
      * in an immutable sequence.
      *
      * WARNING: this requires unbounded buffering.
      */
    inline def sortedByL[R](inline f: T => R)(using inline ord: Ordering[R], ct: ClassTag[T]): Task[ISeq[T]] =
      sortedL(using ord.on(f), ct)
    /** Creates a [[monix.eval.Task Task]] that upon evaluation will collect all items from the source in a sorted order
      * as an immutable sequence.
      *
      * WARNING: this requires unbounded buffering.
      */
    inline def sortedL(using inline ord: Ordering[T], ct: ClassTag[T]): Task[ISeq[T]] =
      obs
        .toL(Array)
        .map { arr =>
          Sorting.stableSort(arr)
          IArraySeq.unsafeWrapArray(arr)
        }
    /** Returns a [[monix.eval.Task Task]] that upon evaluation
      * will collect all items from the source into a [[Map]] instance
      * using provided functions to compute keys and values.
      *
      * WARNING: for infinite streams the process will eventually blow up
      * with an out of memory error.
      */
    inline def mkMapL[K, V](inline keyFun: T => K, inline valueFun: T => V): Task[Map[K, V]] =
      obs.map(v => (keyFun(v), valueFun(v))).toL(Map)
    /** Given a collection factory `factory`, returns a [[monix.eval.Task Task]] that upon evaluation
      * will collect all items from the source in the appropriate representation for the current element type `T`.
      *
      * Example uses for obs: Observable[(Int, String)]:
      * {{{
      *      obs.toL(List) // Task[List[(Int, String)]]
      *      obs.toL(ArrayBuffer) //Task[ArrayBuffer[(Int, String)]]
      *      obs.toL(Map) //Task[Map[Int, String]]
      * }}}
      *
      * WARNING: for infinite streams the process will eventually blow up
      * with an out of memory error.
      */
    inline def toL[R](inline factory: Factory[T, R]): Task[R] =
      obs
        .foldLeftL(factory.newBuilder)(_ += _)
        .map(_.result())
  }
}
object ObservableExtensions extends ObservableExtensions
