//package com.avsystem.commons
//package macros
//
//import scala.deriving.Mirror
//
//trait ApplierUnapplier[T, F] {
//  def apply(f: F): T
//
//  def unapply(t: T): F
//}
//
//object ApplyUnapplyTest {
//  final case class Empty()
//
//  final case class Single(int: Int)
//
//  final case class Multiple(int: Int, str: String)
//
//  final case class Gadt[T](t: T, list: List[T], cos: String)
//
//  final case class Generic[T](value: String)
//
//  trait Custom[T]
//
//  object Custom {
//    def apply[T](t: T): Custom[T] = null
//
//    def unapply[T](whatever: Custom[T]): Option[T] = None
//
//    given [T]: Mirror.ProductOf[Custom[T]] = new Mirror.Product {
//      type MirroredType = T
//      type MirroredMonoType = T
//      type MirroredElemTypes = Custom[T] *: EmptyTuple
//      
//      def fromProduct(product: Product): MirroredType = 
//        product.asInstanceOf[Custom[T]].asInstanceOf[MirroredType]
//    }
//  }
//
//  def applierUnapplier[T: Mirror.ProductOf, F]: ApplierUnapplier[T, F] = new ApplierUnapplier[T, F] {
//    def apply(f: F): T = ???
//
//    def unapply(t: T): F = ???
//  }
//
//  applierUnapplier[Empty, Unit]
//  applierUnapplier[Single, Int]
//  applierUnapplier[Multiple, (Int, String)]
//  applierUnapplier[Gadt[Int], (Int, List[Int], String)]
//  applierUnapplier[Custom[String], String]
//  applierUnapplier[Generic[String], String]
//}
