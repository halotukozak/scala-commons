package com.avsystem.commons
package di

package lazyVals {

  final class Redis {
    println("initializing redis")
  }

  final class Database {
    println("initializing db")
  }

  final class Server(db: Database, cache: Redis) {
    println("initializing server")
  }

  object MyApp {
    lazy val db: Database = new Database
    lazy val redis: Redis = new Redis
    lazy val server: Server = new Server(db, redis)

    def main(args: Array[String]): Unit = {
      server // force init
    }
  }

}

package lazyValsImplicits {

  final class Redis {
    println("initializing redis")
  }

  final class Database {
    println("initializing db")
  }

  final class Server(using db: Database, cache: Redis) {
    println("initializing server")
  }

  object MyApp {
    implicit lazy val db: Database = new Database
    implicit lazy val redis: Redis = new Redis
    lazy val server: Server = new Server

    def main(args: Array[String]): Unit = {
      server // force init
    }
  }

}

package components {

  final class Redis {
    println("initializing redis")
  }

  final class Database {
    println("initializing db")
  }

  final class Server(db: Database, cache: Redis) {
    println("initializing server")
  }

  object MyApp extends Components {
    val db: Component[Database] =
      component(new Database)

    val redis: Component[Redis] =
      component(new Redis)

    val server: Component[Server] =
      component(new Server(db.ref, redis.ref))

    def main(args: Array[String]): Unit = {
      import ExecutionContext.Implicits.global
      server.init // redis & db will initialize in parallel
    }
  }

}

package componentsImplicits {

  final class Redis {
    println("initializing redis")
  }

  final class Database {
    println("initializing db")
  }

  final class Server(using db: Database, cache: Redis) {
    println("initializing server")
  }

  object MyApp extends Components {
    given db: Component[Database] =
      component(new Database)

    given redis: Component[Redis] =
      component(new Redis)

    val server: Component[Server] =
      component(new Server)

    def main(args: Array[String]): Unit = {
      import ExecutionContext.Implicits.global
      server.init // redis & db will initialize in parallel
    }
  }

}

//noinspection ForwardReference
package cycleDetection {
  final class Redis(server: Server) {
    println("initializing redis")
  }

  final class Database {
    println("initializing db")
  }

  final class Server(db: Database, cache: Redis) {
    println("initializing server")
  }

  object MyApp extends Components {

    val db: Component[Database] =
      component(new Database)

    val redis: Component[Redis] =
      component(new Redis(server.ref))

    val server: Component[Server] =
      component(new Server(db.ref, redis.ref))

    def main(args: Array[String]): Unit = {
      import ExecutionContext.Implicits.global
      server.init // redis & db will initialize in parallel
    }
  }
}

//noinspection ForwardReference
package entryPoints {

  final class HttpServer

  final class FtpServer

  final class FullApplication(httpServer: HttpServer, ftpServer: FtpServer)

  object MyApp extends Components {

    val httpServer: Component[HttpServer] =
      component(new HttpServer)

    val ftpServer: Component[FtpServer] =
      component(new FtpServer)

    val fullApplication: Component[FullApplication] =
      component(new FullApplication(httpServer.ref, ftpServer.ref))

    def main(args: Array[String]): Unit = {
      import ExecutionContext.Implicits.global
      fullApplication.init
    }
  }

}
