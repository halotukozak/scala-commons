package com.avsystem.commons
package derivation

import scala.language.future
import scala.compiletime.uninitialized

trait DeferredInstance[T] { this: T =>
  var underlying: T = uninitialized
}
