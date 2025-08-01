package com.avsystem.commons
package jiop

trait Java8CollectionUtils {

  extension [A](it: JIterator[A]) {
    inline def forEachRemaining(inline code: A => Any): Unit =
      it.forEachRemaining(code(_))
  }

  extension [A](it: JIterable[A]) {
    inline def forEach(inline code: A => Any): Unit =
      it.forEach(code(_))
  }

  extension [A](coll: JCollection[A]) {
    inline def removeIf(inline pred: A => Boolean): Unit =
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
    inline def compute(key: K, inline remappingFunction: (K, V) => V): V =
      map.compute(key, (remappingFunction(_, _)))

    inline def computeIfAbsent(key: K)(inline mappingFunction: K => V): V =
      map.computeIfAbsent(key, mappingFunction(_))

    inline def computeIfPresent(key: K)(inline remappingFunction: (K, V) => V): V =
      map.computeIfPresent(key, remappingFunction(_, _))

    inline def forEach(inline action: (K, V) => Any): Unit =
      map.forEach(action(_, _))

    inline def merge(key: K, value: V)(inline remappingFunction: (V, V) => V): V =
      map.merge(key, value, (remappingFunction(_, _)))

    inline def replaceAll(inline function: (K, V) => V): Unit =
      map.replaceAll(function(_, _))
  }
}
