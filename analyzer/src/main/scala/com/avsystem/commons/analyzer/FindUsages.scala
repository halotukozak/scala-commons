package com.avsystem.commons
package analyzer

import scala.tools.nsc.Global

class FindUsages(g: Global) extends AnalyzerRule(g, "findUsages") {

  import global.*

  lazy val rejectedSymbols: Set[String] =
    if argument == null then Set.empty else argument.split(";").toSet

  override def analyze(unit: CompilationUnit): Unit = if rejectedSymbols.nonEmpty then {
    unit.body.foreach { tree =>
      if tree.symbol != null && rejectedSymbols.contains(tree.symbol.fullName) then {
        report(tree.pos, s"found usage of ${tree.symbol.fullName}")
      }
    }
  }
}
