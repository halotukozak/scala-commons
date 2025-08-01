package com.avsystem.commons
package misc

import com.avsystem.commons.annotation.MayBeReplacedWith
import scala.quoted.Type
import scala.quoted.Expr
import scala.quoted.Quotes
import com.avsystem.commons.macros._
import com.avsystem.commons.misc.SamCompanion.ValidSam

object Sam {

  /** Implements a single abstract method trait/class `T` using passed function or expression as implementation of the
    * sole abstract method. The argument passed may be either a function that must match the signature of the abstract
    * method or - in case the method does not take any arguments - an expression which will be returned in the
    * implementation of abstract method (as if the expression was passed as by-name parameter).
    */
  inline def apply[T](inline fun: Any): T = ${ applyImpl[T]('{ fun }) }

  def applyImpl[T: Type](value: Expr[Any])(using quotes: Quotes): Expr[T] = {
    import quotes.reflect.*
    value match
      case '{ $value: f } =>
        val method = TypeRepr
          .of[T]
          .typeSymbol
          .methodMembers
          .filter(_.flags.is(Flags.Deferred)) match
          case m :: Nil => m
          case list => list.find(_.allOverriddenSymbols.forall(_.flags.is(Flags.Deferred))).get

        val returnTpe =
          TypeRepr.of[T].memberType(method) match
            case MethodType(_, _, r) => r
            case ByNameType(r) => r
            case r => r

        val finalResultType =
          if (returnTpe =:= TypeRepr.of[Unit]) TypeRepr.of[Any]
          else returnTpe

        def defn(rhsFn: List[List[Tree]]) = Some {
          val byName = rhsFn == List(Nil) && TypeRepr.of[f] <:< finalResultType
          if byName then value.asTerm
          else
            val argss = rhsFn.map(_.map(tree => Ref(tree.symbol)))
            argss
              .foldLeft(value.asTerm) { (term, args) =>
                Select.unique(term, "apply").appliedToArgs(args)
              }
        }.map { res =>
          if returnTpe =:= TypeRepr.of[Unit]
          then Block(res :: Nil, Literal(UnitConstant()))
          else res
        }

        val anonDef = ClassDef.newAnon[T](method.name :: Nil, defn :: Nil)

        '{
          ValidSam.isValidSam[T, f]
          $anonDef
        }
  }

}
