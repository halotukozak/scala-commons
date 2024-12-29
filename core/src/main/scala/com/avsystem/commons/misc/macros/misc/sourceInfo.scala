package com.avsystem.commons
package misc.macros.misc

import misc.SourceInfo

import scala.quoted.{Expr, Quotes}

def sourceInfo(using quotes: Quotes): Expr[SourceInfo] = {
  import quotes.reflect.*

  def enclosingSymName(sym: Symbol) = if sym.isTerm then sym.name else sym.name

  val pos = Position.ofMacroExpansion

  val ownerChain =
    Iterator
      .iterate(Symbol.spliceOwner)(_.owner)
      .takeWhile(_ != Symbol.noSymbol)

  val ownerNames =
    ownerChain
      .map(enclosingSymName)
      .filterNot(name => name == "macro" || name == "<root>")
      .toList

  '{
    SourceInfo(
      ${ Expr(pos.sourceFile.getJPath.toString) },
      ${ Expr(pos.sourceFile.name) },
      ${ Expr(pos.start) },
      ${ Expr(pos.startLine + 1) },
      ${ Expr(pos.startColumn + 1) },
      ${ Expr(pos.sourceFile.content.get.split("\n")(pos.startLine)) },
      ${ Expr(ownerNames) },
    )
  }
}