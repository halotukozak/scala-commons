package com.avsystem.commons
package redis.commands

import org.apache.pekko.util.ByteString
import com.avsystem.commons.misc.Timestamp
import com.avsystem.commons.redis.*

import scala.concurrent.duration.*

/**
 * Author: ghik Created: 28/09/16.
 */
trait StringsApiSuite extends CommandsSuite {

  import RedisApi.Batches.StringTyped.*

  apiTest("APPEND") {
    setup(set("key", "cos"))
    append("key", "value").assertEquals(8)
    append("nonkey", "value").assertEquals(5)
  }

  apiTest("BITCOUNT") {
    setup(valueType[ByteString].set("key", ByteString(0, 1, 0, 7, 0)))
    bitcount("???").assertEquals(0)
    bitcount("key").assertEquals(4)
    bitcount("key", (2, -1)).assertEquals(3)
  }

  apiTest("BITFIELD") {
    bitfield("key", BitField.signed(8).at(0).get).assertEquals(Opt(0))
    bitfield("key", BitField.signed(8).atWidths(0).set(50)).assertEquals(Opt(0))
    bitfield("key", BitField.signed(8).at(0).incrby(50)).assertEquals(Opt(100))
    bitfield("key", BitField.signed(8).atWidths(0).incrby(50).overflowSat).assertEquals(Opt(127))
    bitfield("key", BitField.signed(8).atWidths(0).incrby(50).overflowFail).assertEquals(Opt.Empty)
    bitfield("key", BitField.unsigned(8).atWidths(1).set(42)).assertEquals(Opt(0))
    bitfield(
      "key",
      BitField.signed(8).atWidths(0).set(500).overflowFail,
      BitField.unsigned(8).atWidths(0).set(257),
      BitField.unsigned(8).atWidths(0).get,
    ).assertEquals(Seq(Opt.Empty, Opt(127), Opt(1)))
  }

  apiTest("BITOP") {
    val withBinValue = valueType[ByteString]
    val keys = Seq("{key}1", "{key}2", "{key}3")
    setup(
      List("{key}1" -> bin"00000000", "{key}2" -> bin"11111111", "{key}3" -> bin"01010101")
        .map({ case (k, v) => withBinValue.set(k, v) })
        .sequence,
    )

    bitop(BitOp.And, "dest{key}", keys*).assertEquals(1)
    withBinValue.get("dest{key}").assertEquals(bin"00000000".opt)

    bitop(BitOp.Or, "dest{key}", keys*).assertEquals(1)
    withBinValue.get("dest{key}").assertEquals(bin"11111111".opt)

    bitop(BitOp.Xor, "dest{key}", keys*).assertEquals(1)
    withBinValue.get("dest{key}").assertEquals(bin"10101010".opt)
  }

  apiTest("BITOP NOT") {
    val withBinValue = valueType[ByteString]
    setup(withBinValue.set("key", bin"01010101"))

    bitopNot("dest{key}", "key").assertEquals(1)
    withBinValue.get("dest{key}").assertEquals(bin"10101010".opt)
  }

  apiTest("BITPOS") {
    val withBinValue = valueType[ByteString]
    setup(
      List(
        "{key}1" -> bin"00000000",
        "{key}2" -> bin"11111111",
        "{key}3" -> bin"00001111",
        "{key}4" -> bin"11110000",
        "{key}5" -> bin"1111000000001111",
      ).map({ case (k, v) => withBinValue.set(k, v) }).sequence,
    )

    bitpos("{key}1", bit = true).assertEquals(-1)
    bitpos("{key}2", bit = false).assertEquals(8)
    bitpos("{key}3", bit = true).assertEquals(4)
    bitpos("{key}3", bit = false).assertEquals(0)
    bitpos("{key}4", bit = true).assertEquals(0)
    bitpos("{key}4", bit = false).assertEquals(4)
    bitpos("{key}5", bit = false, 0, 1).assertEquals(4)
    bitpos("{key}5", bit = false, 1).assertEquals(8)
  }

  apiTest("DECR") {
    setup(set("key", "42"))
    decr("???").assertEquals(-1)
    decr("key").assertEquals(41)
  }

  apiTest("DECRBY") {
    setup(set("key", "42"))
    decrby("???", 3).assertEquals(-3)
    decrby("key", 5).assertEquals(37)
  }

  apiTest("GET") {
    setup(set("key", "value"))
    get("key").assertEquals("value".opt)
    get("???").assertEquals(Opt.Empty)
  }

  apiTest("GETBIT") {
    setup(valueType[ByteString].set("key", bin"00001111"))
    getbit("key", 2).assertEquals(false)
    getbit("key", 6).assertEquals(true)
  }

  apiTest("GETDEL") {
    setup(set("key", "value"))
    getdel("key").assertEquals(Opt("value"))
    getdel("key2").assertEquals(Opt.Empty)
  }

  apiTest("GETEX") {
    setup(set("key", "value"))
    getex("key", Expiration.Ex(10)).assertEquals(Opt("value"))
    getex("key2", Expiration.Ex(10)).assertEquals(Opt.Empty)
    getex("key", Expiration.Persist).assertEquals(Opt("value"))
  }

  apiTest("GETRANGE") {
    setup(set("key", "lolvalue"))
    getrange("key", 0, 2).assertEquals("lol")
    getrange("key", 5, -1).assertEquals("lue")
    getrange("???", 0, 2).assertEquals("")
  }

  apiTest("GETSET") {
    getset("key", "value").assertEquals(Opt.Empty)
    getset("key", "nevalue").assertEquals("value".opt)
  }

  apiTest("INCR") {
    incr("key").assertEquals(1)
    incr("key").assertEquals(2)
  }

  apiTest("INCRBY") {
    incrby("key", 3).assertEquals(3)
    incrby("key", 5).assertEquals(8)
  }

  apiTest("INCRBYFLOAT") {
    incrbyfloat("key", 3.14).assertEquals(3.14)
    incrbyfloat("key", 5).assertEquals(8.14)
  }

  apiTest("MGET") {
    setup(set("{key}1", "value1"), set("{key}2", "value2"), set("{key}3", "value3"))
    mget(Nil).assertEquals(Seq.empty)
    mget("{key}1", "{key}2", "{key}3", "{key}4")
      .assertEquals(Seq("value1".opt, "value2".opt, "value3".opt, Opt.Empty))
  }

  apiTest("MSET") {
    mset(Nil).get
    mset("{key}1" -> "value1", "{key}2" -> "value2", "{key}3" -> "value3").get
  }

  apiTest("MSETNX") {
    msetnx(Nil).assertEquals(true)
    msetnx("{key}1" -> "value1", "{key}2" -> "value2").assertEquals(true)
    msetnx("{key}1" -> "value1", "{key}2" -> "value2", "{key}3" -> "value3").assertEquals(false)
  }

  apiTest("PSETEX") {
    psetex("key", 1000, "value").get
  }

  apiTest("SET") {
    set("key", "value").assertEquals(true)
    set("key", "value", Expiration.Ex(100)).assertEquals(true)
    set("key", "value", Expiration.Px(100000)).assertEquals(true)
    set("key", "value", Expiration.ExAt((Timestamp.now() + 5.seconds).millis / 1000)).assertEquals(true)
    set("key", "value", Expiration.PxAt((Timestamp.now() + 5.seconds).millis)).assertEquals(true)
    set("key", "value", existence = Existence.XX).assertEquals(true)
    set("key", "value", existence = Existence.NX).assertEquals(false)
    setGet("key", "value2").assertEquals(Opt("value"))
  }

  apiTest("SETBIT") {
    setup(valueType[ByteString].set("key", bin"00001111"))
    setbit("key", 1, value = true).assertEquals(false)
    setbit("key", 5, value = false).assertEquals(true)
    valueType[ByteString].get("key").assertEquals(bin"01001011".opt)
  }

  apiTest("SETEX") {
    setex("key", 10, "value").get
  }

  apiTest("SETNX") {
    setnx("key", "value").assertEquals(true)
    setnx("key", "value").assertEquals(false)
  }

  apiTest("SETRANGE") {
    setrange("key", 0, "value").assertEquals(5)
    setrange("key", 3, "dafuq").assertEquals(8)
    get("key").assertEquals("valdafuq".opt)
  }

  apiTest("STRLEN") {
    setup(set("key", "value"))
    strlen("???").assertEquals(0)
    strlen("key").assertEquals(5)
  }
}
