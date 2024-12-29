package com.avsystem.commons
package misc

import com.avsystem.commons.JInteger

final class BoxingUnboxingTest:
  val jint: JInteger = Opt(42).boxedOrNull
