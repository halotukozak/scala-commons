package com.avsystem.commons
package jiop

import misc.Timestamp

import java.time.Instant

trait JavaTimeInterop:
  extension (instant: Instant)
    inline def truncateToTimestamp: Timestamp = Timestamp(instant.toEpochMilli)
    inline def truncateToJDate: JDate = new JDate(instant.toEpochMilli)

object JavaTimeInterop
