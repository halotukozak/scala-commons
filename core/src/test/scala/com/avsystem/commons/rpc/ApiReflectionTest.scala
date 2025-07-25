package com.avsystem.commons
package rpc

import com.avsystem.commons.meta._
import com.avsystem.commons.misc.TypeString
import org.scalatest.funsuite.AnyFunSuite

class cool extends StaticAnnotation

case class ApiInfo[T](
  @infer ts: TypeString[T],
  @multi @mangleOverloads @rpcMethodMetadata methods: List[MethodInfo[?]]
) extends TypedMetadata[T] {
  def repr = s"${ts.value} {${methods.map(m => "\n  " + m.repr).mkString}\n}"
}
object ApiInfo extends ApiMetadataCompanion[ApiInfo]

case class MethodInfo[T](
  @reifyName name: String,
  @reifyName(useRawName = true) rawName: String,
  @reifyFlags flags: MethodFlags,
  @isAnnotated[cool] cool: Boolean,
  @reifyParamListCount paramListCount: Int,
  @multi @rpcTypeParamMetadata typeParams: List[TypeParamInfo],
  @multi @rpcParamMetadata params: List[ParamInfo[?]],
  @forTypeParams @infer resultTs: List[TypeString[?]] => TypeString[T]
) extends TypedMetadata[T] {

  val paramLists: List[List[ParamInfo[?]]] = {
    def extract(listIdx: Int, params: List[ParamInfo[?]]): List[List[ParamInfo[?]]] =
      if (listIdx == paramListCount) Nil
      else params.span(_.pos.indexOfList == listIdx) match {
        case (nextList, rest) =>
          nextList :: extract(listIdx + 1, rest)
      }
    extract(0, params)
  }

  def repr: String = {
    val typeParamsRepr = typeParams.map(_.name).mkStringOrEmpty("[", ", ", "]")
    val paramsRepr = paramLists.map(_.map(_.repr(typeParams)).mkString("(", ", ", ")")).mkString
    val resultTypeString = resultTs(typeParams.map(_.typeString))
    val coolRepr = if(cool) "@cool " else ""
    s"$coolRepr${flags.baseDecl} $name$typeParamsRepr$paramsRepr: $resultTypeString"
  }
}

case class TypeParamInfo(
  @reifyName name: String
) {
  def typeString: TypeString[?] = new TypeString(name)
}

case class ParamInfo[T](
  @reifyName name: String,
  @reifyPosition pos: ParamPosition,
  @reifyFlags flags: ParamFlags,
  @forTypeParams @infer ts: List[TypeString[?]] => TypeString[T]
) extends TypedMetadata[T] {
  def repr(tparams: List[TypeParamInfo]): String = {
    val implicitMod = if (pos.indexInList == 0 && flags.isImplicit) "implicit " else ""
    s"$implicitMod$name: ${ts(tparams.map(_.typeString))}"
  }
}

class SimpleApi {
  final val Thing = "fuu"
  @cool lazy val CoolThing = -1
  def noParamLists: Int = 42
  def noParams(): String = ""
  def multiParamLists(int: Int)(str: String)(): Double = int.toDouble
  def takesImplicits(int: Int)(implicit ord: Ordering[Int], moar: DummyImplicit): String = int.toString
  def takesTypeArgs[A, B](as: List[A], bs: Set[B]): Map[A, B] = Map.empty
  @ignore def completelyIgnoredMethod(int: Int, string: String): Unit = ()
}

class ApiReflectionTest extends AnyFunSuite {
  test("String API JDK11") {
    assume(System.getProperty("java.specification.version") == "11")
    assert(ApiInfo.materialize[String].repr ==
      """String {
        |  def length(): Int
        |  def isEmpty(): Boolean
        |  def charAt(x$1: Int): Char
        |  def codePointAt(x$1: Int): Int
        |  def codePointBefore(x$1: Int): Int
        |  def codePointCount(x$1: Int, x$2: Int): Int
        |  def offsetByCodePoints(x$1: Int, x$2: Int): Int
        |  def getChars(x$1: Int, x$2: Int, x$3: Array[Char], x$4: Int): Unit
        |  def getBytes(x$1: Int, x$2: Int, x$3: Array[Byte], x$4: Int): Unit
        |  def getBytes(x$1: String): Array[Byte]
        |  def getBytes(x$1: java.nio.charset.Charset): Array[Byte]
        |  def getBytes(): Array[Byte]
        |  def contentEquals(x$1: StringBuffer): Boolean
        |  def contentEquals(x$1: CharSequence): Boolean
        |  def equalsIgnoreCase(x$1: String): Boolean
        |  def compareTo(x$1: String): Int
        |  def compareToIgnoreCase(x$1: String): Int
        |  def regionMatches(x$1: Int, x$2: String, x$3: Int, x$4: Int): Boolean
        |  def regionMatches(x$1: Boolean, x$2: Int, x$3: String, x$4: Int, x$5: Int): Boolean
        |  def startsWith(x$1: String, x$2: Int): Boolean
        |  def startsWith(x$1: String): Boolean
        |  def endsWith(x$1: String): Boolean
        |  def indexOf(x$1: Int): Int
        |  def indexOf(x$1: Int, x$2: Int): Int
        |  def lastIndexOf(x$1: Int): Int
        |  def lastIndexOf(x$1: Int, x$2: Int): Int
        |  def indexOf(x$1: String): Int
        |  def indexOf(x$1: String, x$2: Int): Int
        |  def lastIndexOf(x$1: String): Int
        |  def lastIndexOf(x$1: String, x$2: Int): Int
        |  def substring(x$1: Int): String
        |  def substring(x$1: Int, x$2: Int): String
        |  def subSequence(x$1: Int, x$2: Int): CharSequence
        |  def concat(x$1: String): String
        |  def replace(x$1: Char, x$2: Char): String
        |  def matches(x$1: String): Boolean
        |  def contains(x$1: CharSequence): Boolean
        |  def replaceFirst(x$1: String, x$2: String): String
        |  def replaceAll(x$1: String, x$2: String): String
        |  def replace(x$1: CharSequence, x$2: CharSequence): String
        |  def split(x$1: String, x$2: Int): Array[String]
        |  def split(x$1: String): Array[String]
        |  def toLowerCase(x$1: java.util.Locale): String
        |  def toLowerCase(): String
        |  def toUpperCase(x$1: java.util.Locale): String
        |  def toUpperCase(): String
        |  def trim(): String
        |  def strip(): String
        |  def stripLeading(): String
        |  def stripTrailing(): String
        |  def isBlank(): Boolean
        |  def lines(): java.util.stream.Stream[String]
        |  def chars(): java.util.stream.IntStream
        |  def codePoints(): java.util.stream.IntStream
        |  def toCharArray(): Array[Char]
        |  def intern(): String
        |  def repeat(x$1: Int): String
        |  final def +(x$1: Any): String
        |}""".stripMargin
    )
  }

  test("String API JDK17") {
    assume(System.getProperty("java.specification.version") == "17")
    assert(ApiInfo.materialize[String].repr ==
      """String {
        |  def length(): Int
        |  def isEmpty(): Boolean
        |  def charAt(x$1: Int): Char
        |  def codePointAt(x$1: Int): Int
        |  def codePointBefore(x$1: Int): Int
        |  def codePointCount(x$1: Int, x$2: Int): Int
        |  def offsetByCodePoints(x$1: Int, x$2: Int): Int
        |  def getChars(x$1: Int, x$2: Int, x$3: Array[Char], x$4: Int): Unit
        |  def getBytes(x$1: Int, x$2: Int, x$3: Array[Byte], x$4: Int): Unit
        |  def getBytes(x$1: String): Array[Byte]
        |  def getBytes(x$1: java.nio.charset.Charset): Array[Byte]
        |  def getBytes(): Array[Byte]
        |  def contentEquals(x$1: StringBuffer): Boolean
        |  def contentEquals(x$1: CharSequence): Boolean
        |  def equalsIgnoreCase(x$1: String): Boolean
        |  def compareTo(x$1: String): Int
        |  def compareToIgnoreCase(x$1: String): Int
        |  def regionMatches(x$1: Int, x$2: String, x$3: Int, x$4: Int): Boolean
        |  def regionMatches(x$1: Boolean, x$2: Int, x$3: String, x$4: Int, x$5: Int): Boolean
        |  def startsWith(x$1: String, x$2: Int): Boolean
        |  def startsWith(x$1: String): Boolean
        |  def endsWith(x$1: String): Boolean
        |  def indexOf(x$1: Int): Int
        |  def indexOf(x$1: Int, x$2: Int): Int
        |  def lastIndexOf(x$1: Int): Int
        |  def lastIndexOf(x$1: Int, x$2: Int): Int
        |  def indexOf(x$1: String): Int
        |  def indexOf(x$1: String, x$2: Int): Int
        |  def lastIndexOf(x$1: String): Int
        |  def lastIndexOf(x$1: String, x$2: Int): Int
        |  def substring(x$1: Int): String
        |  def substring(x$1: Int, x$2: Int): String
        |  def subSequence(x$1: Int, x$2: Int): CharSequence
        |  def concat(x$1: String): String
        |  def replace(x$1: Char, x$2: Char): String
        |  def matches(x$1: String): Boolean
        |  def contains(x$1: CharSequence): Boolean
        |  def replaceFirst(x$1: String, x$2: String): String
        |  def replaceAll(x$1: String, x$2: String): String
        |  def replace(x$1: CharSequence, x$2: CharSequence): String
        |  def split(x$1: String, x$2: Int): Array[String]
        |  def split(x$1: String): Array[String]
        |  def toLowerCase(x$1: java.util.Locale): String
        |  def toLowerCase(): String
        |  def toUpperCase(x$1: java.util.Locale): String
        |  def toUpperCase(): String
        |  def trim(): String
        |  def strip(): String
        |  def stripLeading(): String
        |  def stripTrailing(): String
        |  def isBlank(): Boolean
        |  def lines(): java.util.stream.Stream[String]
        |  def indent(x$1: Int): String
        |  def stripIndent(): String
        |  def translateEscapes(): String
        |  def transform[R](x$1: java.util.function.Function[_ >: String, _ <: R]): R
        |  def chars(): java.util.stream.IntStream
        |  def codePoints(): java.util.stream.IntStream
        |  def toCharArray(): Array[Char]
        |  def formatted(x$1: Seq[AnyRef]): String
        |  def intern(): String
        |  def repeat(x$1: Int): String
        |  def describeConstable(): java.util.Optional[String]
        |  def resolveConstantDesc(x$1: java.lang.invoke.MethodHandles.Lookup): String
        |  final def +(x$1: Any): String
        |}""".stripMargin
    )
  }

  test("Simple API") {
    assert(ApiInfo.materialize[SimpleApi].repr ==
      """com.avsystem.commons.rpc.SimpleApi {
        |  final val Thing: String
        |  @cool lazy val CoolThing: Int
        |  def noParamLists: Int
        |  def noParams(): String
        |  def multiParamLists(int: Int)(str: String)(): Double
        |  def takesImplicits(int: Int)(implicit ord: Ordering[Int], moar: DummyImplicit): String
        |  def takesTypeArgs[A, B](as: List[A], bs: Set[B]): Map[A, B]
        |}""".stripMargin)
  }
}
