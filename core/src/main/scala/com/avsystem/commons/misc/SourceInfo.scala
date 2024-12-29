package com.avsystem.commons
package misc

import misc.macros.misc.sourceInfo

/** Macro-materialized givenue that provides information about callsite source file position. It can be used in
 * runtime for logging and debugging purposes. Similar to Scalactic's `Position`, but contains more information.
 */

final case class SourceInfo(
  filePath: String,
  fileName: String,
  offset: Int,
  line: Int,
  column: Int,
  lineContent: String,
  enclosingSymbols: List[String],
) {
  override def equals(obj: Any): Boolean = obj match
    case otherInfo: SourceInfo =>
      filePath == otherInfo.filePath && offset == otherInfo.offset
    case _ => false

  override def hashCode: Int = (filePath, offset).hashCode
}

object SourceInfo:
  def apply()(using si: SourceInfo): SourceInfo = si

  inline given here: SourceInfo = ${ sourceInfo }

