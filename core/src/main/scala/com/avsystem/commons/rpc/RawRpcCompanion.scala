//package com.avsystem.commons
//package rpc
//
///**
//  * Base trait for companion objects of raw RPC traits.
//  */
//trait RawRpcCompanion[Raw] {
//  type AsRawRpc[Real] = AsRaw[Raw, Real]
//  type AsRealRpc[Real] = AsReal[Raw, Real]
//  type AsRawRealRpc[Real] = AsRawReal[Raw, Real]
//
//  def asRealRpc[Real](using asReal: AsRealRpc[Real]): AsRealRpc[Real] = asReal
//  def asRawRpc[Real](using asRaw: AsRawRpc[Real]): AsRawRpc[Real] = asRaw
//  def asRawRealRpc[Real](using asRawReal: AsRawRealRpc[Real]): AsRawRealRpc[Real] = asRawReal
//
//  def asReal[Real](raw: Raw)(using asRealRpc: AsRealRpc[Real]): Real = asRealRpc.asReal(raw)
//  def asRaw[Real](real: Real)(using asRawRpc: AsRawRpc[Real]): Raw = asRawRpc.asRaw(real)
//
//  def materializeAsRaw[Real]: AsRawRpc[Real] = RpcMacros.rpcAsRaw[Raw, Real]
//  def materializeAsReal[Real]: AsRealRpc[Real] = RpcMacros.rpcAsReal[Raw, Real]
//  def materializeAsRawReal[Real]: AsRawRealRpc[Real] = RpcMacros.rpcAsRawReal[Raw, Real]
//
//  /**
//    * Like [[materializeAsRaw]] but for arbitrary real type instead of RPC trait.
//    * Scans all public methods of the real type (instead of abstract methods for RPC trait).
//    */
//  def materializeApiAsRaw[Real]: AsRawRpc[Real] = RpcMacros.apiAsRaw[Raw, Real]
//}
