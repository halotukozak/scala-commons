//package com.avsystem.commons
//package misc
//
////import misc.macros.materializeDelegationImpl
//
///**
// * A typeclass which witnesses that type `A` can be wrapped into trait or abstract class `B`
// */
//trait Delegation[A, B] {
//  def delegate(a: A): B
//}
//
//object Delegation {
//  inline given materializeDelegation[A, B]: Delegation[A, B] = ${ materializeDelegationImpl[A, B] }
//
//  /**
//   * Provides following syntax:
//   *
//   * Delegation[TargetType](value)
//   *
//   */
//  def apply[B] = new CurriedDelegation[B]
//
//  final class CurriedDelegation[B] {
//    inline def apply[A](source: A): B = ${ delegateImpl[A, B] }
//  }
//}
