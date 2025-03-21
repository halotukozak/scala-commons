package com.avsystem.commons
package mongo.typed

import com.avsystem.commons.misc.Applier
import org.bson.BsonDocument

trait ProjectionZippers { this: MongoProjection.type =>
  def zip[E, T1](p1: MongoProjection[E, T1]): MongoProjection[E, Tuple1[T1]] =
    new ProductProjection(Seq(p1))

  def zip[E, T1, T2](p1: MongoProjection[E, T1], p2: MongoProjection[E, T2]): MongoProjection[E, (T1, T2)] =
    new ProductProjection(Seq(p1, p2))

  def zip[E, T1, T2, T3](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
  ): MongoProjection[E, (T1, T2, T3)] =
    new ProductProjection(Seq(p1, p2, p3))

  def zip[E, T1, T2, T3, T4](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
    p4: MongoProjection[E, T4],
  ): MongoProjection[E, (T1, T2, T3, T4)] =
    new ProductProjection(Seq(p1, p2, p3, p4))

  def zip[E, T1, T2, T3, T4, T5](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
    p4: MongoProjection[E, T4],
    p5: MongoProjection[E, T5],
  ): MongoProjection[E, (T1, T2, T3, T4, T5)] =
    new ProductProjection(Seq(p1, p2, p3, p4, p5))

  def zip[E, T1, T2, T3, T4, T5, T6](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
    p4: MongoProjection[E, T4],
    p5: MongoProjection[E, T5],
    p6: MongoProjection[E, T6],
  ): MongoProjection[E, (T1, T2, T3, T4, T5, T6)] =
    new ProductProjection(Seq(p1, p2, p3, p4, p5, p6))

  def zip[E, T1, T2, T3, T4, T5, T6, T7](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
    p4: MongoProjection[E, T4],
    p5: MongoProjection[E, T5],
    p6: MongoProjection[E, T6],
    p7: MongoProjection[E, T7],
  ): MongoProjection[E, (T1, T2, T3, T4, T5, T6, T7)] =
    new ProductProjection(Seq(p1, p2, p3, p4, p5, p6, p7))

  def zip[E, T1, T2, T3, T4, T5, T6, T7, T8](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
    p4: MongoProjection[E, T4],
    p5: MongoProjection[E, T5],
    p6: MongoProjection[E, T6],
    p7: MongoProjection[E, T7],
    p8: MongoProjection[E, T8],
  ): MongoProjection[E, (T1, T2, T3, T4, T5, T6, T7, T8)] =
    new ProductProjection(Seq(p1, p2, p3, p4, p5, p6, p7, p8))

  def zip[E, T1, T2, T3, T4, T5, T6, T7, T8, T9](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
    p4: MongoProjection[E, T4],
    p5: MongoProjection[E, T5],
    p6: MongoProjection[E, T6],
    p7: MongoProjection[E, T7],
    p8: MongoProjection[E, T8],
    p9: MongoProjection[E, T9],
  ): MongoProjection[E, (T1, T2, T3, T4, T5, T6, T7, T8, T9)] =
    new ProductProjection(Seq(p1, p2, p3, p4, p5, p6, p7, p8, p9))

  def zip[E, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
    p4: MongoProjection[E, T4],
    p5: MongoProjection[E, T5],
    p6: MongoProjection[E, T6],
    p7: MongoProjection[E, T7],
    p8: MongoProjection[E, T8],
    p9: MongoProjection[E, T9],
    p10: MongoProjection[E, T10],
  ): MongoProjection[E, (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)] =
    new ProductProjection(Seq(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10))

  def zip[E, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
    p4: MongoProjection[E, T4],
    p5: MongoProjection[E, T5],
    p6: MongoProjection[E, T6],
    p7: MongoProjection[E, T7],
    p8: MongoProjection[E, T8],
    p9: MongoProjection[E, T9],
    p10: MongoProjection[E, T10],
    p11: MongoProjection[E, T11],
  ): MongoProjection[E, (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11)] =
    new ProductProjection(Seq(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11))

  def zip[E, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
    p4: MongoProjection[E, T4],
    p5: MongoProjection[E, T5],
    p6: MongoProjection[E, T6],
    p7: MongoProjection[E, T7],
    p8: MongoProjection[E, T8],
    p9: MongoProjection[E, T9],
    p10: MongoProjection[E, T10],
    p11: MongoProjection[E, T11],
    p12: MongoProjection[E, T12],
  ): MongoProjection[E, (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12)] =
    new ProductProjection(Seq(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12))

  def zip[E, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
    p4: MongoProjection[E, T4],
    p5: MongoProjection[E, T5],
    p6: MongoProjection[E, T6],
    p7: MongoProjection[E, T7],
    p8: MongoProjection[E, T8],
    p9: MongoProjection[E, T9],
    p10: MongoProjection[E, T10],
    p11: MongoProjection[E, T11],
    p12: MongoProjection[E, T12],
    p13: MongoProjection[E, T13],
  ): MongoProjection[E, (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13)] =
    new ProductProjection(Seq(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13))

  def zip[E, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
    p4: MongoProjection[E, T4],
    p5: MongoProjection[E, T5],
    p6: MongoProjection[E, T6],
    p7: MongoProjection[E, T7],
    p8: MongoProjection[E, T8],
    p9: MongoProjection[E, T9],
    p10: MongoProjection[E, T10],
    p11: MongoProjection[E, T11],
    p12: MongoProjection[E, T12],
    p13: MongoProjection[E, T13],
    p14: MongoProjection[E, T14],
  ): MongoProjection[E, (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14)] =
    new ProductProjection(Seq(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14))

  def zip[E, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
    p4: MongoProjection[E, T4],
    p5: MongoProjection[E, T5],
    p6: MongoProjection[E, T6],
    p7: MongoProjection[E, T7],
    p8: MongoProjection[E, T8],
    p9: MongoProjection[E, T9],
    p10: MongoProjection[E, T10],
    p11: MongoProjection[E, T11],
    p12: MongoProjection[E, T12],
    p13: MongoProjection[E, T13],
    p14: MongoProjection[E, T14],
    p15: MongoProjection[E, T15],
  ): MongoProjection[E, (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15)] =
    new ProductProjection(Seq(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15))

  def zip[E, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
    p4: MongoProjection[E, T4],
    p5: MongoProjection[E, T5],
    p6: MongoProjection[E, T6],
    p7: MongoProjection[E, T7],
    p8: MongoProjection[E, T8],
    p9: MongoProjection[E, T9],
    p10: MongoProjection[E, T10],
    p11: MongoProjection[E, T11],
    p12: MongoProjection[E, T12],
    p13: MongoProjection[E, T13],
    p14: MongoProjection[E, T14],
    p15: MongoProjection[E, T15],
    p16: MongoProjection[E, T16],
  ): MongoProjection[E, (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16)] =
    new ProductProjection(Seq(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16))

  def zip[E, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
    p4: MongoProjection[E, T4],
    p5: MongoProjection[E, T5],
    p6: MongoProjection[E, T6],
    p7: MongoProjection[E, T7],
    p8: MongoProjection[E, T8],
    p9: MongoProjection[E, T9],
    p10: MongoProjection[E, T10],
    p11: MongoProjection[E, T11],
    p12: MongoProjection[E, T12],
    p13: MongoProjection[E, T13],
    p14: MongoProjection[E, T14],
    p15: MongoProjection[E, T15],
    p16: MongoProjection[E, T16],
    p17: MongoProjection[E, T17],
  ): MongoProjection[E, (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17)] =
    new ProductProjection(Seq(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17))

  def zip[E, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
    p4: MongoProjection[E, T4],
    p5: MongoProjection[E, T5],
    p6: MongoProjection[E, T6],
    p7: MongoProjection[E, T7],
    p8: MongoProjection[E, T8],
    p9: MongoProjection[E, T9],
    p10: MongoProjection[E, T10],
    p11: MongoProjection[E, T11],
    p12: MongoProjection[E, T12],
    p13: MongoProjection[E, T13],
    p14: MongoProjection[E, T14],
    p15: MongoProjection[E, T15],
    p16: MongoProjection[E, T16],
    p17: MongoProjection[E, T17],
    p18: MongoProjection[E, T18],
  ): MongoProjection[E, (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18)] =
    new ProductProjection(Seq(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18))

  def zip[E, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
    p4: MongoProjection[E, T4],
    p5: MongoProjection[E, T5],
    p6: MongoProjection[E, T6],
    p7: MongoProjection[E, T7],
    p8: MongoProjection[E, T8],
    p9: MongoProjection[E, T9],
    p10: MongoProjection[E, T10],
    p11: MongoProjection[E, T11],
    p12: MongoProjection[E, T12],
    p13: MongoProjection[E, T13],
    p14: MongoProjection[E, T14],
    p15: MongoProjection[E, T15],
    p16: MongoProjection[E, T16],
    p17: MongoProjection[E, T17],
    p18: MongoProjection[E, T18],
    p19: MongoProjection[E, T19],
  ): MongoProjection[E, (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19)] =
    new ProductProjection(Seq(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19))

  def zip[E, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
    p4: MongoProjection[E, T4],
    p5: MongoProjection[E, T5],
    p6: MongoProjection[E, T6],
    p7: MongoProjection[E, T7],
    p8: MongoProjection[E, T8],
    p9: MongoProjection[E, T9],
    p10: MongoProjection[E, T10],
    p11: MongoProjection[E, T11],
    p12: MongoProjection[E, T12],
    p13: MongoProjection[E, T13],
    p14: MongoProjection[E, T14],
    p15: MongoProjection[E, T15],
    p16: MongoProjection[E, T16],
    p17: MongoProjection[E, T17],
    p18: MongoProjection[E, T18],
    p19: MongoProjection[E, T19],
    p20: MongoProjection[E, T20],
  ): MongoProjection[E, (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20)] =
    new ProductProjection(
      Seq(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20),
    )

  def zip[E, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
    p4: MongoProjection[E, T4],
    p5: MongoProjection[E, T5],
    p6: MongoProjection[E, T6],
    p7: MongoProjection[E, T7],
    p8: MongoProjection[E, T8],
    p9: MongoProjection[E, T9],
    p10: MongoProjection[E, T10],
    p11: MongoProjection[E, T11],
    p12: MongoProjection[E, T12],
    p13: MongoProjection[E, T13],
    p14: MongoProjection[E, T14],
    p15: MongoProjection[E, T15],
    p16: MongoProjection[E, T16],
    p17: MongoProjection[E, T17],
    p18: MongoProjection[E, T18],
    p19: MongoProjection[E, T19],
    p20: MongoProjection[E, T20],
    p21: MongoProjection[E, T21],
  ): MongoProjection[
    E,
    (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21),
  ] =
    new ProductProjection(
      Seq(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20, p21),
    )

  def zip[E, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22](
    p1: MongoProjection[E, T1],
    p2: MongoProjection[E, T2],
    p3: MongoProjection[E, T3],
    p4: MongoProjection[E, T4],
    p5: MongoProjection[E, T5],
    p6: MongoProjection[E, T6],
    p7: MongoProjection[E, T7],
    p8: MongoProjection[E, T8],
    p9: MongoProjection[E, T9],
    p10: MongoProjection[E, T10],
    p11: MongoProjection[E, T11],
    p12: MongoProjection[E, T12],
    p13: MongoProjection[E, T13],
    p14: MongoProjection[E, T14],
    p15: MongoProjection[E, T15],
    p16: MongoProjection[E, T16],
    p17: MongoProjection[E, T17],
    p18: MongoProjection[E, T18],
    p19: MongoProjection[E, T19],
    p20: MongoProjection[E, T20],
    p21: MongoProjection[E, T21],
    p22: MongoProjection[E, T22],
  ): MongoProjection[
    E,
    (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22),
  ] =
    new ProductProjection(
      Seq(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20, p21, p22),
    )
}

final class ProductProjection[E, T](componentProjections: Seq[MongoProjection[E, _]])(implicit applier: Applier[T])
    extends MongoProjection[E, T] {

  def projectionRefs: Set[MongoRef[E, _]] =
    componentProjections.iterator.flatMap(_.projectionRefs.iterator).toSet

  def showRecordId: Boolean =
    componentProjections.exists(_.showRecordId)

  def decodeFrom(doc: BsonDocument): T =
    applier.apply(componentProjections.map(_.decodeFrom(doc)))

  def on[E0](ref: MongoRef[E0, E]): MongoProjection[E0, T] =
    new ProductProjection(componentProjections.map(_.on(ref)))
}

object GenProjectionZippers {
  def main(args: Array[String]): Unit = {
    for n <- 2 to 22 do {
      val indices = 1 to n
      val tparams = indices.map(i => s"T$i").mkString(", ")
      val projectionParams = indices.map(i => s"p$i: MongoProjection[E, T$i],").mkString("\n")
      val projectionArgs = indices.map(i => s"p$i").mkString(", ")

      print(s"""
           |def zip[E, $tparams](
           |$projectionParams
           |): MongoProjection[E, ($tparams)] =
           |  new ProductProjection(Seq($projectionArgs))
           """.stripMargin)
    }
  }
}
