package com.avsystem.commons
package rest.openapi

import com.avsystem.commons.meta._
import com.avsystem.commons.rest.{Header => HeaderAnnot, _}
import com.avsystem.commons.rpc._
import com.avsystem.commons.serialization.{transientDefault, whenAbsent}

@methodTag[RestMethodTag]
case class OpenApiMetadata[T](
  @multi @tagged[Prefix](whenUntagged = new Prefix)
  @paramTag[RestParamTag](defaultTag = new Path)
  @rpcMethodMetadata
  prefixes: Mapping[OpenApiPrefix[_]],

  @multi @tagged[GET]
  @paramTag[RestParamTag](defaultTag = new Query)
  @rpcMethodMetadata
  gets: Mapping[OpenApiGetOperation[_]],

  @multi @tagged[BodyMethodTag](whenUntagged = new POST)
  @paramTag[RestParamTag](defaultTag = new JsonBodyParam)
  @rpcMethodMetadata
  bodyMethods: Mapping[OpenApiBodyOperation[_]]
) {
  val httpMethods: Mapping[OpenApiOperation[_]] =
    (gets: Mapping[OpenApiOperation[_]]) ++ (bodyMethods: Mapping[OpenApiOperation[_]])

  def operations(resolver: SchemaResolver): Iterator[(String, HttpMethod, Operation)] =
    prefixes.valuesIterator.flatMap(_.operations(resolver)) ++
      httpMethods.valuesIterator.map(m => (m.pathPattern, m.methodTag.method, m.operation(resolver)))

  def paths(resolver: SchemaResolver): Paths = {
    val pathsMap = new MLinkedHashMap[String, MLinkedHashMap[HttpMethod, Operation]]
    operations(resolver).foreach {
      case (path, httpMethod, operation) =>
        val opsMap = pathsMap.getOrElseUpdate(path, new MLinkedHashMap)
        opsMap(httpMethod) = operation
    }
    Paths(pathsMap.iterator.map { case (path, ops) =>
      val pathItem = PathItem(
        get = ops.getOpt(HttpMethod.GET).toOptArg,
        put = ops.getOpt(HttpMethod.PUT).toOptArg,
        post = ops.getOpt(HttpMethod.POST).toOptArg,
        patch = ops.getOpt(HttpMethod.PATCH).toOptArg,
        delete = ops.getOpt(HttpMethod.DELETE).toOptArg,
      )
      (path, RefOr(pathItem))
    }.toMap)
  }

  def openapi(info: Info): OpenApi = {
    val registry = new SchemaRegistry(n => s"#/components/schemas/$n")
    OpenApi(OpenApi.Version, info, paths(registry), components = Components(schemas = registry.registeredSchemas))
  }
}
object OpenApiMetadata extends RpcMetadataCompanion[OpenApiMetadata]

sealed trait OpenApiMethod[T] extends TypedMetadata[T] {
  @reifyAnnot def methodTag: RestMethodTag
  @multi
  @rpcParamMetadata
  @tagged[NonBodyTag] def parameters: List[OpenApiParameter[_]]

  val pathPattern: String = {
    val pathParts = methodTag.path :: parameters.flatMap {
      case OpenApiParameter(path: Path, info) =>
        s"{${info.name}}" :: path.pathSuffix :: Nil
      case _ => Nil
    }
    pathParts.iterator.map(_.stripPrefix("/").stripSuffix("/")).filter(_.nonEmpty).mkString("/", "/", "")
  }
}

case class OpenApiPrefix[T](
  methodTag: Prefix,
  parameters: List[OpenApiParameter[_]],
  @infer @checked result: OpenApiMetadata.Lazy[T]
) extends OpenApiMethod[T] {

  def operations(resolver: SchemaResolver): Iterator[(String, HttpMethod, Operation)] = {
    val prefixParams = parameters.map(_.parameter(resolver))
    result.value.operations(resolver).map { case (path, httpMethod, operation) =>
      (pathPattern + path, httpMethod, operation.copy(parameters = prefixParams ++ operation.parameters))
    }
  }
}

sealed trait OpenApiOperation[T] extends OpenApiMethod[T] {
  @infer
  @checked def responseType: HttpResponseType[T]
  def methodTag: HttpMethodTag
  def requestBody(resolver: SchemaResolver): RefOr[RequestBody]

  def operation(resolver: SchemaResolver): Operation = Operation(
    responseType.responses(resolver),
    parameters = parameters.map(_.parameter(resolver)),
    requestBody = requestBody(resolver)
  )
}

case class OpenApiGetOperation[T](
  methodTag: HttpMethodTag,
  parameters: List[OpenApiParameter[_]],
  responseType: HttpResponseType[T]
) extends OpenApiOperation[T] {
  def requestBody(resolver: SchemaResolver): RefOr[RequestBody] =
    RefOr(RequestBody(content = Map.empty))
}

case class OpenApiBodyOperation[T](
  methodTag: HttpMethodTag,
  parameters: List[OpenApiParameter[_]],
  @multi @rpcParamMetadata @tagged[JsonBodyParam] bodyParams: List[OpenApiParamInfo[_]],
  @optional @encoded @rpcParamMetadata @tagged[Body] singleBody: Opt[OpenApiBody[_]],
  responseType: HttpResponseType[T]
) extends OpenApiOperation[T] {

  def requestBody(resolver: SchemaResolver): RefOr[RequestBody] =
    singleBody.map(_.requestBody.requestBody(resolver)).getOrElse {
      if (bodyParams.isEmpty) RefOr(RequestBody(content = Map.empty))
      else {
        val schema = Schema(`type` = DataType.Object,
          properties = bodyParams.iterator.map(p => (p.name, resolver.resolve(p.restSchema))).toMap,
          required = bodyParams.collect { case p if !p.hasFallbackValue => p.name }
        )
        RefOr(RestRequestBody.jsonRequestBody(RefOr(schema)))
      }
    }
}

case class OpenApiParamInfo[T](
  @reifyName(useRawName = true) name: String,
  @optional @reifyAnnot whenAbsent: Opt[whenAbsent[T]],
  @isAnnotated[transientDefault] transientDefault: Boolean,
  @reifyFlags flags: ParamFlags,
  @infer restSchema: RestSchema[T]
) extends TypedMetadata[T] {
  val hasFallbackValue: Boolean =
    whenAbsent.fold(flags.hasDefaultValue)(wa => Try(wa.value).isSuccess)
}

case class OpenApiParameter[T](
  @reifyAnnot paramTag: NonBodyTag,
  @composite info: OpenApiParamInfo[T]
) extends TypedMetadata[T] {

  def parameter(resolver: SchemaResolver): RefOr[Parameter] = {
    val in = paramTag match {
      case _: Path => Location.Path
      case _: HeaderAnnot => Location.Header
      case _: Query => Location.Query
    }
    RefOr(Parameter(info.name, in, required = !info.hasFallbackValue, schema = resolver.resolve(info.restSchema)))
  }
}

case class OpenApiBody[T](
  @infer requestBody: RestRequestBody[T]
) extends TypedMetadata[T]
