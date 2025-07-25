package com.avsystem.commons
package redis.util

import org.apache.pekko.actor.Actor
import org.apache.pekko.event.LoggingAdapter

/**
  * Author: ghik
  * Created: 12/04/16.
  */
trait ActorLazyLogging { self: Actor =>
  object log {
    val rawLog: LoggingAdapter = org.apache.pekko.event.Logging(context.system, self)

    def error(msg: => String, cause: Throwable = null): Unit =
      if (rawLog.isErrorEnabled) {
        if (cause == null) {
          rawLog.error(msg)
        } else {
          rawLog.error(cause, msg)
        }
      }

    def warning(msg: => String): Unit
    ??? // macro macros.misc.LazyLoggingMacros.warningImpl

    def info(msg: => String): Unit
    ??? // macro macros.misc.LazyLoggingMacros.infoImpl

    def debug(msg: => String): Unit
    ??? // macro macros.misc.LazyLoggingMacros.debugImpl
  }
}
