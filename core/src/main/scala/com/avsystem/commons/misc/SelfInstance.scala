//package com.avsystem.commons
//package misc
//
//import misc.macros.selfInstanceImpl
//
//final case class SelfInstance[C[_]](instance: C[Any])
//
//object SelfInstance:
//  inline given materialize[C[_]]: SelfInstance[C] = ${ selfInstanceImpl[C] }
