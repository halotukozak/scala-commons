package com.avsystem.commons
package jiop

import jiop.ScalaJStreamUtils.*

trait Java8CollectionUtils {

  extension [A](it: JIterator[A])
    inline def forEachRemaining(code: A => Any): Unit =
      it.forEachRemaining(jConsumer(code))

  extension [A](it: JIterable[A])
    inline def forEach(code: A => Any): Unit =
      it.forEach(jConsumer(code))

  extension [A](coll: JCollection[A])
    inline def removeIf(pred: A => Boolean): Unit =
      coll.removeIf(jPredicate(pred))
    inline def scalaStream: ScalaJStream[A] =
      coll.stream.asScala

  extension (coll: JCollection[Int])
    inline def scalaIntStream: ScalaJIntStream =
      coll.stream.asScalaIntStream

  extension (coll: JCollection[Long])
    inline def scalaLongStream: ScalaJLongStream =
      coll.stream.asScalaLongStream

  extension (coll: JCollection[Double])
    inline def scalaDoubleStream: ScalaJDoubleStream =
      coll.stream.asScalaDoubleStream

  extension [K, V](map: JMap[K, V])
    inline def compute(key: K, remappingFunction: (K, V) => V): V =
      map.compute(key, jBiFunction(remappingFunction))

    inline def computeIfAbsent(key: K)(mappingFunction: K => V): V =
      map.computeIfAbsent(key, jFunction(mappingFunction))

    inline def computeIfPresent(key: K)(remappingFunction: (K, V) => V): V =
      map.computeIfPresent(key, jBiFunction(remappingFunction))

    inline def forEach(action: (K, V) => Any): Unit =
      map.forEach(jBiConsumer(action))

    inline def merge(key: K, value: V)(remappingFunction: (V, V) => V): V =
      map.merge(key, value, jBiFunction(remappingFunction))

    inline def replaceAll(function: (K, V) => V): Unit =
      map.replaceAll(jBiFunction(function))
}

object Java8CollectionUtils extends Java8CollectionUtils
