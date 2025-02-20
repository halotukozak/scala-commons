package com.avsystem.commons
package misc

import scala.annotation.implicitNotFound
import scala.deriving.Mirror

/**
 * Typeclass which captures case class `apply` method in a raw form that takes untyped sequence of arguments.
 */
@implicitNotFound("cannot materialize Applier: ${T} is not a case class or case class like type")
trait Applier[T] {
  def apply(rawValues: Seq[Any]): T
}

object Applier {
  given materialize[T](using mirror: Mirror.ProductOf[T]): Applier[T] = (rawValues: Seq[Any]) => {
    val values = rawValues.toArray
    mirror.fromProduct(new Product {
      override def productArity: Int = values.length

      override def productElement(n: Int): Any = values(n)

      override def canEqual(that: Any): Boolean = that.isInstanceOf[T] || ???
    })
  }
}

/**
 * Typeclass which captures case class `unapply`/`unapplySeq` method in a raw form that returns untyped sequence of
 * values.
 */
@implicitNotFound("cannot materialize Unapplier: ${T} is not a case class or case class like type")
trait Unapplier[T] {
  def unapply(value: T): Seq[Any]
}

object Unapplier {
  given materialize[T <: scala.Product](using mirror: Mirror.ProductOf[T]): Unapplier[T] =
    (value: T) => mirror.fromProductTyped(value).productIterator.toSeq
}

class ProductUnapplier[T <: Product] extends Unapplier[T] {
  def unapply(value: T): Seq[Any] = IArraySeq.unsafeWrapArray(value.productIterator.toArray)
}

abstract class ProductApplierUnapplier[T <: Product] extends ProductUnapplier[T] with ApplierUnapplier[T]

@implicitNotFound("cannot materialize ApplierUnapplier: ${T} is not a case class or case class like type")
trait ApplierUnapplier[T] extends Applier[T] with Unapplier[T]

object ApplierUnapplier {
  given materialize[T <: scala.Product](using mirror: Mirror.ProductOf[T]): ApplierUnapplier[T] =
    new ProductApplierUnapplier[T] {
      override def apply(rawValues: Seq[Any]): T = {
        val values = rawValues.toArray
        mirror.fromProduct(new Product {
          override def productArity: Int = values.length

          override def productElement(n: Int): Any = values(n)

          override def canEqual(that: Any): Boolean = that.isInstanceOf[T] || ???
        })
      }

      override def unapply(value: T): Seq[Any] =
        mirror.fromProductTyped(value).productIterator.toSeq
    }
}
