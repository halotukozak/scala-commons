package com.avsystem.commons
package annotation

final class MayBeReplacedWith(val replacement: String) extends Deprecated with StaticAnnotation {
  override val since: String = "Future"
  override val forRemoval: Boolean = false
  override val annotationType: Class[MayBeReplacedWith] = classOf[MayBeReplacedWith]
}
