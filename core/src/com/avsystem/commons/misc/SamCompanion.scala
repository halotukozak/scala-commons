package com.avsystem.commons
package misc

import com.avsystem.commons.misc.SamCompanion.ValidSam
import scala.quoted.Type
import scala.quoted.Quotes
import scala.quoted.Expr
import scala.annotation.tailrec
import com.avsystem.commons.macros.isPublic

abstract class SamCompanion[T, F](using ValidSam[T, F]) {
  inline def apply(inline fun: F): T = Sam[T](fun)
}

object SamCompanion {
  sealed trait ValidSam[T, F]
  object ValidSam {
    inline given isValidSam[T, F]: ValidSam[T, F] = ${ isValidSamImpl[T, F] }

    def isValidSamImpl[T: Type, F: Type](using quotes: Quotes): Expr[ValidSam[T, F]] = {
      import quotes.reflect.*
      given Printer[TypeRepr] = Printer.TypeReprCode

      val targetType = TypeRepr.of[T].widen

      val targetSymbol = targetType.classSymbol
        .filter(sym => sym.flags.is(Flags.Abstract) || sym.flags.is(Flags.Trait))
        .getOrElse(report.errorAndAbort(s"${targetType.show} is neither an abstract class nor trait"))

      targetSymbol.methodMembers.iterator.filter { m =>
        m.flags.is(Flags.Deferred)
        && m.flags.is(Flags.Method)
        && !m.flags.is(Flags.FieldAccessor)
        && m.isPublic
        && targetType.memberType(m).typeArgs.isEmpty
      }.toList match
        case m :: Nil =>
          def extractTypes(tpe: TypeRepr): (List[List[TypeRepr]], TypeRepr) =
            @tailrec def loop(tpe: TypeRepr)(acc: List[List[TypeRepr]]): (List[List[TypeRepr]], TypeRepr) =
              tpe match
                case MethodType(_, params, result) =>
                  loop(result)(params :: acc)
                case ByNameType(result) =>
                  loop(result)(acc)
                case result =>
                  (acc.reverse, result)
            loop(tpe)(Nil)

          val (argTypess, resultTpe) = extractTypes(targetType.memberType(m))

          val byName = argTypess == List(Nil) && TypeRepr.of[F] <:< resultTpe

          val finalResultType =
            if (resultTpe =:= TypeRepr.of[Unit]) TypeRepr.of[Any]
            else resultTpe

          val requiredFunTpe = argTypess.foldRight(finalResultType) { (argTypes, resultType) =>
            val funSym = defn.FunctionClass(argTypes.size)
            funSym.typeRef.appliedTo(argTypes :+ resultType)
          }

          if (!byName && !(TypeRepr.of[F] <:< requiredFunTpe)) {
            report.errorAndAbort {
              val expected =
                s"expected ${if (argTypess == List(Nil)) s"${finalResultType.show} or " else ""}${requiredFunTpe.show}"

              s"${TypeRepr.of[F].widen.show} does not match signature of $m in ${targetType.show}: $expected"
            }
          }
          '{ new ValidSam[T, F] {} }
        case _ =>
          report.errorAndAbort(s"Target trait/class must have exactly one public, abstract, non-generic method")
    }

  }
}
