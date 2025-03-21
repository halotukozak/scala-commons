package com.avsystem.commons

import concurrent.RunNowEC
import misc.*

import scala.annotation.nowarn
import scala.collection.{AbstractIterator, BuildFrom, Factory, mutable}
import scala.compiletime.uninitialized
import scala.util.chaining.scalaUtilChainingOps

trait SharedExtensions {
  extension [A](a: A) {

    /**
     * The "pipe" operator. Alternative syntax to apply a function on an argument. Useful for fluent expressions and
     * avoiding intermediate variables.
     *
     * @example
     *   {{{someVeryLongExpression() |> (v => if(condition(v)) something(v) else somethingElse(v))}}}
     */
    inline def |>[B](f: A => B): B = f(a)
    inline def applyIf[A0 >: A](predicate: A => Boolean)(f: A => A0): A0 =
      if predicate(a) then f(a) else a

    /**
     * Explicit syntax to discard the value of a side-effecting expression. Useful when `-Ywarn-value-discard` compiler
     * option is enabled.
     */
    // noinspection UnitMethodIsParameterless
    @nowarn
    inline def discard: Unit = ()
    inline def thenReturn[T](value: T): T = value
    inline def option: Option[A] = Option(a)
    inline def opt: Opt[A] = Opt(a)

    /**
     * Converts a boxed primitive type into an `Opt` of its corresponding primitive type, converting `null` into
     * `Opt.Empty`. For example, calling `.unboxedOpt` on a `java.lang.Integer` will convert it to `Opt[Int]`.
     */
    inline def unboxedOpt[B](using unboxing: Unboxing[B, A]): Opt[B] = opt.map(unboxing.fun)
    inline def checkNotNull(msg: String): A = if a != null then a else throw new NullPointerException(msg)

    /**
     * Alternative syntax for applying some side effects on a value before returning it, without having to declare an
     * intermediate variable. Also, using `setup` confines the "setting-up" code in a separate code block which has more
     * clarity and avoids polluting outer scope.
     *
     * @example
     *   {{{
     * import javax.swing._
     * // this entire expression returns the panel
     * new JPanel().setup { p =>
     *   p.setEnabled(true)
     *   p.setSize(100, 100)
     * }
     *   }}}
     */
    inline def setup(code: A => Any): A = a.tap(code)
    inline def matchOpt[B](pf: PartialFunction[A, B]): Opt[B] = pf.applyOpt(a)

    /**
     * To be used instead of normal `match` keyword in pattern matching in order to suppress non-exhaustive match
     * checking.
     *
     * @example
     *   {{{
     *   Option(42) uncheckedMatch {
     *     case Some(int) => println(int)
     *   }
     *   }}}
     */
    inline def uncheckedMatch[B](pf: PartialFunction[A, B]): B =
      pf.applyOrElse(a, (obj: A) => throw new MatchError(obj))

    /**
     * Prints AST of the prefix in a compilation error. Useful for debugging macros.
     */
    //    def showAst: A = macro macros.UniversalMacros.showAst[A]

    /**
     * Prints raw AST of the prefix in a compilation error. Useful for debugging macros.
     */
    //    def showRawAst: A = macro macros.UniversalMacros.showRawAst[A]
    //
    //    def showSymbol: A = macro macros.UniversalMacros.showSymbol[A]
    //
    //    def showSymbolFullName: A = macro macros.UniversalMacros.showSymbolFullName[A]

    inline def debugMacro: A = a
  }

  extension [A](a: => A) {
    inline def evalFuture: Future[A] = Future.eval(a)
    inline def evalTry: Try[A] = Try(a)
    inline def optIf(condition: Boolean): Opt[A] = if condition then Opt(a) else Opt.Empty
    inline def optionIf(condition: Boolean): Option[A] = Option.when(condition)(a)
    inline def recoverFrom[T <: Throwable: ClassTag](fallbackValue: => A): A = try a
    catch case _: T => fallbackValue
    inline def recoverToOpt[T <: Throwable: ClassTag]: Opt[A] = try Opt(a)
    catch case _: T => Opt.Empty

  }

  extension [A >: Null](a: A) {
    inline def optRef: OptRef[A] = OptRef(a)
  }

  extension (str: String) {

    /**
     * Makes sure that `String` value is not `null` by replacing `null` with empty string.
     */
    inline def orEmpty: String = if str == null then "" else str
    inline def ensureSuffix(suffix: String): String = if str.endsWith(suffix) then str else str + suffix
    inline def ensurePrefix(prefix: String): String = if str.startsWith(prefix) then str else prefix + str
    inline def uncapitalize: String =
      if str.isEmpty || str.charAt(0).isLower then str
      else str.substring(0, 1).toLowerCase + str.substring(1)

    /**
     * Removes a newline character from every sequence of consecutive newline characters. If the sequence contained just
     * one newline character without any whitespace before and after it, a space is inserted.
     *
     * e.g. `My hovercraft\nis full of eels.\n\nMy hovercraft is\n full of eels.` becomes `My hovercraft is full of
     * eels.\nMy hovercraft is full of eels.`
     *
     * Useful for multi-line string literals with lines wrapped in source code but without intention of including these
     * line breaks in actual runtime string.
     */
    inline def unwrapLines: String =
      RemovableLineBreak.replaceAllIn(
        str,
        { m =>
          val insertSpace = m.end == m.start + 1 && m.start - 1 >= 0 && m.end < str.length &&
            !Character.isWhitespace(str.charAt(m.start - 1)) && !Character.isWhitespace(str.charAt(m.end))
          if insertSpace then " " else m.matched.substring(1)
        },
      )

    /**
     * Adds a `|` character at the beginning of every line in this string except the first line. This is necessary when
     * splicing multiline strings into multiline string interpolations.
     */
    inline def multilineSafe: String = str.replace("\n", "\n|")

    inline def stripCommonIndent: String =
      if str.isEmpty then str
      else {
        val commonIndentLength = str.linesIterator
          .map(l => l.indexWhere(_ != ' '))
          .map(i => if i < 0 then Int.MaxValue else i)
          .min

        str.linesIterator
          .map(l => l.substring(math.min(commonIndentLength, l.length)))
          .mkString("\n")
      }
  }

  extension (int: Int)
    inline def times(code: => Any): Unit = {
      var i = 0
      while i < int do {
        code
        i += 1
      }
    }

  extension [A](fut: Future[A]) {
    inline def onCompleteNow[U](f: Try[A] => U): Unit = fut.onComplete(f)(RunNowEC)
    inline def andThenNow[U](pf: PartialFunction[Try[A], U]): Future[A] = fut.andThen(pf)(RunNowEC)
    inline def foreachNow[U](f: A => U): Unit = fut.foreach(f)(RunNowEC)
    inline def transformNow[S](s: A => S, f: Throwable => Throwable): Future[S] = fut.transform(s, f)(RunNowEC)
    inline def transformNow[S](f: Try[A] => Try[S]): Future[S] = fut.transform(f)(RunNowEC)
    inline def transformWithNow[S](f: Try[A] => Future[S]): Future[S] = fut.transformWith(f)(RunNowEC)
    inline def wrapToTry: Future[Try[A]] = fut.transformNow(Success(_))

    /**
     * Maps a `Future` using [[concurrent.RunNowEC RunNowEC]].
     */
    inline def mapNow[B](f: A => B): Future[B] = fut.map(f)(RunNowEC)

    /**
     * FlatMaps a `Future` using [[concurrent.RunNowEC RunNowEC]].
     */
    inline def flatMapNow[B](f: A => Future[B]): Future[B] = fut.flatMap(f)(RunNowEC)
    inline def filterNow(p: A => Boolean): Future[A] = fut.filter(p)(RunNowEC)
    inline def collectNow[B](pf: PartialFunction[A, B]): Future[B] = fut.collect(pf)(RunNowEC)
    inline def recoverNow[U >: A](pf: PartialFunction[Throwable, U]): Future[U] = fut.recover(pf)(RunNowEC)
    inline def recoverWithNow[B >: A](pf: PartialFunction[Throwable, Future[B]]): Future[B] =
      fut.recoverWith(pf)(RunNowEC)
    inline def zipWithNow[B, R](that: Future[B])(f: (A, B) => R): Future[R] = fut.zipWith(that)(f)(RunNowEC)
    inline def toUnit: Future[Unit] = mapNow(_ => ())
    inline def toVoid: Future[Void] = mapNow(_ => null: Void)

    /**
     * Returns a `Future` that completes with the specified `result`, but only after this future completes.
     */
    inline def thenReturn[T](result: Future[T]): Future[T] =
      val p = Promise[T]()
      fut.onComplete(_ => p.completeWith(result))(RunNowEC)
      p.future

    /**
     * Returns a `Future` that completes successfully, but only after this future completes.
     */
    inline def ignoreFailures: Future[Unit] = thenReturn(Future.successful {})

  }

  extension [A](fut: => Future[A]) {

    /**
     * Evaluates a left-hand-side expression that returns a `Future` and ensures that all exceptions thrown by that
     * expression are converted to a failed `Future`. Also, if left-hand-side expression returns `null`, it's converted
     * to a `Future` failed with `NullPointerException`.
     */
    inline def catchFailures: Future[A] =
      val result =
        try fut
        catch case NonFatal(t) => Future.failed(t)
      if result != null then result else Future.failed(new NullPointerException("null Future"))
  }
  extension (fut: Future.type) {

    /**
     * Evaluates an expression and wraps its value into a `Future`. Failed `Future` is returned if expression evaluation
     * throws an exception. This is very similar to `Future.apply` but evaluates the argument immediately, without
     * dispatching it to some `ExecutionContext`.
     */
    inline def eval[T](expr: => T): Future[T] =
      try Future.successful(expr)
      catch case NonFatal(cause) => Future.failed(cause)

    /**
     * Different version of `Future.traverse`. Transforms a `IterableOnce[A]` into a `Future[IterableOnce[B]]`, which
     * only completes after all `in` `Future`s are completed, using the provided function `A => Future[B]`. This is
     * useful for performing a parallel map. For example, to apply a function to all items of a list
     *
     * @tparam A
     *   the type of the value inside the Futures in the `IterableOnce`
     * @tparam B
     *   the type of the value of the returned `Future`
     * @tparam M
     *   the type of the `IterableOnce` of Futures
     * @param in
     *   the `IterableOnce` of Futures which will be sequenced
     * @param fn
     *   the function to apply to the `IterableOnce` of Futures to produce the results
     * @return
     *   the `Future` of the `IterableOnce` of results
     */
    inline def traverseCompleted[A, B, M[X] <: IterableOnce[X]](
      in: M[A],
    )(fn: A => Future[B])(using bf: BuildFrom[M[A], B, M[B]]): Future[M[B]] = {
      val (barrier, i) = in.iterator.foldLeft((Future.unit, Future.successful(bf.newBuilder(in)))) {
        case ((priorFinished, fr), a) =>
          val transformed = fn(a)
          (transformed.thenReturn(priorFinished), fr.zipWithNow(transformed)(_ += _))
      }
      barrier.thenReturn(i.mapNow(_.result()))
    }

    /**
     * Different version of `Future.sequence`. Transforms a `IterableOnce[Future[A]]` into a `Future[IterableOnce[A]`,
     * which only completes after all `in` `Future`s are completed.
     *
     * @tparam A
     *   the type of the value inside the Futures
     * @tparam M
     *   the type of the `IterableOnce` of Futures
     * @param in
     *   the `IterableOnce` of Futures which will be sequenced
     * @return
     *   the `Future` of the `IterableOnce` of results
     */
    inline def sequenceCompleted[A, M[X] <: IterableOnce[X]](in: M[Future[A]])(using
      bf: BuildFrom[M[Future[A]], A, M[A]],
    ): Future[M[A]] = traverseCompleted(in)(identity)

  }

  extension [A](option: Option[A]) {

    /**
     * Converts this `Option` into `Opt`. Because `Opt` cannot hold `null`, `Some(null)` is translated to `Opt.Empty`.
     */
    def toOpt: Opt[A] = if option.isEmpty then Opt.Empty else Opt(option.get)

    /**
     * Converts this `Option` into `OptRef`, changing the element type into boxed representation if necessary (e.g.
     * `Boolean` into `java.lang.Boolean`). Because `OptRef` cannot hold `null`, `Some(null)` is translated to
     * `OptRef.Empty`.
     */
    inline def toOptRef[B >: Null](using boxing: Boxing[A, B]): OptRef[B] =
      if option.isEmpty then OptRef.Empty else OptRef(boxing.fun(option.get))

    inline def toNOpt: NOpt[A] = if option.isEmpty then NOpt.Empty else NOpt.some(option.get)

    /**
     * Converts this `Option` into `OptArg`. Because `OptArg` cannot hold `null`, `Some(null)` is translated to
     * `OptArg.Empty`.
     */
    inline def toOptArg: OptArg[A] = if option.isEmpty then OptArg.Empty else OptArg(option.get)

    /**
     * Apply side effect only if Option is empty. It's a bit like foreach for None
     *
     * @param sideEffect
     *   \- code to be executed if option is empty
     * @return
     *   the same option
     * @example
     *   {{{captionOpt.forEmpty(logger.warn("caption is empty")).foreach(setCaption)}}}
     */
    inline def forEmpty(sideEffect: => Unit): Option[A] = {
      if option.isEmpty then sideEffect
      option
    }

    /**
     * The same as `fold` but takes arguments in a single parameter list for better type inference.
     */
    inline def mapOr[B](ifEmpty: => B, f: A => B): B = option.fold(ifEmpty)(f)

  }

  extension [A](tr: Try[A]) {

    /**
     * Converts this `Try` into `Opt`. Because `Opt` cannot hold `null`, `Success(null)` is translated to `Opt.Empty`.
     */
    inline def toOpt: Opt[A] = if tr.isFailure then Opt.Empty else Opt(tr.get)

    /**
     * Converts this `Try` into `OptRef`, changing the element type into boxed representation if necessary (e.g.
     * `Boolean` into `java.lang.Boolean`). Because `OptRef` cannot hold `null`, `Success(null)` is translated to
     * `OptRef.Empty`.
     */
    inline def toOptRef[B >: Null](using boxing: Boxing[A, B]): OptRef[B] =
      if tr.isFailure then OptRef.Empty else OptRef(boxing.fun(tr.get))

    inline def toNOpt: NOpt[A] = if tr.isFailure then NOpt.Empty else NOpt.some(tr.get)

    /**
     * Converts this `Try` into `OptArg`. Because `OptArg` cannot hold `null`, `Success(null)` is translated to
     * `OptArg.Empty`.
     */
    inline def toOptArg: OptArg[A] = if tr.isFailure then OptArg.Empty else OptArg(tr.get)
  }

  extension [A](tr: => Try[A]) {

    /**
     * Evaluates a left-hand side expression that return `Try`, catches all exceptions and converts them into a
     * `Failure`.
     */
    inline def catchFailures: Try[A] = try tr
    catch case NonFatal(t) => Failure(t)

  }

  extension (t: Try.type) {

    /**
     * Simple version of `TryOps.traverse`. Transforms a `IterableOnce[Try[A]]` into a `Try[IterableOnce[A]]`. Useful
     * for reducing many `Try`s into a single `Try`.
     */
    inline def sequence[A, M[X] <: IterableOnce[X]](in: M[Try[A]])(using bf: BuildFrom[M[Try[A]], A, M[A]]): Try[M[A]] =
      in.iterator
        .foldLeft(Try(bf.newBuilder(in))) {
          case (f @ Failure(e), Failure(newEx)) => e.addSuppressed(newEx); f
          case (tr, tb) => for (r <- tr; a <- tb) yield r += a
        }
        .map(_.result())

    /**
     * Transforms a `IterableOnce[A]` into a `Try[IterableOnce[B]]` using the provided function `A => Try[B]`. For
     * example, to apply a function to all items of a list:
     *
     * {{{
     *    val myTryList = TryOps.traverse(myList)(x => Try(myFunc(x)))
     * }}}
     */
    inline def traverse[A, B, M[X] <: IterableOnce[X]](
      in: M[A],
    )(fn: A => Try[B])(using bf: BuildFrom[M[A], B, M[B]]): Try[M[B]] =
      in.iterator
        .map(fn)
        .foldLeft(Try(bf.newBuilder(in))) {
          case (f @ Failure(e), Failure(newEx)) => e.addSuppressed(newEx); f
          case (tr, tb) => for (r <- tr; b <- tb) yield r += b
        }
        .map(_.result())

  }

  opaque type Entry[K, V] <: (K, V) = (K, V)
  private final val NoValueMarkerFunc = (_: Any) => NoValueMarker

  extension [A, B](pf: PartialFunction[A, B]) {

    /**
     * The same thing as `orElse` but with arguments flipped. Useful in situations where `orElse` would have to be
     * called on a partial function literal, which does not work well with type inference.
     */
    inline def unless(pre: PartialFunction[A, B]): PartialFunction[A, B] = pre.orElse(pf)

    inline def applyNOpt(a: A): NOpt[B] = pf.applyOrElse(a, NoValueMarkerFunc) match
      case NoValueMarker => NOpt.Empty
      case rawValue => NOpt.some(rawValue.asInstanceOf[B])

    def applyOpt(a: A): Opt[B] = pf.applyOrElse(a, NoValueMarkerFunc) match
      case NoValueMarker => Opt.Empty
      case rawValue => Opt(rawValue.asInstanceOf[B])

    inline def fold[C](a: A)(forEmpty: A => C, forNonEmpty: B => C): C = pf.applyOrElse(a, NoValueMarkerFunc) match
      case NoValueMarker => forEmpty(a)
      case rawValue => forNonEmpty(rawValue.asInstanceOf[B])

  }

  extension [C[X] <: IterableOnce[X], A](coll: C[A]) {
    private def it: Iterator[A] = coll.iterator

    inline def toSized[To](fac: Factory[A, To], sizeHint: Int): To = {
      val b = fac.newBuilder
      b.sizeHint(sizeHint)
      b ++= coll
      b.result()
    }

    inline def toMapBy[K](keyFun: A => K): Map[K, A] = mkMap(keyFun, identity)

    inline def mkMap[K, V](keyFun: A => K, valueFun: A => V): Map[K, V] = {
      val res = Map.newBuilder[K, V]
      it.foreach { a =>
        res += ((keyFun(a), valueFun(a)))
      }
      res.result()
    }

    inline def groupToMap[K, V, To](keyFun: A => K, valueFun: A => V)(using bf: BuildFrom[C[A], V, To]): Map[K, To] = {
      val builders = mutable.Map[K, mutable.Builder[V, To]]()
      it.foreach { a =>
        builders.getOrElseUpdate(keyFun(a), bf.newBuilder(coll)) += valueFun(a)
      }
      builders.iterator.map { case (k, v) => (k, v.result()) }.toMap
    }

    inline def findOpt(p: A => Boolean): Opt[A] = it.find(p).toOpt

    inline def flatCollect[B](f: PartialFunction[A, IterableOnce[B]])(using fac: Factory[B, C[B]]): C[B] =
      coll.iterator.collect(f).flatten.to(fac)

    inline def collectFirstOpt[B](pf: PartialFunction[A, B]): Opt[B] = it.collectFirst(pf).toOpt

    inline def reduceOpt[A1 >: A](op: (A1, A1) => A1): Opt[A1] = if it.isEmpty then Opt.Empty else it.reduce(op).opt

    inline def reduceLeftOpt[B >: A](op: (B, A) => B): Opt[B] = if it.isEmpty then Opt.Empty else it.reduceLeft(op).opt

    inline def reduceRightOpt[B >: A](op: (A, B) => B): Opt[B] =
      if it.isEmpty then Opt.Empty else it.reduceRight(op).opt

    inline def maxOpt(implicit ord: Ordering[A]): Opt[A] = if it.isEmpty then Opt.Empty else it.max.opt

    inline def maxOptBy[B: Ordering](f: A => B): Opt[A] = if it.isEmpty then Opt.Empty else it.maxBy(f).opt

    inline def minOpt(implicit ord: Ordering[A]): Opt[A] = if it.isEmpty then Opt.Empty else it.min.opt

    inline def minOptBy[B: Ordering](f: A => B): Opt[A] = if it.isEmpty then Opt.Empty else it.minBy(f).opt

    inline def indexOfOpt(elem: A): Opt[Int] = coll.iterator.indexOf(elem).opt.filter(_ != -1)

    inline def indexWhereOpt(p: A => Boolean): Opt[Int] = coll.iterator.indexWhere(p).opt.filter(_ != -1)

    inline def mkStringOr(start: String, sep: String, end: String, inline default: String): String =
      if it.nonEmpty then it.mkString(start, sep, end) else default

    inline def mkStringOr(sep: String, default: String): String =
      if it.nonEmpty then it.mkString(sep) else default

    inline def mkStringOrEmpty(start: String, sep: String, end: String): String =
      mkStringOr(start, sep, end, "")

    inline def asyncFoldLeft[B](zero: Future[B])(fun: (B, A) => Future[B])(using ExecutionContext): Future[B] =
      it.foldLeft(zero)((fb, a) => fb.flatMap(b => fun(b, a)))

    inline def asyncFoldRight[B](zero: Future[B])(fun: (A, B) => Future[B])(using ExecutionContext): Future[B] =
      it.foldRight(zero)((a, fb) => fb.flatMap(b => fun(a, b)))

    inline def asyncForeach(fun: A => Future[Unit])(using ExecutionContext): Future[Unit] =
      it.foldLeft[Future[Unit]](Future.unit)((fu, a) => fu.flatMap(_ => fun(a)))

    inline def partitionEither[L, R](
      fun: A => Either[L, R],
    )(using facL: Factory[L, C[L]], facR: Factory[R, C[R]]): (C[L], C[R]) = {
      val leftBuilder = facL.newBuilder
      val rightBuilder = facR.newBuilder
      coll.iterator.foreach(fun(_) match {
        case Left(l) => leftBuilder += l
        case Right(r) => rightBuilder += r
      })
      (leftBuilder.result(), rightBuilder.result())
    }
  }

  extension [C[X] <: IterableOnce[X], K, V](coll: C[(K, V)]) {

    inline def intoMap[M[X, Y] <: BMap[X, Y]](using fac: Factory[(K, V), M[K, V]]): M[K, V] = {
      val builder = fac.newBuilder
      coll.iterator.foreach(builder += _)
      builder.result()
    }

  }

  extension [A](set: BSet[A]) {
    inline def containsAny(other: BIterable[A]): Boolean = other.exists(set.contains)

    inline def containsAll(other: BIterable[A]): Boolean = other.forall(set.contains)

  }

  extension [C[X] <: BIterable[X], A](coll: C[A]) {
    inline def headOpt: Opt[A] = if coll.isEmpty then Opt.Empty else Opt(coll.head)

    inline def lastOpt: Opt[A] = if coll.isEmpty then Opt.Empty else Opt(coll.last)

  }
  private val RemovableLineBreak = "\\n+".r

  object Entry {
    def apply[K, V](key: K, value: V): Entry[K, V] = (key, value)

    def unapply[K, V](entry: Entry[K, V]): Some[(K, V)] = Some(entry)
  }

  private object NoValueMarker

  extension [M[X, Y] <: BMap[X, Y], K, V](map: M[K, V]) {
    inline def getOpt(key: K): Opt[V] = map.get(key).toOpt

    /** For iterating, filtering, mapping etc without having to use tuples */
    inline def entries: Iterator[Entry[K, V]] = map.iterator.map { case (k, v) => Entry(k, v) }

  }
  extension [A](it: Iterator[A]) {
    def pairs: Iterator[(A, A)] = new AbstractIterator[(A, A)] {
      private var first: NOpt[A] = NOpt.empty

      def hasNext: Boolean = it.hasNext && (first.nonEmpty || {
        first = NOpt(it.next())
        it.hasNext
      })

      def next(): (A, A) =
        if !hasNext then throw new NoSuchElementException
        else
          val f = first.get // safe because hasNext was called
          first = NOpt.Empty
          (f, it.next())
    }

    inline def nextOpt: Opt[A] = if it.hasNext then it.next().opt else Opt.Empty

    inline def drainTo[C[_]](n: Int)(using fac: Factory[A, C[A]]): C[A] = {
      val builder = fac.newBuilder
      var i = 0
      while it.hasNext && i < n do {
        builder += it.next()
        i += 1
      }
      builder.result()
    }

    def collectWhileDefined[B](pf: PartialFunction[A, B]): Iterator[B] = new AbstractIterator[B] {
      private var fetched = false
      private var value: NOpt[B] = uninitialized

      private def fetch(): Unit = if it.hasNext then value = pf.applyNOpt(it.next()) else value = NOpt.Empty

      def hasNext: Boolean = {
        if !fetched then {
          fetch()
          fetched = true
        }
        value.isDefined
      }

      def next(): B = {
        if !fetched then fetch()
        value match
          case NOpt(v) => fetched = false; v
          case NOpt.Empty => throw new NoSuchElementException
      }
    }

  }

  extension (it: Iterator.type) {
    def untilEmpty[T](elem: => Opt[T]): Iterator[T] = new AbstractIterator[T] {
      private var fetched = false
      private var value = Opt.empty[T]

      def hasNext: Boolean = {
        if !fetched then {
          value = elem
          fetched = true
        }
        value.isDefined
      }

      def next(): T =
        if !fetched then value = elem
        value match
          case Opt(v) =>
            fetched = false
            v
          case Opt.Empty => throw new NoSuchElementException
    }

    def iterateUntilEmpty[T](start: Opt[T])(nextFun: T => Opt[T]): Iterator[T] = new AbstractIterator[T] {
      private var fetched = true
      private var value = start

      def hasNext: Boolean = {
        if !fetched then {
          value = nextFun(value.get)
          fetched = true
        }
        value.isDefined
      }

      def next(): T = {
        if !fetched then value = nextFun(value.get)
        value match
          case Opt(v) =>
            fetched = false
            v
          case Opt.Empty => throw new NoSuchElementException
      }
    }

  }

}

object SharedExtensions extends SharedExtensions
