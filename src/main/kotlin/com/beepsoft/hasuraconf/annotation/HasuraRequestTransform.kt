package com.beepsoft.hasuraconf.annotation

import io.hasura.metadata.v3.TransformHeaders
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

//           "request_transform": {
//            "body": "{\n    \"email\": {{$body.input.args.email}},\n    \"password\": {{$body.input.args.password}}\n}",
//            "url": "{{$base_url}}/signup/email-password",
//            "content_type": "application/json",
//            "method": "POST",
//            "query_params": {},
//            "template_engine": "Kriti"
/**
 * Used for defining transforms for actions or event triggers
 */
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraRequestTransform (
    val body: String,
    val url: String,
    val contentType: String = "application/json",
    val method: HasuraHttpMethod,
    val queryParams:Array<HasuraQueryParam> = [],
    val templateEngine: String = "Kriti",
    val requestHeaders: HasuraTransformHeaders = HasuraTransformHeaders(),
)

@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraTransformHeaders(
    val addHeaders:Array<HasuraTransformHeader> = [],
    val removeHeaders: Array<String> = []
)

@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraTransformHeader(
    val name: String,
    val value: String
)

