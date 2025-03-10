package com.avsystem.commons
package redis.commands

import com.avsystem.commons.misc.{NamedEnum, NamedEnumCompanion}
import com.avsystem.commons.redis.CommandEncoder.CommandArg
import com.avsystem.commons.redis.*
import com.avsystem.commons.redis.commands.ReplyDecoders.*
import com.avsystem.commons.redis.exception.UnexpectedReplyException
import com.avsystem.commons.redis.protocol.*

import scala.collection.mutable.ListBuffer

trait GeoApi extends ApiSubset {

  /** Executes [[http://redis.io/commands/geoadd GEOADD]] */
  def geoadd(key: Key, member: Value, point: GeoPoint): Result[Boolean] =
    execute(new Geoadd(key, Opt.Empty, changed = false, (member, point).single).map(_ > 0))

  /** Executes [[http://redis.io/commands/geoadd GEOADD]] */
  def geoadd(key: Key, item: (Value, GeoPoint), items: (Value, GeoPoint)*): Result[Int] =
    execute(new Geoadd(key, Opt.Empty, changed = false, item +:: items))

  /**
   * Executes [[http://redis.io/commands/geoadd GEOADD]] or simply returns 0 when `items` is empty, without sending the
   * command to Redis
   */
  def geoadd(
    key: Key,
    items: Iterable[(Value, GeoPoint)],
    existence: OptArg[Existence] = OptArg.Empty,
    changed: Boolean = false,
  ): Result[Int] =
    execute(new Geoadd(key, existence.toOpt, changed, items))

  /** Executes [[http://redis.io/commands/geohash GEOHASH]] */
  def geohash(key: Key, members: Value*): Result[Seq[Opt[GeoHash]]] =
    execute(new Geohash(key, members))

  /**
   * Executes [[http://redis.io/commands/geohash GEOHASH]] NOTE: `members` CAN be empty (Redis accepts it)
   */
  def geohash(key: Key, members: Iterable[Value]): Result[Seq[Opt[GeoHash]]] =
    execute(new Geohash(key, members))

  /** Executes [[http://redis.io/commands/geopos GEOPOS]] */
  def geopos(key: Key, members: Value*): Result[Seq[Opt[GeoPoint]]] =
    execute(new Geopos(key, members))

  /**
   * Executes [[http://redis.io/commands/geopos GEOPOS]] NOTE: `members` CAN be empty (Redis accepts it)
   */
  def geopos(key: Key, members: Iterable[Value]): Result[Seq[Opt[GeoPoint]]] =
    execute(new Geopos(key, members))

  /** Executes [[http://redis.io/commands/geodist GEODIST]] */
  def geodist(key: Key, member1: Value, member2: Value, unit: GeoUnit = GeoUnit.M): Result[Opt[Double]] =
    execute(new Geodist(key, member1, member2, unit))

  /** Executes [[http://redis.io/commands/georadius GEORADIUS]] */
  def georadius[A <: GeoradiusAttrs](
    key: Key,
    point: GeoPoint,
    radius: Double,
    unit: GeoUnit,
    attributes: A = GeoradiusAttrs.None,
    count: OptArg[Long] = OptArg.Empty,
    sortOrder: OptArg[SortOrder] = OptArg.Empty,
    readOnly: Boolean = false,
  ): Result[Seq[A#Attributed[Value]]] =
    execute(new Georadius(key, point, radius, unit, attributes, count.toOpt, sortOrder.toOpt, readOnly))

  /** Executes [[http://redis.io/commands/georadiusbymember GEORADIUSBYMEMBER]] */
  def georadiusbymember[A <: GeoradiusAttrs](
    key: Key,
    member: Value,
    radius: Double,
    unit: GeoUnit,
    attributes: A = GeoradiusAttrs.None,
    count: OptArg[Long] = OptArg.Empty,
    sortOrder: OptArg[SortOrder] = OptArg.Empty,
    readOnly: Boolean = false,
  ): Result[Seq[A#Attributed[Value]]] =
    execute(new Georadiusbymember(key, member, radius, unit, attributes, count.toOpt, sortOrder.toOpt, readOnly))

  /** Executes [[http://redis.io/commands/georadius GEORADIUS]] */
  def georadiusStore(
    key: Key,
    point: GeoPoint,
    radius: Double,
    unit: GeoUnit,
    storeKey: Key,
    storeDist: Boolean = false,
    count: OptArg[Long] = OptArg.Empty,
    sortOrder: OptArg[SortOrder] = OptArg.Empty,
  ): Result[Opt[Long]] =
    execute(new GeoradiusStore(key, point, radius, unit, count.toOpt, sortOrder.toOpt, storeKey, storeDist))

  /** Executes [[http://redis.io/commands/georadiusbymember GEORADIUSBYMEMBER]] */
  def georadiusbymemberStore(
    key: Key,
    member: Value,
    radius: Double,
    unit: GeoUnit,
    storeKey: Key,
    storeDist: Boolean = false,
    count: OptArg[Long] = OptArg.Empty,
    sortOrder: OptArg[SortOrder] = OptArg.Empty,
  ): Result[Opt[Long]] =
    execute(new GeoradiusbymemberStore(key, member, radius, unit, count.toOpt, sortOrder.toOpt, storeKey, storeDist))

  private final class Geoadd(key: Key, existence: Opt[Existence], changed: Boolean, items: Iterable[(Value, GeoPoint)])
      extends RedisIntCommand
      with NodeCommand {
    val encoded: Encoded = encoder("GEOADD")
      .key(key)
      .optAdd(existence)
      .addFlag("CH", changed)
      .add(items.iterator.map({ case (v, p) => (p, valueCodec.write(v)) }))
      .result

    override def immediateResult: Opt[Int] = whenEmpty(items, 0)
  }

  private final class Geohash(key: Key, members: Iterable[Value])
      extends RedisSeqCommand[Opt[GeoHash]](nullBulkOr(bulk(bs => GeoHash(bs.utf8String))))
      with NodeCommand {
    val encoded: Encoded = encoder("GEOHASH").key(key).datas(members).result
  }

  private final class Geopos(key: Key, members: Iterable[Value])
      extends RedisSeqCommand[Opt[GeoPoint]](nullMultiBulkOr(multiBulkAsGeoPoint))
      with NodeCommand {
    val encoded: Encoded = encoder("GEOPOS").key(key).datas(members).result
  }

  private final class Geodist(key: Key, member1: Value, member2: Value, unit: GeoUnit)
      extends RedisOptDoubleCommand
      with NodeCommand {
    val encoded: Encoded = encoder("GEODIST").key(key).data(member1).data(member2).add(unit).result
  }

  private abstract class AbstractGeoradius[T](decoder: ReplyDecoder[T])(
    key: Key,
    point: Opt[GeoPoint],
    member: Opt[Value],
    radius: Double,
    unit: GeoUnit,
    flags: List[String],
    count: Opt[Long],
    sortOrder: Opt[SortOrder],
    readOnly: Boolean,
    storeKey: Opt[Key],
    storeDist: Boolean,
  ) extends AbstractRedisCommand[T](decoder)
      with NodeCommand {

    val encoded: Encoded = {
      val command = (if point.isDefined then "GEORADIUS" else "GEORADIUSBYMEMBER") + (if readOnly then "_RO" else "")
      encoder(command)
        .key(key)
        .optAdd(point)
        .optAdd(member.map(valueCodec.write))
        .add(radius)
        .add(unit)
        .add(flags)
        .optAdd("COUNT", count)
        .optAdd(sortOrder)
        .optKey(if storeDist then "STOREDIST" else "STORE", storeKey)
        .result
    }
  }

  private final class Georadius[A <: GeoradiusAttrs](
    key: Key,
    point: GeoPoint,
    radius: Double,
    unit: GeoUnit,
    attributes: A,
    count: Opt[Long],
    sortOrder: Opt[SortOrder],
    readOnly: Boolean,
  ) extends AbstractGeoradius[Seq[A#Attributed[Value]]](multiBulkAsSeq(geoAttributed(attributes, bulkAs[Value])))(
        key,
        point.opt,
        Opt.Empty,
        radius,
        unit,
        attributes.encodeFlags,
        count,
        sortOrder,
        readOnly,
        Opt.Empty,
        storeDist = false,
      )

  private final class GeoradiusStore(
    key: Key,
    point: GeoPoint,
    radius: Double,
    unit: GeoUnit,
    count: Opt[Long],
    sortOrder: Opt[SortOrder],
    storeKey: Key,
    storeDist: Boolean,
  ) extends AbstractGeoradius[Opt[Long]](nullBulkOr(integerAsLong))(
        key,
        point.opt,
        Opt.Empty,
        radius,
        unit,
        Nil,
        count,
        sortOrder,
        readOnly = false,
        storeKey.opt,
        storeDist,
      )

  private final class Georadiusbymember[A <: GeoradiusAttrs](
    key: Key,
    member: Value,
    radius: Double,
    unit: GeoUnit,
    attributes: A,
    count: Opt[Long],
    sortOrder: Opt[SortOrder],
    readOnly: Boolean,
  ) extends AbstractGeoradius[Seq[A#Attributed[Value]]](multiBulkAsSeq(geoAttributed(attributes, bulkAs[Value])))(
        key,
        Opt.Empty,
        member.opt,
        radius,
        unit,
        attributes.encodeFlags,
        count,
        sortOrder,
        readOnly,
        Opt.Empty,
        storeDist = false,
      )

  private final class GeoradiusbymemberStore(
    key: Key,
    member: Value,
    radius: Double,
    unit: GeoUnit,
    count: Opt[Long],
    sortOrder: Opt[SortOrder],
    storeKey: Key,
    storeDist: Boolean,
  ) extends AbstractGeoradius[Opt[Long]](nullBulkOr(integerAsLong))(
        key,
        Opt.Empty,
        member.opt,
        radius,
        unit,
        Nil,
        count,
        sortOrder,
        readOnly = false,
        storeKey.opt,
        storeDist,
      )
}

abstract class GeoradiusAttrs(val flags: Int) { self =>

  import GeoradiusAttrs.*

  type Attributed[A]

  def isEmpty: Boolean = flags == NoFlags

  def encodeFlags: List[String] = {
    val res = new ListBuffer[String]
    if (flags & DistFlag) != 0 then {
      res += "WITHDIST"
    }
    if (flags & HashFlag) != 0 then {
      res += "WITHHASH"
    }
    if (flags & CoordFlag) != 0 then {
      res += "WITHCOORD"
    }
    res.result()
  }

  def decode[A](element: ArrayMsg[RedisMsg], finalFlags: Int, wrapped: A): Attributed[A]

  def +(other: GeoradiusAttrs): GeoradiusAttrs { type Attributed[A] = self.Attributed[other.Attributed[A]] } =
    new GeoradiusAttrs(self.flags | other.flags) {
      type Attributed[A] = self.Attributed[other.Attributed[A]]
      def decode[A](element: ArrayMsg[RedisMsg], finalFlags: Int, wrapped: A): Attributed[A] =
        self.decode(element, finalFlags, other.decode(element, finalFlags, wrapped))
    }
}
object GeoradiusAttrs {
  private final val NoFlags = 0
  private final val DistFlag = 1 << 0
  private final val HashFlag = 1 << 1
  private final val CoordFlag = 1 << 2

  private def offset(flags: Int, flag: Int): Int =
    if (flags & flag) != 0 then 1 else 0

  object None extends GeoradiusAttrs(NoFlags) {
    type Attributed[A] = A

    def decode[A](element: ArrayMsg[RedisMsg], finalFlags: Int, wrapped: A): A = wrapped
  }

  case class Withdist[A](dist: Double, wrapped: A)
  object Withdist extends GeoradiusAttrs(DistFlag) {
    type Attributed[A] = Withdist[A]

    def decode[A](element: ArrayMsg[RedisMsg], finalFlags: Int, wrapped: A): Withdist[A] =
      element.elements(1) match {
        case BulkStringMsg(dist) => Withdist(dist.utf8String.toDouble, wrapped)
        case msg => throw new UnexpectedReplyException(s"Expected bulk string for DIST, got $msg")
      }
  }

  case class Withhash[A](hash: Long, wrapped: A)
  object Withhash extends GeoradiusAttrs(HashFlag) {
    type Attributed[A] = Withhash[A]

    def decode[A](element: ArrayMsg[RedisMsg], finalFlags: Int, wrapped: A): Withhash[A] =
      element.elements(1 + offset(finalFlags, DistFlag)) match {
        case IntegerMsg(hash) => Withhash(hash, wrapped)
        case msg => throw new UnexpectedReplyException(s"Expected integer for HASH, got $msg")
      }
  }

  case class Withcoord[A](coords: GeoPoint, wrapped: A)
  object Withcoord extends GeoradiusAttrs(CoordFlag) {
    type Attributed[A] = Withcoord[A]

    def decode[A](element: ArrayMsg[RedisMsg], finalFlags: Int, wrapped: A): Withcoord[A] =
      element.elements(1 + offset(finalFlags, DistFlag) + offset(finalFlags, HashFlag)) match {
        case ArrayMsg(IndexedSeq(BulkStringMsg(rawLong), BulkStringMsg(rawLat))) =>
          Withcoord(GeoPoint(rawLong.utf8String.toDouble, rawLat.utf8String.toDouble), wrapped)
        case msg =>
          throw new UnexpectedReplyException(s"Expected two-element array of bulk strings for COORD, got $msg")
      }
  }
}

case class GeoPoint(longitude: Double, latitude: Double)
object GeoPoint {
  implicit val commandArg: CommandArg[GeoPoint] = CommandArg { case (enc, GeoPoint(long, lat)) =>
    enc.add(long).add(lat)
  }
}

case class GeoHash(raw: String) extends AnyVal

sealed abstract class GeoUnit(val name: String) extends NamedEnum
object GeoUnit extends NamedEnumCompanion[GeoUnit] {
  case object M extends GeoUnit("m")
  case object Km extends GeoUnit("km")
  case object Mi extends GeoUnit("mi")
  case object Ft extends GeoUnit("ft")

  val values: List[GeoUnit] = caseObjects
}
