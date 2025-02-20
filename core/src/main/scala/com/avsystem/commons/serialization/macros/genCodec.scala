package com.avsystem.commons
package serialization.macros

import serialization.GenCodec

def materializeImpl[T: Type](using quotes: Quotes): Expr[GenCodec[T]] = '{
  ???
}
