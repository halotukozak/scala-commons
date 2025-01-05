package com.avsystem.commons
package di

import scala.concurrent.Await
import scala.concurrent.duration.Duration

abstract class BaseComponent(using info: ComponentInfo) {
  println(s"$info init")
}

final class SubDao(using ComponentInfo) extends BaseComponent

final class SubService(dao: SubDao)(using ComponentInfo) extends BaseComponent

final class SubSystem extends Components {
  override protected def componentNamePrefix: String = "sub."

  private val dao: Component[SubDao] =
    component(new SubDao)

  val service: Component[SubService] =
    component(new SubService(dao.ref))
}

final class Service(subService: SubService)(using ComponentInfo) extends BaseComponent

final class System(subSystem: SubSystem) extends Components {
  val service: Component[Service] =
    component(new Service(subSystem.service.ref))
}

object ComponentComposition {
  def main(args: Array[String]): Unit = {
    val subSystem = new SubSystem
    val system = new System(subSystem)

    import ExecutionContext.Implicits.global
    Await.result(system.service.init, Duration.Inf)
  }
}
