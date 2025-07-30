package com.avsystem.commons.macros

import scala.quoted.{Quotes, Expr, Type}

extension (using quotes: Quotes)(sym: quotes.reflect.Symbol)
  def isPublic: Boolean =
    import quotes.reflect.*
    !sym.isNoSymbol &&
    !(sym.flags.is(Flags.Private) || sym.flags.is(Flags.PrivateLocal) || sym.flags.is(Flags.Protected) ||
      sym.privateWithin.isDefined || sym.protectedWithin.isDefined)

extension (using quotes: Quotes)(obj: quotes.reflect.ClassDef.type)
  def newAnon[T: Type](
    methodNames: List[String],
    defns: List[List[List[quotes.reflect.Tree]] => Option[quotes.reflect.Term]]
  ): Expr[T] = {
    import quotes.reflect.*

    val name: String = Symbol.freshName("$anon")

    val parents =
      if TypeRepr.of[T].classSymbol.exists(_.flags.is(Flags.Trait))
      then List(TypeTree.of[Object], TypeTree.of[T])
      else List(TypeTree.of[T])

    def decls(cls: Symbol): List[Symbol] = methodNames.map { name =>
      Symbol.newMethod(
        parent = cls,
        name = name,
        tpe = This(cls).tpe.memberType(cls.methodMember(name).head),
        flags = Flags.Override | Flags.Synthetic,
        privateWithin = Symbol.noSymbol
      )
    }

    val cls = Symbol.newClass(Symbol.spliceOwner, name, parents.map(_.tpe), decls, None)

    val body = defns.zip(methodNames).map { (defn, name) =>
      DefDef(cls.methodMember(name).head, defn)
    }

    val clsDef = ClassDef(cls, parents, body)

    val newCls =
      Typed(New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone, TypeTree.of[T])

    Block(clsDef :: Nil, newCls).asExprOf[T]
  }
