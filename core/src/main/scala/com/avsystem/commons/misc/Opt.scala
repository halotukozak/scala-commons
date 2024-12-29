package com.avsystem.commons
package misc

import com.avsystem.commons.IIterable

object Opt {
  // Used as Opt's raw value to represent empty Opt. Unfortunately, null can't be used for that purpose
  // because https://github.com/scala/bug/issues/7396
  private object EmptyMarker extends Serializable

  def apply[A](value: A): Opt[A] = if value != null then new Opt[A](value) else Opt.Empty

  def unapply[A](opt: Opt[A]): Opt[A] = opt //name-based extractor

  def some[A](value: A): Opt[A] =
    if value != null then new Opt[A](value)
    else throw new NullPointerException

  given opt2Iterable[A]: Conversion[Opt[A], IIterable[A]] = _.toList

  final val Empty: Opt[Nothing] = new Opt(EmptyMarker)

  def empty[A]: Opt[A] = Empty

  private val emptyMarkerFunc: Any => Any = _ => EmptyMarker

  final class WithFilter[+A] private[Opt](self: Opt[A], p: A => Boolean) {
    def map[B](f: A => B): Opt[B] = self.filter(p).map(f)

    def flatMap[B](f: A => Opt[B]): Opt[B] = self.filter(p).flatMap(f)

    def foreach[U](f: A => U): Unit = self.filter(p).foreach(f)

    def withFilter(q: A => Boolean): WithFilter[A] = new WithFilter[A](self, x => p(x) && q(x))
  }

  extension [A](opt: => Opt[A])
    /** When a given condition is true, evaluates the `opt` argument and returns it.
     * When the condition is false, `opt` is not evaluated and `Opt.Empty` is
     * returned.
     */
    def when(cond: Boolean): Opt[A] = if cond then opt else Opt.Empty

    /** Unless a given condition is true, this will evaluate the `opt` argument and
     * return it. Otherwise, `opt` is not evaluated and `Opt.Empty` is returned.
     */
    inline def unless(cond: Boolean): Opt[A] = when(!cond)
  end extension
}

/**
 * Like `Option` but implemented as value class (avoids boxing) and treats `null` as no value.
 * Therefore, there is no equivalent for `Some(null)`.
 *
 * If you need a value-class version of `Option` which differentiates between no value and `null` value,
 * use [[NOpt]].
 *
 * WARNING: Unfortunately, using `Opt` in pattern matches turns of exhaustivity checking.
 * Please be aware of that tradeoff.
 */
final class Opt[+A] private(private val rawValue: Any) extends AnyVal with OptBase[A] with Serializable {

  import Opt.*

  private def value: A = rawValue.asInstanceOf[A]

  def isEmpty: Boolean = rawValue.asInstanceOf[AnyRef] eq EmptyMarker

  inline def isDefined: Boolean = !isEmpty

  inline def nonEmpty: Boolean = isDefined

  def get: A =
    if isEmpty then throw new NoSuchElementException("empty Opt") else value

  inline def boxed[B](using boxing: Boxing[A, B]): Opt[B] =
    map(boxing.fun)

  inline def boxedOrNull[B >: Null](using boxing: Boxing[A, B]): B =
    if isEmpty then null else boxing.fun(value)

  inline def unboxed[B](using unboxing: Unboxing[B, A]): Opt[B] =
    map(unboxing.fun)

  inline def toOption: Option[A] =
    if isEmpty then None else Some(value)

  inline def toOptRef[B >: Null](using boxing: Boxing[A, B]): OptRef[B] =
    if isEmpty then OptRef.Empty else OptRef(boxing.fun(value))

  inline def toNOpt: NOpt[A] =
    if isEmpty then NOpt.Empty else NOpt(value)

  inline def toOptArg: OptArg[A] =
    if isEmpty then OptArg.Empty else OptArg(value)

  inline def getOrElse[B >: A](default: => B): B =
    if isEmpty then default else value

  inline def orNull[B >: A](using ev: Null <:< B): B =
    if isEmpty then ev(null) else value

  /**
   * Analogous to `Option.map` except that when mapping function returns `null`,
   * empty `Opt` is returned as a result.
   */
  inline def map[B](f: A => B): Opt[B] =
    if isEmpty then Opt.Empty else Opt(f(value))

  inline def fold[B](ifEmpty: => B)(f: A => B): B =
    if isEmpty then ifEmpty else f(value)

  /**
   * The same as [[fold]] but takes arguments in a single parameter list for better type inference.
   */
  inline def mapOr[B](ifEmpty: => B, f: A => B): B =
    if isEmpty then ifEmpty else f(value)

  inline def flatMap[B](f: A => Opt[B]): Opt[B] =
    if isEmpty then Opt.Empty else f(value)

  inline def flatten[B](using ev: A <:< Opt[B]): Opt[B] =
    if isEmpty then Opt.Empty else ev(value)

  inline def filter(p: A => Boolean): Opt[A] =
    if (isEmpty || p(value)) this else Opt.Empty

  def withFilter(p: A => Boolean): Opt.WithFilter[A] =
    new Opt.WithFilter[A](this, p)

  inline def filterNot(p: A => Boolean): Opt[A] =
    if isEmpty || !p(value) then this else Opt.Empty

  inline def contains[A1 >: A](elem: A1): Boolean =
    !isEmpty && value == elem

  inline def exists(p: A => Boolean): Boolean =
    !isEmpty && p(value)

  inline def forall(p: A => Boolean): Boolean =
    isEmpty || p(value)

  inline def foreach[U](f: A => U): Unit =
    if !isEmpty then f(value)

  /**
   * Analogous to `Option.collect` except that when the function returns `null`,
   * empty `Opt` is returned as a result.
   */
  def collect[B](pf: PartialFunction[A, B]): Opt[B] =
    if !isEmpty then {
      val res = pf.applyOrElse(value, Opt.emptyMarkerFunc)
      new Opt(if (res == null) EmptyMarker else res)
    } else Opt.Empty

  inline def orElse[B >: A](alternative: => Opt[B]): Opt[B] =
    if isEmpty then alternative else this

  inline def iterator: Iterator[A] =
    if isEmpty then Iterator.empty else Iterator.single(value)

  inline def toList: List[A] =
    if isEmpty then List() else value :: Nil

  inline def toRight[X](left: => X): Either[X, A] =
    if isEmpty then Left(left) else Right(value)

  inline def toLeft[X](right: => X): Either[A, X] =
    if isEmpty then Right(right) else Left(value)

  inline def zip[B](that: Opt[B]): Opt[(A, B)] =
    if isEmpty || that.isEmpty then Opt.Empty else Opt((this.get, that.get))

  /**
   * Apply side effect only if Opt is empty. It's a bit like foreach for Opt.Empty
   *
   * @param sideEffect - code to be executed if opt is empty
   * @return the same opt
   * @example {{{captionOpt.forEmpty(logger.warn("caption is empty")).foreach(setCaption)}}}
   */
  inline def forEmpty(sideEffect: => Unit): Opt[A] = {
    if isEmpty then sideEffect
    this
  }

  override def toString: String =
    if isEmpty then "Opt.Empty" else s"Opt($value)"

}
