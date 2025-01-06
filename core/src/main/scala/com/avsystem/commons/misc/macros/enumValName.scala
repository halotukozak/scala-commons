package com.avsystem.commons
package misc.macros

import misc.ValueEnum

import scala.deriving.Mirror

inline def enumValName[Value, ValName](valNameConstructor: String => ValName) =
  ${ enumValNameImpl[Value, ValName]('{ valNameConstructor }) }
def enumValNameImpl[Value: Type, ValName: Type](valNameConstructor: Expr[String => ValName])(using quotes: Quotes) = {
  import quotes.reflect.*
  def omitAnonClass(owner: Symbol): Symbol =
    if owner.isClassConstructor && owner.owner.isAnonymousClass then
      owner.owner.owner
    else owner

  val owner = omitAnonClass(Symbol.spliceOwner.owner)

  val valid = owner.isTerm &&
    owner.isValDef &&
    owner.flags.is(Flags.Final) &&
    !owner.flags.is(Flags.Lazy) &&
    //    owner.owner == Symbol.spliceOwner.owner.owner && //todo: nested objects should not be allowed
    owner.isPublic /*getter */ &&
    owner.typeRef <:< TypeRepr.of[Value]

  if !valid then
    report.errorAndAbort("ValueEnum must be assigned to a public, final, non-lazy val in its companion object " +
      "with explicit `Value` type annotation, e.g. `final val MyEnumValue: Value = new MyEnumClass")

  val name = Expr(owner.name.toString)

  '{ $valNameConstructor($name) }
}

