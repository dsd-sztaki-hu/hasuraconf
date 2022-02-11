package com.beepsoft.hasuraconf.annotation

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy


/**
 *  Ignore a field from a type. Can be used for action types when some fields should not be exposed to graphql,
 *  but are used at runtime by the action implementation and can be provided by eg. an HTTP middleware. One
 *  use case is when the @HasuraAction is not used only to define action metadata, but it is also the actual
 *  implementation of the action in kotlin. In spring we can have a Filter which passes the full action payload
 *  in a HasuraIgnoreField so that the action function as all the information not just the dedicated
 *  input data.
 */
@Target(AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraIgnoreField()
