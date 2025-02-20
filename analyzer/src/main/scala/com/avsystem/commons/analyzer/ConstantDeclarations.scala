package com.avsystem.commons
package analyzer

import scala.tools.nsc.Global

class ConstantDeclarations(g: Global) extends AnalyzerRule(g, "constantDeclarations", Level.Off) {

  import global.*

  def analyze(unit: CompilationUnit): Unit = unit.body.foreach {
    case t @ ValDef(_, name, tpt, rhs) if t.symbol.hasGetter && t.symbol.owner.isEffectivelyFinal =>
      val getter = t.symbol.getterIn(t.symbol.owner)
      if getter.isPublic && getter.isStable && getter.overrides.isEmpty then {
        val constantValue = rhs.tpe match {
          case ConstantType(_) => true
          case _ => false
        }

        def doReport(msg: String): Unit =
          report(t.pos, msg, site = t.symbol)

        val firstChar = name.toString.charAt(0)
        if constantValue && (firstChar.isLower || !getter.isFinal) then {
          doReport("a literal-valued constant should be declared as a `final val` with an UpperCamelCase name")
        }
        if !constantValue && firstChar.isUpper && !getter.isFinal then {
          doReport("a constant with UpperCamelCase name should be declared as a `final val`")
        }
        if getter.isFinal && constantValue && !(tpt.tpe =:= rhs.tpe) then {
          doReport("a constant with a literal value should not have an explicit type annotation")
        }
      }
    case _ =>
  }
}
