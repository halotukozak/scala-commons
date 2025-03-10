package com.avsystem.commons.misc

import com.avsystem.commons.IIterable

object NOpt {
  // These two are used as NOpt's raw value to represent empty NOpt and NOpt(null).
  // Unfortunately, null itself can't be used for that purpose because https://github.com/scala/bug/issues/7396
  private object EmptyMarker extends Serializable

  private object NullMarker extends Serializable

  /**
   * Creates a [[NOpt]] out of given value. Works like `Option.apply`, i.e. `null` is translated into an empty [[NOpt]].
   * Note however that [[NOpt]] does have a representation of "present null" (which can be obtained using
   * [[NOpt.some]]).
   */
  def apply[A](value: A): NOpt[A] =
    if value == null then NOpt.Empty
    else new NOpt(value)

  def unapply[A](opt: NOpt[A]): NOpt[A] = opt // name-based extractor

  def some[A](value: A): NOpt[A] =
    new NOpt(if value == null then NullMarker else value)

  given opt2Iterable[A]: Conversion[NOpt[A], IIterable[A]] = _.toList

  final val Empty: NOpt[Nothing] = new NOpt(EmptyMarker)

  def empty[A]: NOpt[A] = Empty

  private val emptyMarkerFunc: Any => Any = _ => EmptyMarker

  final class WithFilter[+A] private[NOpt] (self: NOpt[A], p: A => Boolean) {
    def map[B](f: A => B): NOpt[B] = self.filter(p).map(f)

    def flatMap[B](f: A => NOpt[B]): NOpt[B] = self.filter(p).flatMap(f)

    def foreach[U](f: A => U): Unit = self.filter(p).foreach(f)

    def withFilter(q: A => Boolean): WithFilter[A] = new WithFilter[A](self, x => p(x) && q(x))
  }
}

/**
 * Like [[Opt]] but does have a counterpart for `Some(null)`. In other words, [[NOpt]] is a "nullable [[Opt]]".
 */
final class NOpt[+A] private (private val rawValue: Any) extends AnyVal with OptBase[A] with Serializable {

  import NOpt.*

  private def value: A = (if rawValue.asInstanceOf[AnyRef] eq NullMarker then null else rawValue).asInstanceOf[A]

  def isEmpty: Boolean = rawValue.asInstanceOf[AnyRef] eq EmptyMarker

  inline def isDefined: Boolean = !isEmpty

  inline def nonEmpty: Boolean = isDefined

  def get: A =
    if isEmpty then throw new NoSuchElementException("empty NOpt") else value

  inline def boxed[B](using boxing: Boxing[A, B]): NOpt[B] =
    map(boxing.fun)

  inline def unboxed[B](using unboxing: Unboxing[B, A]): NOpt[B] =
    map(unboxing.fun)

  inline def toOption: Option[A] =
    if isEmpty then None else Some(value)

  /**
   * Converts this `NOpt` into `Opt`. Because `Opt` cannot hold `null`, `NOpt(null)` is translated to `Opt.Empty`.
   */
  inline def toOpt: Opt[A] =
    if isEmpty then Opt.Empty else Opt(value)

  /**
   * Converts this `NOpt` into `OptRef`, changing the element type into boxed representation if necessary (e.g.
   * `Boolean` into `java.lang.Boolean`). Because `OptRef` cannot hold `null`, `NOpt(null)` is translated to
   * `OptRef.Empty`.
   */
  inline def toOptRef[B >: Null](using boxing: Boxing[A, B]): OptRef[B] =
    if isEmpty then OptRef.Empty else OptRef(boxing.fun(value))

  /**
   * Converts this `NOpt` into `OptArg`. Because `OptArg` cannot hold `null`, `NOpt(null)` is translated to
   * `OptArg.Empty`.
   */
  inline def toOptArg: OptArg[A] =
    if isEmpty then OptArg.Empty else OptArg(value)

  inline def getOrElse[B >: A](default: => B): B =
    if isEmpty then default else value

  inline def orNull[B >: A](using ev: Null <:< B): B =
    if isEmpty then ev(null) else value

  inline def map[B](f: A => B): NOpt[B] =
    if isEmpty then NOpt.Empty else NOpt.some(f(value))

  inline def fold[B](ifEmpty: => B)(f: A => B): B =
    if isEmpty then ifEmpty else f(value)

  /**
   * The same as [[fold]] but takes arguments in a single parameter list for better type inference.
   */
  inline def mapOr[B](ifEmpty: => B, f: A => B): B =
    if isEmpty then ifEmpty else f(value)

  inline def flatMap[B](f: A => NOpt[B]): NOpt[B] =
    if isEmpty then NOpt.Empty else f(value)

  inline def flatten[B](implicit ev: A <:< NOpt[B]): NOpt[B] =
    if isEmpty then NOpt.Empty else ev(value)

  inline def filter(p: A => Boolean): NOpt[A] =
    if isEmpty || p(value) then this else NOpt.Empty

  def withFilter(p: A => Boolean): NOpt.WithFilter[A] =
    new NOpt.WithFilter[A](this, p)

  inline def filterNot(p: A => Boolean): NOpt[A] =
    if isEmpty || !p(value) then this else NOpt.Empty

  inline def contains[A1 >: A](elem: A1): Boolean =
    !isEmpty && value == elem

  inline def exists(p: A => Boolean): Boolean =
    !isEmpty && p(value)

  inline def forall(p: A => Boolean): Boolean =
    isEmpty || p(value)

  inline def foreach[U](f: A => U): Unit =
    if !isEmpty then f(value)

  def collect[B](pf: PartialFunction[A, B]): NOpt[B] =
    if !isEmpty then {
      val res = pf.applyOrElse(value, NOpt.emptyMarkerFunc)
      new NOpt(if res == null then NullMarker else res)
    } else NOpt.Empty

  inline def orElse[B >: A](alternative: => NOpt[B]): NOpt[B] =
    if isEmpty then alternative else this

  inline def iterator: Iterator[A] =
    if isEmpty then Iterator.empty else Iterator.single(value)

  inline def toList: List[A] =
    if isEmpty then List() else value :: Nil

  inline def toRight[X](left: => X): Either[X, A] =
    if isEmpty then Left(left) else Right(value)

  inline def toLeft[X](right: => X): Either[A, X] =
    if isEmpty then Right(right) else Left(value)

  inline def zip[B](that: NOpt[B]): NOpt[(A, B)] =
    if isEmpty || that.isEmpty then NOpt.Empty else NOpt((this.get, that.get))

  /**
   * Apply side effect only if NOpt is empty. It's a bit like foreach for NOpt.Empty
   *
   * @param sideEffect
   *   \- code to be executed if nopt is empty
   * @return
   *   the same nopt
   * @example
   *   {{{captionNOpt.forEmpty(logger.warn("caption is empty")).foreach(setCaption)}}}
   */
  inline def forEmpty(sideEffect: => Unit): NOpt[A] = {
    if isEmpty then sideEffect
    this
  }

  override def toString: String =
    if isEmpty then "NOpt.Empty" else s"NOpt($value)"
}
