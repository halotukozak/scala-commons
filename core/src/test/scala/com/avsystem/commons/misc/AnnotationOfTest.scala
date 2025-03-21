package com.avsystem.commons
package misc

import annotation.AnnotationAggregate

import org.scalatest.funsuite.AnyFunSuite

case class genann[T](value: T) extends StaticAnnotation

case class genagg[T](value: T) extends AnnotationAggregate {
  @genann(value)
  final def aggregated: List[StaticAnnotation] = reifyAggregated
}

@genagg(42)
class Subject

abstract class SelfAnnots(using val annots: SelfAnnotations[genann[?]])

@genagg(42)
@genann("fuu") class Klass extends SelfAnnots

@genagg(42)
@genann("fuu") object Objekt extends SelfAnnots

class AnnotationOfTest extends AnyFunSuite {
  test("aggregate with generic") {
//    assert(AnnotationOf.materialize[genann[Int], Subject].annot.value == 42)
  }

  test("self annotations") {
    assert(new Klass().annots.annots == List(genann(42), genann("fuu")))
    assert(Objekt.annots.annots == List(genann(42), genann("fuu")))
  }
}
