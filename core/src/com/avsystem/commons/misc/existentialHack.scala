package com.avsystem.commons.misc

/** Scala 3 does not support existential types, so eg:
  * ```scala
  * def m[K[_]](e: K[?])
  * ```
  * will throw an error "unreducible application of higher-kinded type K to wildcard argument".
  *
  * This type alias is a workaround for thi limitation, allowing to use `AnyOf[K]` instead of `K[?]`.
  *
  * Usage:
  * ```scala
  * def m[K[_]](e: AnyOf[K])
  * ```
  */

type AnyOf[K[_]] = Kind[K]#T

type Kind[K[_]] = {
  type A
  type T = K[A]
}
