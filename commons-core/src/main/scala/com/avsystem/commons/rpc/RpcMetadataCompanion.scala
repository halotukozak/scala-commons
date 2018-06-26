package com.avsystem.commons
package rpc

import com.avsystem.commons.macros.rpc.RpcMacros

trait RpcMetadataCompanion[M[_]] extends RpcImplicitsProvider {
  def materializeForRpc[Real]: M[Real] = macro RpcMacros.rpcMetadata[M[Real], Real]

  final class Lazy[Real](metadata: => M[Real]) {
    lazy val value: M[Real] = metadata
  }
  object Lazy {
    def apply[Real](metadata: => M[Real]): Lazy[Real] = new Lazy(metadata)

    // macro effectively turns `metadata` param into by-name param (implicit params by themselves cannot be by-name)
    implicit def lazyMetadata[Real](implicit metadata: M[Real]): Lazy[Real] = macro RpcMacros.lazyMetadata
  }
}
