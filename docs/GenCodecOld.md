# [`GenCodec`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/GenCodec.html)

AVSystem commons library contains a typesafe, typeclass-based serialization framework, similar to other Scala
serialization libraries like [Circe](https://circe.github.io/circe/) or [uPickle](https://github.com/lihaoyi/upickle).
However, `GenCodec` is **not** a JSON library even though it has support for JSON serialization.

**[API reference](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/index.html)**

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

## Table of Contents  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [`GenCodec` typeclass](#gencodec-typeclass)
    - [Writing and reading simple (primitive) values](#writing-and-reading-simple-primitive-values)
    - [Writing and reading *lists*](#writing-and-reading-lists)
    - [Writing and reading *objects*](#writing-and-reading-objects)
        - [More about object field order](#more-about-object-field-order)
    - [Writing custom "native" types](#writing-custom-native-types)
    - [Reading metadata from an `Input`](#reading-metadata-from-an-input)
    - [Implementations of `Input` and
      `Output` available by default](#implementations-of-input-and-output-available-by-default)
- [Codecs available by default](#codecs-available-by-default)
- [`GenKeyCodec`](#genkeycodec)
- [Serializing and deserializing examples](#serializing-and-deserializing-examples)
- [Making your own types serializable](#making-your-own-types-serializable)
- [Simple wrappers](#simple-wrappers)
- [Case classes](#case-classes)
    - [Safe evolution and refactoring - summary](#safe-evolution-and-refactoring---summary)
    - [Case class like types](#case-class-like-types)
- [Singletons](#singletons)
- [Sealed hierarchies](#sealed-hierarchies)
    - [Nested format](#nested-format)
    - [Flat format](#flat-format)
    - [Customizing sealed hierarchy codecs](#customizing-sealed-hierarchy-codecs)
    - [Nested vs flat format](#nested-vs-flat-format)
- [Third party classes](#third-party-classes)
    - [Injecting additional implicits into
      `GenCodec` materialization](#injecting-additional-implicits-into-gencodec-materialization)
- [`GenObjectCodec`](#genobjectcodec)
- [Summary](#summary)
    - [Codec dependencies](#codec-dependencies)
    - [Types supported by automatic materialization](#types-supported-by-automatic-materialization)
    - [Recursive types, generic types and GADTs (generalized algebraic data types)](#recursive-types-generic-types-and-gadts-generalized-algebraic-data-types)
    - [Customizing annotations](#customizing-annotations)
    - [Safely introducing changes to serialized classes (retaining backwards compatibility)](#safely-introducing-changes-to-serialized-classes-retaining-backwards-compatibility)
- [Performance](#performance)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

Features:

* `GenCodec` is *generic* - it supports many serialization formats. There is a JSON backend provided by default but it
  can support any format structurally similar to JSON (one that supports writing simple values, lists and objects).
  Enabling support for a particular serialization format is a matter of providing adequate implementations
  of [Input](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/Input.html)
  and [Output](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/Output.html). This is
  particularly easy to do for any of the popular JSON AST representations (Circe, Play, uPickle, etc.). Even though
  `GenCodec` supports JSON serialization, it's not a *JSON library*. Therefore, it does not provide its own JSON AST
  representation.
* `GenCodec` is *typesafe* - it is typeclass-based, i.e. type `T` can be serialized/deserialized only when there is an
  implicit `GenCodec[T]` available. This is fundamental when it comes to type safety. Thanks to how typeclasses work in
  Scala, data types that the programmer wants to serialize are thoroughly validated during compilation to determine
  whether they can be serialized or not. For example `List[T]` is serializable only when `T` is serializable and a case
  class is serializable only if its fields are serializable. This validation goes arbitrary levels deep. Typeclass also
  helps making the serialization format more compact and platform independent, by avoiding usage of runtime reflection.
* `GenCodec` is *boilerplate-free* - it provides macros for automatic derivation of codecs for case classes (and case
  class like types) and sealed hierarchies. This includes complex types like recursively-defined case classes and GADTs.
  These macro generated codecs can be further customized with annotations.
* `GenCodec` is *fast* - the speed primarily comes from avoiding any intermediate representations during serialization
  and deserialization, like some JSON AST or [shapeless](https://github.com/milessabin/shapeless)' `Generic` used by
  many other Scala serialization libraries. See [Performance](#performance) for benchmark results.
* `GenCodec` works in *ScalaJS*. Macro-generated codecs compile to compact and fast JavaScript.

## [`GenCodec`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/GenCodec.html) typeclass

The central trait of the framework is the [
`GenCodec`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/GenCodec.html) typeclass:

```scala
trait GenCodec[T] {
  def read(input: Input): T
  def write(output: Output, value: T): Unit
}
```

A `GenCodec[T]` can read a value of type `T` from an [
`Input`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/Input.html)
and write a value of type `T` to an [
`Output`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/Output.html).
[`Input`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/Input.html) and [
`Output`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/Output.html) are abstract, raw,
stream-like, mutable entities which perform the actual serialization and
deserialization using some particular format hardwired into them, like JSON. Therefore, [
`GenCodec`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/GenCodec.html) by itself is
not
bound to any format. It only depends on the fact that this format is capable of serializing following types and
structures:

* integer numbers, arbitrarily large (`Byte`s, `Short`s, `Int`s, `Long`s and `BigInt`s)
* decimal numbers, arbitrarily large (`Float`s, `Double`s and `BigDecimal`s)
* `Char`s, `String`s, `Boolean`s and `null`s
* arbitrary byte chunks
* millisecond-precision timestamps
* arbitrarily nested *lists*
* arbitrarily nested *objects*, i.e. sequences of *fields* - key-value mappings where each key is a string

Not all of these types must be supported "natively". Some of them may be represented using others, e.g. `Byte`s and
`Short`s using `Int`s or timestamps using `Long`s with number of milliseconds since Unix epoch. **Serialization format
is not required to keep type information**. It should only ensure that writing and reading the same types is consistent,
e.g. when an `Int` was written by a codec then it should be also able to successfully read an `Int`. However, it is the
responsibility of the codec itself to have writing and reading logic consistent with each other.

The only exception to this rule is `null` handling - every `Input` implementation must be able to distinguish `null`
value from any other values through its `readNull()` method. This means that e.g. empty string cannot be used as a
representation of `null` because it would be then indistinguishable from an actual empty string.
Every `Input`/`Output` pair must have a dedicated representation for `null`.

### Writing and reading simple (primitive) values

Every [`Output`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/Output.html) exposes
a [`SimpleOutput`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/SimpleOutput.html)
instance through its `writeSimple` method. `SimpleOutput` aggregates methods for writing simple/primitive values like
`Int`, `String`, etc. These values are considered "simple" because they can be written as a whole with a single method
call.

**NOTE**: `SimpleOutput`'s methods like `writeString` **MUST NEVER** be passed `null` as the value to write.
Instead, `GenCodec` must manually do null checking and use `Output`'s `writeNull()` method for explicit `null` values.
Fortunately, there are helpers for creating nullsafe codecs, e.g. `GenCodec.nullableSimple`.

Similarly, every [`Input`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/Output.html)
exposes a [
`SimpleInput`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/SimpleOutput.html)
instance through `readSimple` method which can be used to read simple values. `GenCodec` should always read the same
type
as was written. For example, if `writeInt` is used to write then `readInt` should be used to read by the same codec
implementation.

If the codec wants to be nullsafe, then it can use `readNull()` on `Input`. Unlike other reading methods, `readNull()`
returns a boolean value which indicates if the value held by `Input` was actually a `null`. If `false` is returned
then the codec may proceed to read its actual type, e.g. with `readString`.

### Writing and reading *lists*

In order to write a *list*, which is an ordered sequence of arbitrary values, the codec must obtain a
[`ListOutput`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/ListOutput.html)
by calling `writeList` on `Output`. Then, it may use its `writeElement()` to obtain a fresh `Output` for each
consecutive
element. `Output` returned for each element must be fully written *before* obtaining `Output` for next element.
After all elements are written, the codec is required to call `finish()` on the `ListOutput`.

Reading a list from an `Input` is analogous. The codec must obtain a [
`ListInput`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/ListInput.html)
using `Input`'s `readList()` method. `ListInput` exposes a `hasNext` method to check whether there are more elements to
read
and a `nextElement` method which provides an `Input` instance for next list element. This `Input` instance must be fully
read before `hasNext` or `nextElement` is called again. At the very end, the codec may want to call `skipRemaining()`
on then `ListInput` in order to skip any remaining elements (only necessary when list is not read until `hasNext`
returns
`false` - e.g. when a fixed number of elements is expected).

Note that because serialization formats are not required to keep type information, lists may sometimes be
indistinguishable from some primitive values. For example, `JsonStringOutput` may use JSON arrays of numbers to
represent
binary values (which are written through `SimpleInput`'s `writeBinary` method). In such situation, calling `readList()`
on the JSON input will succeed even if `writeBinary` was originally used on the JSON output.

### Writing and reading *objects*

An *object* is a mapping between non-null `String` keys and arbitrary values. Internally, each serialization format
may represent an object either as an ordered key-value pair sequence or as a random access key-value mapping (i.e.
it must *either* preserve order of fields *or* provide random access to fields by field name).

In order to write an object, `writeObject()` must be called on `Output` to obtain an
[`ObjectOutput`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/ObjectOutput.html)
instance. The codec may then obtain an `Output` instance for each named field through `writeField` method.
This instance must be fully written before `writeField` can be called again. At the very end, `finish()` must be called
on the `ObjectOutput`.

In order to read an object, the codec must call `readObject()` on `Input` in order to obtain an
[`ObjectInput`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/ObjectInput.html)
instance. Similarly to `ListInput`, `ObjectInput` provides a `hasNext` method to check whether there are any remaining
fields in the `ObjectInput`. In order to read each field, the codec may use `nextField()` method with returns a
[`FieldInput`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/FieldInput.html) - a
subinterface of `Input` which additionally provides field name.

If serialization format implemented by particular `Input`/`Output` implementation preserves field order then `nextField`
is guaranteed to return fields in the same order as they were written by `ObjectOutput`. However, if the serialiaztion
format does **not** preserve field order then it must provide random field access instead. This is done by `peekField`
method. Codec implementations must be prepared for both situations.

If you need a custom object serialization for your type then your best bet is probably to implement appropriate `apply`
and `unapply`
methods on companion object of your type. And even if you can't do it (e.g. because you're writing a codec for a third
party type) then you can still use [
`fromApplyUnapplyProvider`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/GenCodec$.html#fromApplyUnapplyProvider[T](applyUnapplyProvider:Any):com.avsystem.commons.serialization.GenCodec[T])
and avoid implementing your codec manually.

Finally, if you *really* need to write your object codec by hand then you can make it easier by using
`getNextNamedField` helper method on `ObjectInput` instead of `nextField` and `peekField` directly.

#### More about object field order

For most serialization formats, it's completely natural to retain object field order. For example, a JSON string
naturally has object fields stored in the order that they
were written to `JsonObjectOutput`. For these formats it is required that an `ObjectInput` returns object fields in
exactly the same order as they were written to
a corresponding `ObjectOutput`. This normally includes all serialization formats backed by strings, byte sequences,
streams, etc.

> :bulb: It is generally recommended reading data with particular `Input` only when it was written using its
> corresponding `Output`. However, some intputs offer additional guarantees. For example `JsonStringInput` will accept
> fields in any order even though `JsonStringOutput` retains field order in the resulting JSON string. This way it's
> possible to use `JsonStringInput` to read JSON that was written by other tools or humans that may mix up field order.

However, there are also serialization formats that use memory representation where an object is usually backed by a
hashtable. Such representations cannot retain field order.
`GenCodec` can still work with these but as an alternative to preserving field order, they must implement random field
access (field-by-name access). This is done by
overriding [
`peekField`](avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/ObjectInput.html#peekField(name:String):com.avsystem.commons.misc.Opt[com.avsystem.commons.serialization.FieldInput])
on `ObjectInput`.

To summarize, an `ObjectInput` is generally **not** guaranteed to retain order of fields as they were written to an
`ObjectOutput` but if it doesn't then it must
provide random field access by field name. Also, note that out of all the default and macro-generated codecs provided,
only [flat sealed hierarchy codecs](#flat-format) actually depends
on this requirement. All the other (non-custom) codecs ignore field order during deserialization so you can e.g. freely
reorder fields in your case classes if they use macro generated codecs.

### Writing custom "native" types

`Input` and `Output` interfaces provide some "backdoors" which make it possible for codec implementations to take
advantage
of particular serialization format's additional capabilities which are not exposed in standard `Input`/`Output` APIs.
For example, [BSON](http://bsonspec.org/) used by MongoDB has special representation for Object IDs which we would like
to use. `Input` and `Output` implementations for BSON may support it through their `writeCustom` and `readCustom`
methods.
See their documentation for more details.

### Reading metadata from an `Input`

As mentioned before, serialization formats in general are not required to keep any type information (except
distinguishing
`null` values from others). This way they can be as simple and compact as possible. However, many common formats
naturally *do* keep some type information. For example, when reading a JSON we can easily check whether it contains an
object, an array, a string, a number, a boolean or a `null` value. Codecs could use this in order to optimize layout of
their serialized values and avoid manual writing of their own type information.

`Input` may expose type information through its `readMetadata` method. `Output` on the other hand may tell the codec
whether its corresponding `Input` is capable of keeping this type information - this is done by `Output`'s
`keepsMetadata`
method. See their documentation for more details.

### Implementations of `Input` and `Output` available by default

Implementations of `Input` and `Output` provided by `scala-commons` by default are:

* `SimpleValueInput`/`SimpleValueOutput` - translate serialized values into simple Scala objects.
  Primitive types are represented by themselves, lists are represented by standard Scala `List[T]` values and objects
  are represented by standard Scala `Map[String, T]` values.
* `JsonStringInput`/`JsonStringOutput` - JSON implementations that work
  directly on `String` values, without intermediate JSON AST
* `CborInput`/`CborOutput` - [Concise Binary Object Representation, CBOR](https://tools.ietf.org/html/rfc7049)
* `StreamInput`/`StreamOutput` - simple opaque binary format
* `HoconInput` (in `commons-hocon` module) - for reading from
  [Human Optimized Config Object Notation, HOCON](https://github.com/lightbend/config/blob/master/HOCON.md)
* `BsonReaderInput`/`BsonWriterOutput`,`BsonWriterNamedOutput`,`BsonValueOutput` (in `commons-mongo` module) - for
  reading and writing from [Binary JSON, BSON](http://bsonspec.org/), format used by MongoDB wire protocol.

## Codecs available by default

In order to serialize/deserialize value of some type, there needs to be an implicit value of type `GenCodec[T]`
available.
The library by default provides codecs for common Scala and Java types:

* `Unit`, `Null`, `String`, `Symbol`, `Char`, `Boolean`, `Byte`, `Short`, `Int`, `Long`, `Float`, `Double`,
  `java.util.Date`, `Array[Byte]`, `BigInt`, `BigDecimal` and all their Java boxed counterparts
  (like `java.lang.Integer`).
* Any Scala tuple, provided that every tuple element type can be serialized. Tuples are serialized into lists.
* Any `Array[T]`, provided that `T` can be serialized
* Any Scala collection extending `scala.collection.Seq[T]` or `scala.collection.Set[T]`, provided that `T` can be
  serialized
  and there is an appropriate instance of `Factory` (e.g. `GenCodec[List[T]]` requires `Factory[T, List[T]]`).
  All standard library collections have this `Factory` instance so you only have to worry about it when dealing with
  custom collection implementations.
* Any `java.util.Collection[T]`, provided that `T` can be serialized
* Any `scala.collection.Map[K,V]` provided that `V` can be serialized and there is an implicit
  [`GenKeyCodec`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/GenKeyCodec.html)
  available for `K` so that it can be converted to a string and used as object key.
  There must also be an appropriate instance of `Factory` for particular `Map` implementation (e.g. `HashMap[K,V]`
  requires `Factory[(K, V), HashMap[K, V]]`). All standard library map implementations have this `Factory` instance
  so you only have to worry about it when dealing with custom map implementations.
* Any `java.util.Map[K,V]`, with the same restrictions as for Scala maps (there must be `GenCodec` for `V` and
  `GenKeyCodec` for `K`)
* `Option[T]`, `Opt[T]`, `OptArg[T]`, `NOpt[T]`, `OptRef[T]`, provided that `T` can be serialized.
* `Either[A,B]`, provided that `A` and `B` can be serialized.
* [`NamedEnum`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/misc/NamedEnum.html)s
  whose companion object extends [
  `NamedEnumCompanion`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/misc/NamedEnumCompanion.html).
  This includes [`ValueEnum`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/misc/ValueEnum.html)s.
* Java enums

## [`GenKeyCodec`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/GenKeyCodec.html)

For serialization of maps, there is an auxilliary typeclass - [
`GenKeyCodec`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/GenKeyCodec.html).
It provides the ability to translate values of some type into `String` keys that can be used as object keys by
[`GenCodec`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/GenCodec.html)s for Scala
and Java `Map` types. By default, following types have [
`GenKeyCodec`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/GenKeyCodec.html)
provided:

* `String`
* `Boolean`, `Char`, `Byte`, `Short`, `Int`, `Long`
* `JBoolean`, `JCharacter`, `JByte`, `JShort`, `JInteger`, `JLong`
* [`NamedEnum`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/misc/NamedEnum.html)s
  whose companion object extends [
  `NamedEnumCompanion`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/misc/NamedEnumCompanion.html).
  This includes [`ValueEnum`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/misc/ValueEnum.html)s.
* Java enums

## Serializing and deserializing examples

To serialize and deserialize values, you can use the [
`read`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/GenCodec.html#read(input:com.avsystem.commons.serialization.Input):T)
and [
`write`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/GenCodec.html#write(output:com.avsystem.commons.serialization.Output,value:T):Unit)
methods directly. However, most of the *backends* (`Input`/`Output` implementations) provide some helper method which
you can use instead of instantiating `Input` and `Output`s manually and passing them to `GenCodec`.

For example, the `JsonStringOutput` and `JsonStringInput` companion objects provide `read` and `write` functions that
require an implicit `GenCodec` instance and convert directly between serialized types and JSON strings.

```scala
import com.avsystem.commons.serialization.json._
  
// primitive types like `Int` are represented by themselves
JsonStringOutput.write[Int](123) // JSON: 123
JsonStringInput.read[Int]("123") // 123

// `Option`, `Opt`, `NOpt`, `OptRef` and `OptArg` are represented either as `null` (when empty) or directly
// as the underlying value (when non-empty). `Some(null)` should not be used - it is indistinguishable from
// `None` unless the codec for the type wrapped in `Option` has some special `null` handling.
val raw = JsonStringOutput.write[Option[String]](None) // JSON: null
JsonStringInput.read[Option[String]]("null") // None

val raw = JsonStringOutput.write[Option[String]](Some("sth")) // "sth"
JsonStringInput.read[Option[String]]("sth") // Some("sth")

// all collections are represented as lists
val raw = JsonStringOutput.write(Set(1,2,3)) // JSON: [1,2,3]
JsonStringInput.read[Set[Int]](raw) // Set(1,2,3)

// maps are represented as objects
val raw = JsonStringOutput.write(Map("1" -> 1, "2" -> 2)) // JSON: {"1":1,"2":2}
JsonStringInput.read[Map[String,Int]](raw) // Map("1" -> 1, "2" -> 2)

// maps without GenKeyCodec instance for key type are represented as lists of key-value pairs
val raw = JsonStringOutput.write(Map(1.0 -> 1, 2.0 -> 2)) // JSON: [{"k":1.0,"v":1},{"k":2.0,"v":2}]
JsonStringInput.read[Map[Double,Int]](raw) // Map(1.0 -> 1, 2.0 -> 2)

// tuples are represented as heterogeneous lists
val raw = JsonStringOutput.write((1, "sth", 2.0)) // JSON: [1,"sth",2.0]
JsonStringInput.read[(Int,String,Double)](raw) // (1, "sth", 2.0)
```

## Making your own types serializable

In order to make your own (or third-party) classes and types serializable, you need to provide an instance of
[`GenCodec`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/GenCodec.html)
for it. You can implement it manually, but in most cases you'll probably rely on one of the predefined
codecs (primitive types, collections, standard library classes, etc.) or materialize it automatically by making
the companion object of your type extend
[`HasGenCodec`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/HasGenCodec.html)
or by using the
[
`GenCodec.materialize`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/GenCodec$.html#materialize[T]:com.avsystem.commons.serialization.GenCodec[T])
macro directly.

## Simple wrappers

When your type is a simple wrapper over a type that already has a `GenCodec` instance, the easiest way to provide a
codec for the wrapper type is to use `TransparentWrapperCompanion` as base class for its companion object:

```scala
case class BinaryData(bytes: Array[Byte]) extends AnyVal
object BinaryData extends TransparentWrapperCompanion[Array[Byte], BinaryData]
```

NOTE: if your type wraps `String`, `Int` or `Long` then you can use `StringWrapperCompanion`, `IntWrapperCompanion` and
`LongWrapperCompanion` for brevity.

Using `TransparentWrapperCompanion` not only derives a `GenCodec` instance from the wrapped type's instance but also
other typeclass instances, e.g. [`GenKeyCodec`](#genkeycodec). This mechanism is extensible and can be used by any
typeclass that wants to "understand" wrapping. Therefore, `TransparentWrapperCompanion` is a generic tool, not
specifically bound in any way to `GenCodec`.

## Case classes

```scala
case class Person(name: String, birthYear: Int)
object Person extends HasGenCodec[Person]
```

By making companion object of `Person` extend `HasGenCodec[Person]` you're making the compiler automatically
materialize an instance of `GenCodec[Person]` and inject it into the companion object.
The same works for case classes, case class like types (i.e. ones that have appropriate `apply` and `unapply` methods
in their companion object) and sealed hierarchies.

There are also other flavors of `HasGenCodec`. For example, `HasPolyGenCodec` handles parameterized (generic) data types
and `HasGenCodecWithDeps` allows injecting additional implicits into macro materialization.
Even if none of the available base companion classes fits your needs, it should be relatively painless to write your
own.

The macro-materialized codec for case class serializes it into an object where field names serve as keys and field
values as associated values. For example, `Person("Fred", 1990)` would be represented (using `JsonStringOutput`)
as `{"name":"Fred","birthYear":1990}`.

The macro will only compile if it can find a `GenCodec` instance for every field of your case class. Otherwise, you'll
get a compilation error telling you that some field can't be serialized because no implicit `GenCodec` is defined for
its type. This way the macro will fully validate your case class. This is good - you'll never serialize any type by
accident and if you forget to make any type serializable, the compiler will tell you about it. This way you avoid
problems usually associated with runtime reflection based serialization, particularly popular in Java ecosystem.

In general, the serialization framework requires that the serialized representation retains order of object fields and
during deserialization supplies them in exactly the same order as they were written during serialization. This is
usually a reasonable assumption because most serialization formats are either textual, binary or stream-like (the word "
serialization" itself indicates a sequential order). If field order is not preserved, serialization format must support
random field access instead - see [object field order](#object-field-order).

The codec materialized for case class guarantees that the fields are written in the order of their declaration in
constructor. However, during deserialization the codec is lenient and does not require that the order of fields is the
same as during serialization. It will successfully deserialize the case class as long as all the fields are present in
the serialized format (in any order) or have a default value defined (either as Scala-level default parameter value or
with [`@whenAbsent`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/whenAbsent.html)
annotation). Any superfluous fields will be simply ignored. This allows the programmer to refactor the case class
without breaking compatibility with serialization format - fields may be reordered and removed. New fields may also be
added, as long as they have a default value defined.

The way macro materializes the codec may be customized with annotations. All annotations are governed by the same
[annotation processing](Annotations.md) rules.

* Using [`@name`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/name.html) you can
  change the raw field name used for serialization of each case class field.

  ```scala
  case class Entity(@name("_id") id: String, data: Int)
  ```

  This is useful in particular when you want to refactor your case class and change the name of some field without
  changing the serialized format (in order to remain backwards compatible). Note that the annotation (like all other
  annotations used to customize serialization) may also be inherited from implemented/overridden member.

* Using [`@whenAbsent`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/whenAbsent.html)
  you can provide a fallback value for case class field. This value
  is used during deserialization when the field is missing in the encoded data. Alternatively to using `@whenAbsent`,
  you can simply provide Scala-level default parameter value and it
  will also be picked up as fallback value for deserialization. However, `@whenAbsent` is better when you want the
  default value to be used *only* during deserialization, without
  affecting programming interface.

* Using [`@generated`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/generated.html)
  annotation on some additional members of your case class you can instruct the codec to serialize some additional
  fields into the resulting format. This can be useful when some case class field has been removed or converted to a
  regular `val` or `def` but we want the serialized format to be backwards compatible. Sometimes it may also be
  necessary to generate additional fields for the purpose of database indexing.

  ```scala
  case class Person(name: String, birthYear: Int) {
    @generated def upperName: String = name.toUpper
  }
  ```

  Generated members may also be customized with `@name` annotation. During serialization, generated fields are emitted
  after all the "regular" fields have been written. Unlike for the regular fields, there is no guarantee about the order
  of generated fields in the serialized format. During deserialization, generated fields are simply ignored.

* If one of the fields in your case class has a default value (in Scala or with `@whenAbsent`), you might want to not
  serialize that field if its value is the default one. To instruct the codec to omit default values, [
  `@transientDefault`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/transientDefault.html)
  annotation can be used.

  ```scala
  case class Person(name: String, birthYear: Int, @transientDefault planet: String = "Earth")
  ```

  This comes in handy especially when your field might not have a value at all. You can express it using [
  `Opt`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/misc/Opt.html) which is serialized either as
  `null` (when empty) or directly as the value inside it (when non-empty). By specifying `Opt.Empty` as default value
  and applying `@transientDefault` annotation, you can completely avoid emitting the field when there is no value.

  ```scala
  case class Person(name: String, birthYear: Int, @transientDefault maidenName: Opt[String] = Opt.Empty)
  ```

  Note that the absence of a field with default value in the serialized data does not cause any errors during
  deserialization - if the field is missing, the codec will simply use its default value (it works even without
  `@transientDefault` annotation).

* If your case class has exactly one field, you might want to avoid it being wrapped in an object and simply serialize
  the value of that field. This way your class would be a "transparent wrapper" over some type. Wrapping a primitive
  type into nicely named wrapper is a common technique to increase readability and type safety. In Scala, value classes
  are often utilized for this purpose.
  If your case class has exactly one field, you can annotate is as [
  `@transparent`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/transparent.html) and
  the macro materialized codec will simply serialize the wrapped value.

  ```scala
  @transparent case class DatabaseId(raw: String) extends AnyVal
  ```

#### Safe evolution and refactoring - summary

Following changes can be made to a case class while keeping it backwards compatible with the old format (the old format
will successfully deserialize to the new case class):

* renaming the case class or moving to a different package, object, etc. - case class name is not serialized (unless
  it's a part of a [sealed hierarchiy](#sealed-hierarchies))
* reordering fields
* removing a field
* renaming a field, as long as the old name is provided using `@name` annotation
* adding a field, as long as it has default value defined
* changing the type of a field, as long as the old and new type serialize to the same format
* adding generated fields
* adding implicit parameters (they must be available at the codec materialization site and will be embedded in the codec
  itself)
* case class may also be safely lifted to a sealed hierarchy (when using `@flatten` and `@defaultCase` -
  see [sealed hierarchies](#sealed-hierarchies) for details)

### Case class like types

If, for whatever reason, your class can't be a case class, but you still want it to be serialized like a case class
would be, you can make it look like a case class. In order to do this, simply provide your own implementations of
`apply` and `unapply` methods in the companion object of your trait/class. For case classes, these methods are generated
automatically by the compiler.

```scala
class Person(val name: String, val birthYear: Int)
object Person extends HasGenCodec[Person] {
  def apply(name: String, birthYear: Int): Person = new Person(name, birthYear)
  def unapply(person: Person): Option[(String, Int)] = Some((person.name, person.birthYear))
}
```

**NOTE**: if `apply` method takes a repeated (varargs) parameter, then there must be an `unapplySeq` method instead of
`unapply` and the repeated parameter should correspond to a `Seq` in the `unapplySeq` return type.

**NOTE**: the `Option` in return type of `unapply`/`unapplySeq` may be replaced with other similar types, e.g. `Opt`,
`NOpt`, etc. thanks to
the [name based extractors](https://hseeberger.wordpress.com/2013/10/04/name-based-extractors-in-scala-2-11/) feature in
Scala. This way you may avoid boxing associated with `Option`s.

You can use all the customization features available for regular case classes - `@name`, `@transientDefault` (applied on
`apply` parameters), `@generated`, `@transparent`, etc.

## Singletons

`GenCodec.materialize` macro is also able to generate (trivial) codecs for singletons, i.e. `object`s or types like
`this.type`.
Singletons always serialize into empty object (unless `@generated` fields are defined). When using [
`SimpleValueOutput`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/SimpleValueOutput.html),
empty object is represented by empty `Map`.

```scala
object SomeObject {
  implicit val codec: GenCodec[SomeObject.type] = GenCodec.materialize[SomeObject.type]
}
```

Just like case classes, singletons might define or inherit members annotated as [
`@generated`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/generated.html) - their
values will be serialized as the only object fields and ignored during deserialization.

Singletons will successfully deserialize from any object, ignoring all its fields.

Singleton codecs may not seem very useful as standalone codes - they're primarily used when the object is a part of
a [sealed hierarchy](#sealed-hierarchies).

## Sealed hierarchies

`HasGenCodec` base companion class and all its variants can also be used to derive a `GenCodec` for a sealed trait or
class. There are two possible serialization formats for sealed hierarchies: *nested* (the default one) and *flat*
(enabled using [`@flatten`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/flatten.html)
annotation). The nested format is the default one for historical reasons - it is generally recommended to use the flat
format as it's more robust and customizable. The advantage of nested format is that it does not depend on the order of
object fields.

### Nested format

```scala
sealed trait Timeout
case class FiniteTimeout(seconds: Int) extends Timeout
case object InfiniteTimeout extends Timeout
object Timeout extends HasGenCodec[Timeout]
```

In nested format, values of sealed traits or classes are serialized into objects with just one field. The name of that
field is the name of actual class being serialized. The value of that field will be the serialized class itself, using
its own codec. For example, `FiniteTimeout(60)` would be represented (using [
`JsonStringOutput`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/json/JsonStringOutput.html))
as `{"FiniteTimeout":{"seconds":60}}`

`GenCodec` for each case class/object may be provided explicitly or left for the macro to materialize. In other words,
the `materialize` macro called for sealed trait *will* descend into its subtypes and materialize their codecs
recursively. However, it will still *not* descend into any case class fields.

### Flat format

The other format is called "flat" because it does not introduce the intermediate single-field object. It is enabled by
annotating your sealed hierarchy root with [
`@flatten`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/flatten.html) annotation,
e.g.

```scala
@flatten sealed trait Timeout
case class FiniteTimeout(seconds: Int) extends Timeout
case object InfiniteTimeout extends Timeout
object Timeout extends HasGenCodec[Timeout]
```

Instead of creating a single-field object, now the `materialize` macro will assume that every case class/object
serializes to an object (e.g. JSON object) and will use this object as a representation of the entire sealed type. In
order to differentiate between case classes during deserialization, an additional discriminator field containing class
name is added at the beginning of resulting object. For example, `FiniteTimeout(60)` would be represented (using [
`JsonStringOutput`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/json/JsonStringOutput.html))
as `{"_case":"FiniteTimeout","seconds":60}`

#### Transparent wrappers in flat sealed hierarchies

It is possible for a case class in a flat sealed hierarchy to be a _transparent wrapper_. To do this, the wrapped
type must have an implicit `ApplyUnapplyCodec` (a subtype of `GenCodec` specific for case classes and case class like
types).
The wrapping case itself must have a companion that extends `TransparentWrapperCompanion`.

```scala
case class NumericalData(num: Int)
object NumericalData extends HasApplyUnapplyCodec[NumericalData]
case class TextualData(text: String)
object TextualData extends HasApplyUnapplyCodec[TextualData]

@flat sealed trait DataUnion
case class NumericalCase(data: NumericalData) extends DataUnion
object NumericalCase extends TransparentWrapperCompanion[NumericalData, NumericalCase]
case class TextualCase(data: TextualData) extends DataUnion
object TextualCase extends TransparentWrapperCompanion[TextualData, TextualCase]
object DataUnion extends HasGenCodec[DataUnion]
```

Serializing `NumericalCase(NumericalData(42))` using `JsonStringOutput` would yield
JSON string `{"_case": "NumericalCase", "num": 42}`.

`NumericalData` and `TextualData` would typically be defined in a different file than the sealed hierarchy itself.
That's the typical motivation for using transparent wrappers in a sealed hierarchy - you can't have these types
in the same compilation unit as the sealed trait.

### Customizing sealed hierarchy codecs

Similarly to case classes, sealed hierarchy codecs may be customized with annotations. All annotations are governed by
the same [annotation processing](Annotations.md) rules.

* Using [`@name`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/name.html) you can
  change the class name saved as marker field name in nested format or as marker field value in flat format.

   ```scala
   sealed trait Tree
   @name("L") case class Leaf(value: Int) extends Tree
   @name("B") case class Branch(left: Tree, right: Tree) extends Tree
   object Tree extends HasGenCodec[Tree]
   ```

* When using flat format, name of the marker field (`_case` by default) may be customized by passing it as an argument
  to `@flatten` annotation, e.g. `@flatten("type")`.

* When using flat format, one of the case classes may be annotated as [
  `@defaultCase`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/defaultCase.html). When
  marker field is missing during deserialization, the codec will assume that it's deserializing the case class annotated
  as `@defaultCase`. This mechanism is useful to retain backwards compatibility when refactoring a case class into a
  sealed hierarchy with multiple case classes.

* It's important to remember that deserialization of the flat format relies on the preservation of field order by
  serialization backend (or random field access):
  In particular, the marker field must be known to the codec before it reads other fields so that it knows which class
  to create and how to deserialize the rest of the fields.
  There is one escape hatch from this requirement - a field present in one or more of case classes in the sealed
  hierarchy may be marked as [
  `@outOfOrder`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/outOfOrder.html). See
  the documentation of this annotation for more details. The direct motivation for introducing this annotation was to
  support deserialization of `_id` field in MongoDB documents - the database server always serves documents with `_id`
  being the very first field.

### Nested vs flat format

Advantages of nested format:

* Codec materialized for sealed hierarchy may reuse already existing codecs for its case classes
* Each case class may serialize to arbitrary representation while flat format requires every case class to serialize to
  an object
* It does not rely on object field order

Advantages of flat format:

* When some field is present in more than one case class, it may be extracted from serialized form in uniform way,
  regardless of which case class it comes from. This may greatly simplify querying and indexing databases used to store
  sealed hierarchies.
* Case class serialized with flat sealed hierarchy codec may be safely deserialized using codec of the case class
  itself.
* Using `@defaultCase` annotation, a case class may be safely refactored into a sealed hierarchy.

In other words, when the serialized form is opaque and you don't care about it as long as it deserializes properly to
the same value then the nested format should be better. If you care about how the serialized form looks like and you
want to retain it through refactorings then probably the flat format is easier to maintain.

## Third party classes

When you need to serialize a type that comes from a third party library, you must implement a `GenCodec` for it, put
somewhere in your codebase and remember to import it when needed. You must import it because it's not possible to put it
in companion object of the type being serialized. However, you can still use all the goodness of macro materialization
only if you can make the third party type "look like" a case class by defining a "fake" companion for that type and
passing it explicitly to `GenCodec.fromApplyUnapplyProvider`. For example, here's an easy way to make a typical Java
bean class serializable with `GenCodec`:

This is the third party Java class:

```java
public class JavaPerson {
    private String name;
    private int birthYear;
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getBirthYear() { return birthYear; }
    public void setBirthYear(int birthYear) { this.birthYear = birthYear; }
}
```

This is your Scala code to make it serializable:

```scala
object JavaPersonFakeCompanion {
  def apply(name: String, birthYear: Int): JavaPerson = {
    val result = new JavaPerson
    result.setName(name)
    result.setBirthYear(birthYear)
    result
  }
  def unapply(javaPerson: JavaPerson): Option[(String, Int)] =
    Some((javaPerson.getName, javaPerson.getBirthYear))
    
  implicit val javaPersonCodec: GenCodec[JavaPerson] = 
    GenCodec.fromApplyUnapplyProvider[JavaPerson](JavaPersonFakeCompanion)
}

```

Now, as long as `JavaPersonFakeCompanion.javaPersonCodec` is in scope, `JavaPerson` instances will serialize just as if
it was a regular Scala case class. The macro derives serialization format from signatures of `apply` and `unapply`
methods and uses them to create and deconstruct `JavaPerson` instances.

### Injecting additional implicits into `GenCodec` materialization

When possible, you should keep all your `GenCodec` instances (and other implicits) in companion objects of your data
types.
This way they will be effectively globally visible and won't need to be imported. However, this is not always possible -
a `GenCodec` instance for a third party class is the most common example of such situation.

Of course, you can always import third party implicits explicitly into the scope. However, this is problematic because
it
doesn't always work (presumably due to compiler bugs) and IDEs like IntelliJ IDEA tend to recognize such imports as
unused.
Because of this, `GenCodec` comes with a handy base companion class, similar to `HasGenCodec` but capable of injecting
additional implicits. It is called `HasGenCodecWithDeps`.

```scala
object MyAdditionalImplicits {
  implicit val javaPersonCodec: GenCodec[JavaPerson] = ...
  ...
}

case class UsesJavaPerson(javaPerson: JavaPerson)
object UsesJavaPerson extends HasGenCodecWithDeps[MyAdditionalImplicits.type, JavaPerson]
```

In order to reduce boilerplate, you can make your own base companion class which automatically injects desired
implicits:

```scala
abstract class HasCustomizedGenCodec[T](
  implicit macroCodec: MacroInstances[MyAdditionalImplicits.type, () => GenCodec[T]]
) extends HasGenCodecWithDeps[MyAdditionalImplicits.type, T]
```

## [`GenObjectCodec`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/GenKeyCodec.html)

`GenObjectCodec` is a subinterface of `GenCodec` which directly exposes methods that write to `ObjectOutput` and read
from `ObjectInput`. You can use this typeclass instead of plain `GenCodec` if you want to be sure that some particular
type serializes to an *object*. `GenObjectCodec` instances are provided similarly to `GenCodec` instances - there is
a `GenObjectCodec.materialize` macro and `HasGenObjectCodec` companion base class.

## Summary

### Codec dependencies

The `materialize` macro will only generate [
`GenCodec`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/GenCodec.html)
implementation for case class or sealed hierarchy if all fields of case classes are already serializable
(i.e. their types have their own [
`GenCodec`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/GenCodec.html)
instances). For example, the following code will not compile:

```scala
case class Address(city: String, zipcode: String)
case class Person(name: String, address: Address)
object Person {
  implicit val codec: GenCodec[Person] = GenCodec.materialize[Person] // error!
}
```

The `materialize` macro does not descend into case class fields and will therefore refuse to generate codec for `Person`
because it doesn't have a codec for `Address`. This behavior is intentional and serves to avoid making types
serializable
by accident. However, there is an alternative macro which *does* descend into dependencies,
[
`GenCodec.materializeRecursively`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/GenCodec$.html#materializeRecursively[T]:com.avsystem.commons.serialization.GenCodec[T]):

```scala
case class Address(city: String, zipcode: String)
case class Person(name: String, address: Address)
object Person {
  implicit val codec: GenCodec[Person] = GenCodec.materializeRecursively[Person]
}
```

`materializeRecursively` will generate a codec for `Address`. However, this codec will be visible only by the `Person`
codec.
That means you can now serialize `Person` objects, but you still can't serialize `Address` objects by themselves. Also,
remember that `materializeRecursively` descends into dependencies only when it actually needs to do it, i.e. first it
tries to use any already declared [
`GenCodec`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/GenCodec.html).

### Types supported by automatic materialization

`materialize` and `materializeRecursively` macros work for:

* case classes, provided that all field types are serializable
* case class like types, i.e. classes or traits whose companion object contains a pair of matching `apply`/`unapply`
  methods defined like in case class companion, provided that all field types are serializable
* singleton types, e.g. types of `object`s or `this.type`
* sealed traits or sealed abstract classes, provided that `GenCodec` can also be materialized for all non-abstract
  subtypes (typically case classes). If the nested serialization format is used (i.e. `@flatten` annotation is **not**
  used) then `GenCodec`s for subtypes may also be declared explicitly and will be reused by sealed trait's codec.

### Recursive types, generic types and GADTs (generalized algebraic data types)

A recursively defined case class:

```scala
case class SimpleTree(children: List[SimpleTree])
object SimpleTree extends HasGenCodec[SimpleTree]
```

A generic (and recursive) data type:

```scala
sealed trait Tree[T]
case class Leaf[T](value: T) extends Tree[T]
case class Branch[T](left: Tree[T], right: Tree[T]) extends Tree[T]
object Tree extends HasPolyGenCodec[Tree]
```

A generalized algebraic data type (also recursive):

```scala
sealed trait Expr[T]
case class StringLiteral(value: String) extends Expr[String]
case class IntLiteral(value: Int) extends Expr[Int]
case object NullLiteral extends Expr[Null]
case class Plus[T](lhs: Expr[T], rhs: Expr[T]) extends Expr[T]
object Expr extends HasGadtCodec[Expr]
```

Note that for generic types we must use `HasPolyGenCodec` or `HasGadtCodec` instead of `HasGenCodec`. It should also be
relatively easy to create custom versions of these base companion classes for whatever combination of type kind, bounds
and implicit dependencies you need to use.

### Customizing annotations

All annotations are governed by [annotation processing](Annotations.md) rules.

* [`@name`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/name.html)
* [`@whenAbsent`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/whenAbsent.html)
* [`@transparent`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/transparent.html)
* [
  `@transientDefault`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/transientDefault.html)
* [`@flatten`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/flatten.html)
* [`@defaultCase`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/defaultCase.html)
* [`@outOfOrder`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/outOfOrder.html)
* [`@generated`](http://avsystem.github.io/scala-commons/api/com/avsystem/commons/serialization/generated.html)

### Safely introducing changes to serialized classes (retaining backwards compatibility)

When changing definitions of serialized data types, you must ensure that your changes don't break compatibility.
`GenCodec` guarantees that some changes are safe
while other are not.

Let's assume a case class with an autogenerated `GenCodec` instance:

```scala
case class Data(int: Int, str: String)
object Data extends HasGenCodec[Data]
```

A summary of safe and unsafe changes to this class definition:

```scala
// OK - renaming a case class is fine because its name is not serialized (but NOT OK if the case class is a part of a sealed hierarchy - see later)
case class RenamedData(num: Int, str: String)

// OK - reordering fields is fine because the codec can read fields in any order
case class Data(str: String, num: Int)

// OK - removing a field is fine because the codec ignores nonexistent fields
case class Data(num: Int)

// NOT OK - adding a field is not fine because the field may be missing in previously serialized data
case class Data(num: Int, str: String, flag: Boolean)

// OK - adding a field with default value is fine because the default value will be used when the field is missing in previously serialized data
case class Data(num: Int, str: String, flag: Boolean = false)
case class Data(num: Int, str: String, @whenAbsent(false) flag: Boolean)

// NOT OK - changing the name of a field is not fine because previously serialized data will contain the old name
case class Data(number: Int, str: String)

// OK - changing the name of a field is ok if the previous name is retained in `@name` annotation
case class Data(@name("num") number: Int, str: String)

// NOT OK - changing the type of a field to a type with different representation is not fine
case class Data(num: String, str: String)

// OK - changing the type of a field to a type with compatible representation is fine (all `Int` values will properly deserialize as `Long`)
case class Data(num: Long, str: String)

// OK - changing the type of a field to a type that is a transparent wrapper of the previous type is fine
case class Amount(value: Int) extends AnyVal
object Amount extends IntWrapperCompanion[Amount]

case class Data(num: Amount, str: String)

// OK - lifting a case class into a sealed hierarchy is fine but only for flat format and when using `@defaultCase` annotation
@flatten sealed trait Info
@defaultCase case class Data(num: Int, str: String) extends Info
case object Empty extends Info
object Info extends HasGenCodec[Info]
```

Now let's assume a sealed hierarchy with an autogenerated `GenCodec` instance:

```scala
sealed trait Info
case class Data(num: Int, str: Int) extends Info
case object Empty extends Info
object Info extends HasGenCodec[Info]
```

Case classes in a sealed hierarchy may be changed in the same way as if they were standalone classes (except for
renaming).
An additional summary of safe and unsafe changes to this trait definition:

```scala
// NOT OK - renaming a case class/object in hierarchy is not fine because the class name is used as a discriminator
case class InfoData(num: Int, str: Int) extends Info

// OK - renaming a case class/object in hierarchy is fine when the old name is preserved in `@name` annotation
@name("Data") case class InfoData(num: Int, str: Int) extends Info

// OK - adding more case classes/objects to the hierarchy is fine
...
case class MoreInfo(num: Double, flag: Boolean) extends Info

// NOT OK - changing the sealed hierarchy format between nested and flat is not fine
@flatten sealed trait Info
...
```

Remember that the rules listed above only apply to automatically materialized codecs.
If you implement your codecs manually then you need to ensure compatibility manually as well.

## Performance

There are
JMH [benchmarks](https://github.com/AVSystem/scala-commons/blob/master/commons-benchmark/jvm/src/main/scala/com/avsystem/commons/ser/JsonSerializationBenchmark.scala)
implemented for JSON serialization, comparing `GenCodec` with [Circe](https://circe.github.io/circe/)
and [uPickle](https://github.com/lihaoyi/upickle).

Example results (higher score is better):

```
[info] Benchmark                                      Mode  Cnt        Score        Error  Units
[info] JsonReadingBenchmark.readCCCirce              thrpt   10   649967.336 ±  12828.283  ops/s
[info] JsonReadingBenchmark.readCCGenCodec           thrpt   10  1050431.352 ±  16007.544  ops/s
[info] JsonReadingBenchmark.readCCUpickle            thrpt   10   698061.199 ±  13618.354  ops/s
[info] JsonReadingBenchmark.readFlatSHGenCodec       thrpt   10   457469.129 ±   7083.221  ops/s
[info] JsonReadingBenchmark.readFoosCirce            thrpt   10     3016.276 ±     39.010  ops/s
[info] JsonReadingBenchmark.readFoosGenCodec         thrpt   10     3098.885 ±     39.858  ops/s
[info] JsonReadingBenchmark.readFoosUpickle          thrpt   10     3083.453 ±     29.865  ops/s
[info] JsonReadingBenchmark.readPrimitivesCirce      thrpt   10  1438760.846 ±  26739.354  ops/s
[info] JsonReadingBenchmark.readPrimitivesGenCodec   thrpt   10  1852565.866 ±  16059.918  ops/s
[info] JsonReadingBenchmark.readPrimitivesUpickle    thrpt   10  1889290.939 ±  24997.719  ops/s
[info] JsonReadingBenchmark.readSHCirce              thrpt   10   278322.679 ±   3304.612  ops/s
[info] JsonReadingBenchmark.readSHGenCodec           thrpt   10   533291.220 ±   4675.452  ops/s
[info] JsonReadingBenchmark.readSHUpickle            thrpt   10   298463.522 ±   3644.145  ops/s
[info] JsonWritingBenchmark.writeCCCirce             thrpt   10   688167.968 ±   8117.737  ops/s
[info] JsonWritingBenchmark.writeCCGenCodec          thrpt   10  1529662.369 ±  26909.344  ops/s
[info] JsonWritingBenchmark.writeCCUpickle           thrpt   10   985345.145 ±  43860.322  ops/s
[info] JsonWritingBenchmark.writeFlatSHGenCodec      thrpt   10   810840.889 ±  52376.100  ops/s
[info] JsonWritingBenchmark.writeFoosCirce           thrpt   10     2896.501 ±    327.638  ops/s
[info] JsonWritingBenchmark.writeFoosGenCodec        thrpt   10     4520.711 ±    182.557  ops/s
[info] JsonWritingBenchmark.writeFoosUpickle         thrpt   10     3096.716 ±    199.797  ops/s
[info] JsonWritingBenchmark.writePrimitivesCirce     thrpt   10  1263376.319 ±  49751.056  ops/s
[info] JsonWritingBenchmark.writePrimitivesGenCodec  thrpt   10  2904553.884 ± 207071.275  ops/s
[info] JsonWritingBenchmark.writePrimitivesUpickle   thrpt   10  2142443.367 ±  85212.161  ops/s
[info] JsonWritingBenchmark.writeSHCirce             thrpt   10   190421.595 ±   4475.344  ops/s
[info] JsonWritingBenchmark.writeSHGenCodec          thrpt   10   912956.930 ±  17628.358  ops/s
[info] JsonWritingBenchmark.writeSHUpickle           thrpt   10   239291.635 ±   8626.623  ops/s
```
