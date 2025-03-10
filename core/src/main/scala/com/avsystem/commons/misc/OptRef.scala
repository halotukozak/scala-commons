package com.avsystem.commons
package misc

object OptRef {
  def apply[A >: Null](value: A): OptRef[A] = new OptRef[A](value)

  def unapply[A >: Null](opt: OptRef[A]): OptRef[A] = opt // name-based extractor

  def some[A >: Null](value: A): OptRef[A] =
    if value != null then new OptRef[A](value)
    else throw new NullPointerException

  object Boxed:
    def unapply[A, B >: Null](optRef: OptRef[B])(using unboxing: Unboxing[A, B]): Opt[A] =
      if optRef.isEmpty then Opt.Empty else Opt(unboxing.fun(optRef.get))

  given opt2Iterable[A >: Null]: Conversion[OptRef[A], IIterable[A]] = _.toList

  final val Empty: OptRef[Null] = new OptRef[Null](null)

  def empty[A >: Null]: OptRef[A] = Empty

  private val nullFunc: Any => Null = _ => null

  final class WithFilter[+A >: Null] private[OptRef] (self: OptRef[A], p: A => Boolean) {
    def map[B >: Null](f: A => B): OptRef[B] = self.filter(p).map(f)

    def flatMap[B >: Null](f: A => OptRef[B]): OptRef[B] = self.filter(p).flatMap(f)

    def foreach[U](f: A => U): Unit = self.filter(p).foreach(f)

    def withFilter(q: A => Boolean): WithFilter[A] = new WithFilter[A](self, x => p(x) && q(x))
  }
}

/**
 * Like [[Opt]] but has better Java interop thanks to the fact that wrapped value has type `A` instead of `Any`. For
 * example, Scala method defined like this:
 * {{{
 *   def takeMaybeString(str: OptRef[String]): Unit
 * }}}
 * will be seen by Java as:
 * {{{
 *   public void takeMaybeString(String str);
 * }}}
 * and `null` will be used to represent absence of value. <p/> This comes at the cost of `A` having to be a nullable
 * type. Also, empty value is represented internally using `null` which unfortunately makes [[OptRef]] suffer from
 * SI-7396 (`hashCode` fails on `OptRef.Empty` which means that you can't add [[OptRef]] values into hash sets or use
 * them as hash map keys).
 */
final class OptRef[+A >: Null] private (private val value: A) extends AnyVal with OptBase[A] with Serializable {
  def isEmpty: Boolean = value == null

  inline def isDefined: Boolean = !isEmpty

  inline def nonEmpty: Boolean = isDefined

  def get: A =
    if isEmpty then throw new NoSuchElementException("empty OptRef") else value

  inline def toOpt: Opt[A] =
    Opt(value)

  inline def toOption: Option[A] =
    Option(value)

  inline def toNOpt: NOpt[A] =
    if isEmpty then NOpt.Empty else NOpt(value)

  inline def toOptArg: OptArg[A] =
    if isEmpty then OptArg.Empty else OptArg(value)

  inline def getOrElse[B >: A](default: => B): B =
    if isEmpty then default else value

  inline def orNull[B >: A](using ev: Null <:< B): B =
    value.asInstanceOf[B]

  inline def map[B >: Null](f: A => B): OptRef[B] =
    if isEmpty then OptRef.Empty else OptRef(f(value))

  inline def fold[B](ifEmpty: => B)(f: A => B): B =
    if isEmpty then ifEmpty else f(value)

  /**
   * The same as [[fold]] but takes arguments in a single parameter list for better type inference.
   */
  inline def mapOr[B](ifEmpty: => B, f: A => B): B =
    if isEmpty then ifEmpty else f(value)

  inline def flatMap[B >: Null](f: A => OptRef[B]): OptRef[B] =
    if isEmpty then OptRef.Empty else f(value)

  inline def filter(p: A => Boolean): OptRef[A] =
    if isEmpty || p(value) then this else OptRef.Empty

  def withFilter(p: A => Boolean): OptRef.WithFilter[A] =
    new OptRef.WithFilter[A](this, p)

  inline def filterNot(p: A => Boolean): OptRef[A] =
    if isEmpty || !p(value) then this else OptRef.Empty

  inline def contains[A1 >: A](elem: A1): Boolean =
    !isEmpty && value == elem

  inline def exists(p: A => Boolean): Boolean =
    !isEmpty && p(value)

  inline def forall(p: A => Boolean): Boolean =
    isEmpty || p(value)

  inline def foreach[U](f: A => U): Unit =
    if !isEmpty then f(value)

  def collect[B >: Null](pf: PartialFunction[A, B]): OptRef[B] =
    if !isEmpty then new OptRef(pf.applyOrElse(value, OptRef.nullFunc)) else OptRef.Empty

  inline def orElse[B >: A](alternative: => OptRef[B]): OptRef[B] =
    if isEmpty then alternative else this

  inline def iterator: Iterator[A] =
    if isEmpty then Iterator.empty else Iterator.single(value)

  inline def toList: List[A] =
    if isEmpty then List() else new ::(value, Nil)

  inline def toRight[X](left: => X): Either[X, A] =
    if isEmpty then Left(left) else Right(value)

  inline def toLeft[X](right: => X): Either[A, X] =
    if isEmpty then Right(right) else Left(value)

  inline def zip[B >: Null](that: OptRef[B]): OptRef[(A, B)] =
    if isEmpty || that.isEmpty then OptRef.Empty else OptRef((this.get, that.get))

  /**
   * Apply side effect only if OptRef is empty. It's a bit like foreach for OptRef.Empty
   *
   * @param sideEffect
   *   \- code to be executed if optRef is empty
   * @return
   *   the same optRef
   * @example
   *   {{{captionOptRef.forEmpty(logger.warn("caption is empty")).foreach(setCaption)}}}
   */
  inline def forEmpty(sideEffect: => Unit): OptRef[A] = {
    if isEmpty then sideEffect
    this
  }

  override def toString: String =
    if isEmpty then "OptRef.Empty" else s"OptRef($value)"
}
