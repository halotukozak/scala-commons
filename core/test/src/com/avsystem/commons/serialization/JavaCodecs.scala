package com.avsystem.commons
package serialization

object JavaCodecs {
  given buildablePojoCodec: GenCodec[BuildablePojo] =
    GenCodec.fromJavaBuilder(BuildablePojo.builder())(_.build())
}
