package com.avsystem.commons
package misc

object OptArg {

  /**
   * This implicit conversion allows you to pass unwrapped values where `OptArg` is required.
   */
  implicit def argToOptArg[A](value: A): OptArg[A] = OptArg(value)

  // additional implicits to cover most common, safe numeric promotions
  implicit def intToOptArgLong(int: Int): OptArg[Long] = OptArg(int)

  implicit def intToOptArgDouble(int: Int): OptArg[Double] = OptArg(int)

  private object EmptyMarker extends Serializable

  def apply[A](value: A): OptArg[A] = new OptArg[A](if value != null then value else EmptyMarker)

  def unapply[A](opt: OptArg[A]): OptArg[A] = opt // name-based extractor

  def some[A](value: A): OptArg[A] =
    if value != null then new OptArg[A](value)
    else throw new NullPointerException

  val Empty: OptArg[Nothing] = new OptArg(EmptyMarker)

  def empty[A]: OptArg[A] = Empty
}

/**
 * [[OptArg]] is like [[Opt]] except it's intended to be used to type-safely express optional method/constructor
 * parameters while at the same time avoiding having to explicitly wrap arguments when passing them (thanks to the
 * implicit conversion from `A` to `OptArg[A]`). For example:
 *
 * {{{
 *   def takesMaybeString(str: OptArg[String] = OptArg.Empty) = ???
 *
 *   takesMaybeString()         // default empty value is used
 *   takesMaybeString("string") // no explicit wrapping into OptArg required
 * }}}
 *
 * Note that like [[Opt]], [[OptArg]] assumes its underlying value to be non-null and `null` is translated into
 * `OptArg.Empty`. <br/> It is strongly recommended that [[OptArg]] type is used ONLY in signatures where implicit
 * conversion `A => OptArg[A]` is intended to work. You should not use [[OptArg]] as a general-purpose "optional value"
 * type - other types like [[Opt]], [[NOpt]] and `Option` serve that purpose. For this reason [[OptArg]] deliberately
 * does not have any "transforming" methods like `map`, `flatMap`, `orElse`, etc. Instead it's recommended that
 * [[OptArg]] is converted to [[Opt]], [[NOpt]] or `Option` as soon as possible (using `toOpt`, `toNOpt` and `toOption`
 * methods).
 */
final class OptArg[+A] private (private val rawValue: Any) extends AnyVal with OptBase[A] with Serializable {

  import OptArg.*

  private def value: A = rawValue.asInstanceOf[A]

  def get: A = if isEmpty then throw new NoSuchElementException("empty OptArg") else value

  def isEmpty: Boolean = rawValue.asInstanceOf[AnyRef] eq EmptyMarker

  inline def isDefined: Boolean = !isEmpty

  inline def nonEmpty: Boolean = isDefined

  inline def boxedOrNull[B >: Null](implicit boxing: Boxing[A, B]): B =
    if isEmpty then null else boxing.fun(value)

  inline def toOpt: Opt[A] =
    if isEmpty then Opt.Empty else Opt(value)

  inline def toOption: Option[A] =
    if isEmpty then None else Some(value)

  inline def toNOpt: NOpt[A] =
    if isEmpty then NOpt.Empty else NOpt.some(value)

  inline def toOptRef[B >: Null](implicit boxing: Boxing[A, B]): OptRef[B] =
    if isEmpty then OptRef.Empty else OptRef(boxing.fun(value))

  inline def getOrElse[B >: A](default: => B): B =
    if isEmpty then default else value

  inline def orNull[B >: A](implicit ev: Null <:< B): B =
    if isEmpty then ev(null) else value

  inline def fold[B](ifEmpty: => B)(f: A => B): B =
    if isEmpty then ifEmpty else f(value)

  /**
   * The same as [[fold]] but takes arguments in a single parameter list for better type inference.
   */
  inline def mapOr[B](ifEmpty: => B, f: A => B): B =
    if isEmpty then ifEmpty else f(value)

  inline def contains[A1 >: A](elem: A1): Boolean =
    !isEmpty && value == elem

  inline def exists(p: A => Boolean): Boolean =
    !isEmpty && p(value)

  inline def forall(p: A => Boolean): Boolean =
    isEmpty || p(value)

  inline def foreach[U](f: A => U): Unit = {
    if !isEmpty then f(value)
  }

  inline def iterator: Iterator[A] =
    if isEmpty then Iterator.empty else Iterator.single(value)

  inline def toList: List[A] =
    if isEmpty then List() else new ::(value, Nil)

  inline def toRight[X](left: => X): Either[X, A] =
    if isEmpty then Left(left) else Right(value)

  inline def toLeft[X](right: => X): Either[A, X] =
    if isEmpty then Right(right) else Left(value)

  /**
   * Apply side effect only if OptArg is empty. It's a bit like foreach for OptArg.Empty
   *
   * @param sideEffect
   *   \- code to be executed if optArg is empty
   * @return
   *   the same optArg
   * @example
   *   {{{captionOptArg.forEmpty(logger.warn("caption is empty")).foreach(setCaption)}}}
   */
  inline def forEmpty(sideEffect: => Unit): OptArg[A] = {
    if isEmpty then {
      sideEffect
    }
    this
  }

  override def toString: String =
    if isEmpty then "OptArg.Empty" else s"OptArg($value)"
}
