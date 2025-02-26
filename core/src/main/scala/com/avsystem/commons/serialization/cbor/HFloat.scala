package com.avsystem.commons
package serialization.cbor

object HFloat {
  final val PositiveInfinity = new HFloat(0x7c00.toShort)
  final val NegativeInfinity = new HFloat(0xfc00.toShort)
  final val NaN = new HFloat(0x7e00.toShort)
  final val Zero = new HFloat(0x0000.toShort)
  final val NegativeZero = new HFloat(0x8000.toShort)
  final val MinValue = new HFloat(0x0001.toShort)
  final val MaxValue = new HFloat(0x7bff.toShort)
  final val Epsilon = new HFloat(0x1400.toShort)

  final val MinExponent = -14
  final val MaxExponent = 15

  private final val FloatNegativeZero = java.lang.Float.intBitsToFloat(0x80000000)

  private final val SignShift = 15
  private final val ExponentShift = 10
  private final val ExponentMask = 0x1f
  private final val SignificandMask = 0x3ff
  private final val ExponentBias = 15

  private final val FloatSignShift = 31
  private final val FloatExponentShift = 23
  private final val FloatExponentMask = 0xff
  private final val FloatSignificandMask = 0x7fffff
  private final val FloatExponentBias = 127

  private final val FloatExpExcess = FloatExponentShift - ExponentShift
  private final val RoundingBit = 1 << (FloatExpExcess - 1)
  // if Float significand is greater or equal this value, exponent will be incremented during rounding
  private final val ExpRoundingMask = 0x7ff000

  def fromFloat(float: Float): HFloat =
    if float.isNaN then NaN
    else {
      val fbits = java.lang.Float.floatToIntBits(float)
      val neg = (fbits >>> FloatSignShift) != 0
      float match {
        case 0.0f => if neg then NegativeZero else Zero
        case Float.PositiveInfinity => PositiveInfinity
        case Float.NegativeInfinity => NegativeInfinity
        case _ =>
          val signBit = (if neg then 1 else 0) << SignShift
          var exp = ((fbits >>> FloatExponentShift) & FloatExponentMask) - FloatExponentBias
          var significand = (fbits & FloatSignificandMask) >>> FloatExpExcess

          if (fbits & ExpRoundingMask) == ExpRoundingMask then { // round up after truncation
            exp += 1
            significand = 0
          } else if (fbits & RoundingBit) != 0 then {
            significand += 1
          }

          if exp >= MinExponent && exp <= MaxExponent then
            new HFloat((signBit | ((exp + ExponentBias) << ExponentShift) | significand).toShort)
          else if exp > MaxExponent then if neg then NegativeInfinity else PositiveInfinity
          else if exp >= MinExponent - ExponentShift then { // subnormal half-precision float
            val sshift = MinExponent - exp
            var subnormalSignificand = ((1 << ExponentShift) | significand) >>> sshift
            if (significand & (1 << (sshift - 1))) != 0 then { // round up after truncation
              subnormalSignificand += 1
            }
            new HFloat((signBit | subnormalSignificand).toShort)
          } else if neg then NegativeZero
          else Zero
      }
    }
}

/**
 * IEEE 754 half-precision floating point number [[https://en.wikipedia.org/wiki/Half-precision_floating-point_format]]
 *
 * This class only implements conversion to and from standard `Float` (single precision floating point). Arithmetic is
 * not implemented.
 */
final class HFloat(val raw: Short) extends AnyVal {

  import HFloat.*

  private def bits: Int =
    raw.toInt & 0xffff

  private def exponentBits: Int =
    (bits >>> ExponentShift) & ExponentMask

  private def exponent: Int =
    exponentBits - ExponentBias

  private def significand: Int =
    bits & SignificandMask

  def isNaN: Boolean = exponentBits == ExponentMask && significand != 0

  def isInfinity: Boolean = exponentBits == ExponentMask && significand == 0

  def isPosInfinity: Boolean = this == PositiveInfinity

  def isNegInfinity: Boolean = this == NegativeInfinity

  def toFloat: Float =
    if isNaN then Float.NaN
    else
      this match {
        case Zero => 0
        case NegativeZero => FloatNegativeZero
        case PositiveInfinity => Float.PositiveInfinity
        case NegativeInfinity => Float.NegativeInfinity
        case _ if exponentBits == 0 => // subnormal half-precision float
          val expadd = Integer.numberOfLeadingZeros(bits << (Integer.SIZE - ExponentShift)) + 1
          mkNormalFloat((bits >>> SignShift) != 0, MinExponent - expadd, (bits << expadd) & SignificandMask)
        case _ =>
          mkNormalFloat((bits >>> SignShift) != 0, exponent, significand)
      }

  private def mkNormalFloat(negative: Boolean, exponent: Int, hpSignificand: Int): Float = {
    val signBits = (if negative then 1 else 0) << FloatSignShift
    val exponentBits = (exponent + FloatExponentBias) << FloatExponentShift
    val significandBits = hpSignificand << FloatExpExcess
    java.lang.Float.intBitsToFloat(signBits | exponentBits | significandBits)
  }
}
