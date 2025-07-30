package com.avsystem.commons
package serialization

import com.avsystem.commons.annotation.MayBeReplacedWith
import scala.quoted.Expr
import scala.quoted.Type
import scala.quoted.Quotes
import com.avsystem.commons.macros._
import com.avsystem.commons.macros.error

/** A typeclass which serves as evidence that some type `T` is a "transparent" wrapper of some other type. This usually
  * means that instances of various typeclasses (e.g. [[GenCodec]]) for type `T` could be automatically derived from
  * instances for the wrapped type. How this actually happens is decided in each typeclass which can define appropriate
  * implicit.
  */
@MayBeReplacedWith("Scala 3 opaque types")
trait TransparentWrapping[R, T] {
  def wrap(r: R): T
  def unwrap(t: T): R
}
object TransparentWrapping {
  private val reusableIdentity = new TransparentWrapping[Any, Any] {
    def wrap(r: Any): Any = r
    def unwrap(t: Any): Any = t
  }

  // unfortunately can't make this implicit, the compiler is not good enough and gets lost in implicit divergence
  def identity[T]: TransparentWrapping[T, T] =
    reusableIdentity.asInstanceOf[TransparentWrapping[T, T]]
}

/** Base class for companion objects of case classes which are transparent wrappers ("newtypes") over their only field.
  * This is the usual way of providing [[TransparentWrapping]] for some type and is intended as a replacement for
  * [[transparent]] annotation where possible.
  */
abstract class TransparentWrapperCompanion[R, T <: Product] extends TransparentWrapping[R, T] with (R => T) {
  given self: TransparentWrapping[R, T] = this

  def apply(r: R): T
  def unapply(t: T): T

  final def wrap(r: R): T = apply(r)
  final def unwrap(t: T): R =
    // In Scala 3 extractors were redesigned. We assume that you make a simple case value class.
    try unapply(t).productElement(0).asInstanceOf[R]
    catch
      case _: MatchError =>
        throw new NoSuchElementException(s"unapply for $t failed")

  given ordering(using Ordering[R]): Ordering[T] =
    Ordering.by(unwrap)
}

abstract class StringWrapperCompanion[T <: Product] extends TransparentWrapperCompanion[String, T]
abstract class IntWrapperCompanion[T <: Product] extends TransparentWrapperCompanion[Int, T]
abstract class LongWrapperCompanion[T <: Product] extends TransparentWrapperCompanion[Long, T]
abstract class FloatWrapperCompanion[T <: Product] extends TransparentWrapperCompanion[Float, T]
abstract class DoubleWrapperCompanion[T <: Product] extends TransparentWrapperCompanion[Double, T]
abstract class BooleanWrapperCompanion[T <: Product] extends TransparentWrapperCompanion[Boolean, T]
