package com.avsystem.commons
package misc

import scala.quoted.{Expr, Quotes}

/**
  * Macro-materialized implicit value that provides information about callsite source file position.
  * It can be used in runtime for logging and debugging purposes.
  * Similar to Scalactic's `Position`, but contains more information.
  */
case class SourceInfo(
  filePath: String,
  fileName: String,
  offset: Int,
  line: Int,
  column: Int,
  lineContent: String,
  enclosingSymbols: List[String]
) {
  override def equals(obj: Any): Boolean = obj match {
    case otherInfo: SourceInfo => filePath ==
      otherInfo.filePath && offset == otherInfo.offset
    case _ => false
  }

  override def hashCode: Int =
    (filePath, offset).hashCode
}

object SourceInfo {
  def apply()(using si: SourceInfo): SourceInfo = si

  import com.avsystem.commons.macros.error

  inline given here: SourceInfo = ${ hereImpl }
  private def hereImpl(using quotes: Quotes): Expr[SourceInfo] = {
    import quotes.reflect.*

    val pos = Position.ofMacroExpansion

    val ownerChain =
      Iterator
        .iterate(Symbol.spliceOwner)(_.owner)
        .takeWhile(_ != Symbol.noSymbol)

    val filePath = Expr(pos.sourceFile.getJPath.get.toString)
    val fileName = Expr(pos.sourceFile.name)
    val offset = Expr(pos.start)
    val line = Expr(pos.startLine + 1)
    val column = Expr(pos.startColumn + 1)
    val lineContent = Expr(pos.sourceFile.content.get.split("\n")(pos.startLine))
    val enclosingSymbols = Expr(ownerChain.filterNot(sym => sym.flags.is(Flags.Macro) || sym.name == "<root>").map(_.name).toList)

    '{ SourceInfo($filePath, $fileName, $offset, $line, $column, $lineContent, $enclosingSymbols) }
  }
}
