package com.beepsoft.hasuraconf.annotation

import io.hasura.metadata.v3.ResponseTransformation
import java.lang.annotation.Inherited
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import kotlin.reflect.KClass

/**
 * Marks a function as a template for defining the types of a Hasura action.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraAction (
    /**
     * Explicit name for the action, if want to override default Java/Kotlin method name.
     */
    val name : String = "",

    /**
     * Type of the action: mutation or string. Typically mutation
     */
    val type : HasuraActionType = HasuraActionType.MUTATION,

    /**
     * Synchonicity of the action
     */
    val kind : HasuraActionKind = HasuraActionKind.SYNCHRONOUS,

    /**
     * The handler's URL or the environment variable configured in Hasura, eg. "{{ACTION_ENDPOINT_ENV_VAR}}"
     */
    val handler: String = "",

    /**
     * Forward client headers when calling the action?
     */
    val forwardClientHeaders: Boolean = true,

    /**
     * Specific headers to forward when forwardClientHeaders is true
     */
    val headers: Array<HasuraHeader> = [],

    /**
     * Description of the action.
     */
    val comment: String = "",

    /**
     * Roles that can invoke the action.
     */
    val permissions: Array<String> = [],

    /**
     * Timeout to wait for action to finish
     */
    val timeout: Long = 0,

    /**
     * Marks whether method arguments should be wrapped in an "input type" the fields of which match the aguments.
     */
    val wrapArgsInType: Boolean = false,

    /**
     * Override output type name in case return type is an Object and don't want to use its Java/Kotlin name.
     */
    val outputTypeName: String = "",

    /**
     * Alternative output type to use instead of what is declared for the action. Eg. when the action is a Spring
     * and the return type is a `ResponseEntity<ActualOutputType>` the outputType should be set to ActualOutputType
     * so that the action generator doesn't try to map ResponseEntity  to graphql.
     */
    val outputType: KClass<*> = Void::class,

    /**
     * Request transformation for a REST call to be exposed as this action
     */
    val requestTransform: HasuraRequestTransform = HasuraRequestTransform(
        body = "<<<empty>>>",
        method = HasuraHttpMethod.GET,
        url = "<<<empty>>>"
    ),

    val responseTransform: HasuraResponseTransform = HasuraResponseTransform()
)

/**
 * An HTTP Header with name/value
 */
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraHeader (
    val name: String,
    // Either value or valueFromEnv must be set
    val value: String = "",
    val valueFromEnv: String = ""
)

enum class HasuraActionType {
    MUTATION,
    QUERY,
}

enum class HasuraActionKind {
    SYNCHRONOUS,
    ASYNCHRONOUS
}

