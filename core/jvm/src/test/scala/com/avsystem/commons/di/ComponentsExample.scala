package com.avsystem.commons
package di

import scala.concurrent.Await
import scala.concurrent.duration.Duration

final case class DynamicConfig(databaseUrl: String, bulbulator: BulbulatorConfig)

final case class BulbulatorConfig(types: List[String])

abstract class MyComponent {
  println(s"starting $this initialization on ${Thread.currentThread()}")
  Thread.sleep(100)
  println(s"finished $this initialization")

  def destroy(): Unit = {
    println(s"starting teardown of $this")
    Thread.sleep(100)
    println(s"finished teardown of $this")
  }
}

final class DynamicDep(db: Database) extends MyComponent

final class Database(databaseUrl: String) extends MyComponent

final class BulbulatorDao(config: BulbulatorConfig)(using db: Database) extends MyComponent

final class DeviceDao(using db: Database) extends MyComponent

final class FullApplication(dynamicDep: DynamicDep)(using bulbulatorDao: BulbulatorDao, deviceDao: DeviceDao) {
  println("full initialization")
}

trait DatabaseComponents extends Components {
  def config: DynamicConfig

  def dynamicDep(db: Component[Database]): Component[DynamicDep] =
    component(new DynamicDep(db.ref)).destroyWith(_.destroy())

  given database: Component[Database] =
    component(new Database(config.databaseUrl)).destroyWith(_.destroy())

  given bulbulatorDao: Component[BulbulatorDao] =
    component(new BulbulatorDao(config.bulbulator)).destroyWith(_.destroy())

  given deviceDao: Component[DeviceDao] =
    component(new DeviceDao).destroyWith(_.destroy())
}

final class ComponentsExample(val config: DynamicConfig) extends Components with DatabaseComponents {
  val fullApplication: Component[FullApplication] =
    component(new FullApplication(dynamicDep(database).ref))
}

object ComponentsExample {

  import ExecutionContext.Implicits.global

  def main(args: Array[String]): Unit = {
    val config = DynamicConfig("whatever", BulbulatorConfig(List("jeden", "drugi")))
    val comps = new ComponentsExample(config)
    Await.result(comps.fullApplication.init, Duration.Inf)
    Await.result(comps.fullApplication.destroy, Duration.Inf)
  }
}
