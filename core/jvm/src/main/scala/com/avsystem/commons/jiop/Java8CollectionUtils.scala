package com.avsystem.commons
package jiop

trait Java8CollectionUtils {

  extension [A](it: JIterator[A]) {
    def forEachRemaining(code: A => Any): Unit =
      it.forEachRemaining(code(_))
  }

  extension [A](it: JIterable[A]) {
    def forEach(code: A => Any): Unit =
      it.forEach(code(_))
  }

  extension [A](coll: JCollection[A]) {
    def removeIf(pred: A => Boolean): Unit =
      coll.removeIf(pred(_))

    def scalaStream: ScalaJStream[A] =
      coll.stream.asScala
  }

  extension [A](coll: JCollection[Int]) {
    def scalaIntStream: ScalaJIntStream =
      coll.stream.asScalaIntStream
  }

  extension [A](coll: JCollection[Long]) {
    def scalaLongStream: ScalaJLongStream =
      coll.stream.asScalaLongStream
  }

  extension [A](coll: JCollection[Double]) {
    def scalaDoubleStream: ScalaJDoubleStream =
      coll.stream.asScalaDoubleStream
  }

  extension [K, V](map: JMap[K, V]) {
    def compute(key: K, remappingFunction: (K, V) => V): V =
      map.compute(key, (remappingFunction(_, _)))

    def computeIfAbsent(key: K)(mappingFunction: K => V): V =
      map.computeIfAbsent(key, mappingFunction(_))

    def computeIfPresent(key: K)(remappingFunction: (K, V) => V): V =
      map.computeIfPresent(key, remappingFunction(_, _))

    def forEach(action: (K, V) => Any): Unit =
      map.forEach(action(_, _))

    def merge(key: K, value: V)(remappingFunction: (V, V) => V): V =
      map.merge(key, value, (remappingFunction(_, _)))

    def replaceAll(function: (K, V) => V): Unit =
      map.replaceAll(function(_, _))
  }
}
