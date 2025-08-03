package com.avsystem.commons.macros

import scala.quoted.{Quotes, Expr, Type}

extension (using quotes: Quotes)(sym: quotes.reflect.Symbol)
  def isPublic: Boolean =
    import quotes.reflect.*
    !sym.isNoSymbol &&
    !(sym.flags.is(Flags.Private) || sym.flags.is(Flags.PrivateLocal) || sym.flags.is(Flags.Protected) ||
      sym.privateWithin.isDefined || sym.protectedWithin.isDefined)
  def methodAndFieldMembers: List[quotes.reflect.Symbol] =
    import quotes.reflect.*
    sym.methodMembers ++ sym.fieldMembers

extension (using quotes: Quotes)(obj: quotes.reflect.ClassDef.type)
  def newAnon[T: Type](
    methods: Map[String, List[List[quotes.reflect.Tree]] => Option[quotes.reflect.Term]] = Map.empty
    // fields: Map[String, Option[quotes.reflect.Term]] = Map.empty // TODO: support fields
  ): Expr[T] = {
    import quotes.reflect.*

    val name: String = Symbol.freshName("$anon")

    val fields = Map.empty[String, Option[Term]]

    val parents =
      if TypeRepr.of[T].classSymbol.exists(_.flags.is(Flags.Trait))
      then List(TypeTree.of[Object], TypeTree.of[T])
      else List(TypeTree.of[T])

    def methodDecls(cls: Symbol) = methods.keys.map { name =>
      Symbol.newMethod(
        parent = cls,
        name = name,
        tpe = This(cls).tpe.memberType(cls.methodMember(name).head),
        flags = Flags.Override | Flags.Synthetic,
        privateWithin = Symbol.noSymbol
      )
    }

    def fieldDecls(cls: Symbol) = fields.keys.map { name =>
      Symbol.newVal(
        parent = cls,
        name = name,
        tpe = This(cls).tpe.memberType(cls.fieldMember(name)),
        flags = Flags.Override | Flags.Synthetic,
        privateWithin = Symbol.noSymbol
      )
    }

    val cls = Symbol.newClass(
      Symbol.spliceOwner,
      name,
      parents.map(_.tpe),
      cls => (methodDecls(cls) ++ fieldDecls(cls)).toList,
      None
    )

    val methodDefs = methods.map { (name, defn) => DefDef(cls.methodMember(name).head, defn) }
    val fieldDefs = fields.map { (name, defn) => ValDef(cls.fieldMember(name), defn) }

    val clsDef = ClassDef(cls, parents, (methodDefs ++ fieldDefs).toList)

    val newCls =
      Typed(New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone, TypeTree.of[T])

    Block(clsDef :: Nil, newCls).asExprOf[T]
  }

def singleValueFor[T: Type](using quotes: Quotes): Option[Expr[T]] = {
  import quotes.reflect.*

  Expr
    .summon[scala.ValueOf[T]]
    .map(expr => Some('{ $expr.value }))
    .getOrElse {
      TypeRepr.of[T] match
        case ThisType(tpe) =>
          Some(This(tpe.typeSymbol).asExprOf[T])
        case _ =>
          None
    }
}

transparent inline def getCompanionOf[T]: Any = ${ getCompanionOfImpl[T] }

def getCompanionOfImpl[T: Type](using quotes: Quotes): Expr[?] =
  import quotes.reflect.*
  companionOf[T].getOrElse {
    report.errorAndAbort(s"Could not find companion for type ${Type.show[T]}")
  }

def companionOf[T: Type](using quotes: Quotes): Option[Expr[?]] = {
  import quotes.reflect.*

  val companionSymbol = TypeRepr.of[T].classSymbol.flatMap {
    case sym if !sym.companionModule.isNoSymbol =>
      Some(sym.companionModule)
    case _ => None
  }

  companionSymbol.flatMap { companion =>
    companion.termRef.asType match
      case '[t] => singleValueFor[t]
  }
}

def isFirstListVarargs(using quotes: Quotes)(meth: quotes.reflect.Symbol): Boolean =
  import quotes.reflect.*
  meth.paramSymss.headOption.flatMap(_.lastOption).exists(_ == defn.RepeatedParamClass)

//todo quotes.reflect.MethodType may be wrong
def matchingApplyUnapply(using
  quotes: Quotes
)(tpe: quotes.reflect.TypeRepr, applySig: quotes.reflect.MethodType, unapplySig: quotes.reflect.MethodType): Boolean = {
  applySig.resType =:= tpe && (unapplySig.paramTypes match {
    case List(unapplyParam) if unapplyParam =:= tpe =>
      isCorrectUnapply(unapplySig.resType, applySig.paramTypes)
    case _ => false
  })
}

//todo: currenly only Scala 2 unapply is supported
def isCorrectUnapply(using quotes: Quotes)(
  tpe: quotes.reflect.TypeRepr,
  applyParams: List[quotes.reflect.TypeRepr],
  elemAdjust: quotes.reflect.TypeRepr => quotes.reflect.TypeRepr = identity[quotes.reflect.TypeRepr]
): Boolean = {
  import quotes.reflect.*

  def hasIsEmpty = tpe.typeSymbol.methodAndFieldMembers.exists(m =>
    m.name == "isEmpty" && isParameterless(m) && m.typeRef =:= TypeRepr.of[Boolean]
  )

  def hasProperGet(resultTypeCondition: TypeRepr => Boolean): Boolean =
    tpe.typeSymbol.methodAndFieldMembers.exists(m =>
      m.name == "get" && isParameterless(m) && resultTypeCondition(m.typeRef)
    )

  applyParams match {
    case Nil =>
      tpe =:= TypeRepr.of[Boolean]
    case List(singleParam) =>
      hasIsEmpty && hasProperGet(resType => elemAdjust(resType) =:= singleParam)
    case params =>
      hasIsEmpty && hasProperGet { resType =>
        val elemTypes = Iterator
          .range(1, 22)
          .map { i =>
            resType.typeSymbol.methodAndFieldMembers
              .filter(_.name == s"_$i")
              .find(sig => sig.typeRef.typeArgs == Nil && sig.paramSymss == Nil)
              .map(sig => elemAdjust(sig.typeRef))
              .getOrElse(null.asInstanceOf[TypeRepr])
          }
          .takeWhile(_ != null)
          .toList

        def check(params: List[TypeRepr], elemTypes: List[TypeRepr]): Boolean = (params, elemTypes) match
          case (Nil, Nil) => true
          case (p :: prest, et :: etrest) =>
            p =:= et && check(prest, etrest)
          case _ => false

        check(params, elemTypes)
      }
  }
}

def isParameterless(using quotes: Quotes)(signature: quotes.reflect.Symbol): Boolean =
  signature.paramSymss.flatten == Nil
