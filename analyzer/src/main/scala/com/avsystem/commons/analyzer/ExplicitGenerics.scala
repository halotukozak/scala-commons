package com.avsystem.commons
package analyzer

import scala.tools.nsc.Global

class ExplicitGenerics(g: Global) extends AnalyzerRule(g, "explicitGenerics") {

  import global.*

  lazy val explicitGenericsAnnotTpe = classType("com.avsystem.commons.annotation.explicitGenerics")

  def analyze(unit: CompilationUnit) = if explicitGenericsAnnotTpe != NoType then {
    def requiresExplicitGenerics(sym: Symbol): Boolean =
      sym != NoSymbol && (sym :: sym.overrides).flatMap(_.annotations).exists(_.tree.tpe <:< explicitGenericsAnnotTpe)

    def analyzeTree(tree: Tree): Unit = analyzer.macroExpandee(tree) match {
      case `tree` | EmptyTree =>
        tree match {
          case t @ TypeApply(pre, args) if requiresExplicitGenerics(pre.symbol) =>
            val inferredTypeParams = args.forall {
              case tt: TypeTree => tt.original == null || tt.original == EmptyTree
              case _ => false
            }
            if inferredTypeParams then {
              report(t.pos, s"${pre.symbol} requires that its type arguments are explicit (not inferred)")
            }
          case _ =>
        }
        tree.children.foreach(analyzeTree)
      case prevTree =>
        analyzeTree(prevTree)
    }
    analyzeTree(unit.body)
  }
}
