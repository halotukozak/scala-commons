package com.avsystem.commons
package analyzer

import scala.tools.nsc.Global

class ThrowableObjects(g: Global) extends AnalyzerRule(g, "throwableObjects", Level.Warn) {

  import global.*

  private lazy val throwableTpe = typeOf[Throwable]
  private lazy val throwableSym = throwableTpe.dealias.typeSymbol

  def analyze(unit: CompilationUnit): Unit = unit.body.foreach {
    case md: ModuleDef =>
      val tpe = md.symbol.typeSignature
      def fillInStackTraceSym: Symbol =
        tpe.member(TermName("fillInStackTrace")).alternatives.find(_.paramLists == List(Nil)).get

      if tpe <:< throwableTpe && fillInStackTraceSym.owner == throwableSym then {
        report(md.pos, "objects should never extend Throwable unless they have no stack trace")
      }
    case _ =>
  }
}
