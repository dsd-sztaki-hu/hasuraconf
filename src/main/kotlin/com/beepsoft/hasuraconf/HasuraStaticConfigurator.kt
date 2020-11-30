package com.beepsoft.hasuraconf

import com.google.common.net.HttpHeaders
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

/**
 * Hasura static configurator provides a way to load a "bulk" metadata file into Hasura in a safe way.
 * These bulk operations may contain, eg `run_sql` commands, or other non idempotent operations,  which when executed a
 * second time triggers a Hasura/Postgresql error. Eg. when you try to add the same columns twice, the second invocation
 * will fail.
 *
 * Running the following static configuration the second time
 *
 * ```
 *  {
 *   "hasuraconfLoadSeparately": true,
 *   "type": "bulk",
 *   "args": [
 *     {
 *       "hasuraconfIgnoreError": {
 *         "message":"column \"custom_user_data\" of relation \"users\" already exists",
 *         "status_code":"42701"
 *       },
 *       "type": "run_sql",
 *       "args": {
 *         "sql": "ALTER TABLE \"public\".\"users\" ADD COLUMN \"custom_user_data\" text NULL;",
 *         "cascade": false,
 *         "read_only": false
 *       }
 *     }
 *   ]
 * }
 * ```
 * ... we get this error from Hasura:
 *
 * ```
 * {
 *	"internal": {
 *		"statement": "ALTER TABLE \"public\".\"users\" ADD COLUMN \"custom_user_data\" text NULL;",
 *		"prepared": false,
 *		"error": {
 *			"exec_status": "FatalError",
 *			"hint": null,
 *			"message": "column \"custom_user_data\" of relation \"users\" already exists",
 *			"status_code": "42701",
 *			"description": null
 *		},
 *		"arguments": [
 *
 *		]
 *	},
 *	"path": "$.args[0].args",
 *	"error": "query execution failed",
 *	"code": "postgres-error"
 * }
 * ```
 *
 * HasuraStaticConfigurator allows catching and ignoring such errors, so that the static initialization can be
 * executed any number of times. The way to do it is to include a `hasuraconfIgnoreError` field in the operations, like
 * this:
 *
 * ```
 *  {
 *   "type": "bulk",
 *   "args": [
 *     {
 *       "hasuraconfIgnoreError": {
 *         "message":"column \"custom_user_data\" of relation \"users\" already exists",
 *         "status_code":"42701"
 *       },
 *       "type": "run_sql",
 *       "args": {
 *         "sql": "ALTER TABLE \"public\".\"users\" ADD COLUMN \"custom_user_data\" text NULL;",
 *         "cascade": false,
 *         "read_only": false
 *       }
 *     }
 *   ]
 * }
 * ```
 * With these when `HasuraStaticConfigurator` executes the above static configuration and Hasura responds with
 * an error containing the message `"column \"custom_user_data\" of relation \"users\" already exists"` and the
 * error code `"42701"` then this error will be ignored and the configuration will proceed with the next operation.
 *
 *
 *
 * Supported values:
 *
 * * hasuraconfLoadSeparately: in case type of the config is `bulk` this field can be put at the top leve object. If
 *   set to true it instructs HasuraStaticConfigurator to execute each operation in the `args` array one by one,
 *   looking for a separate `hasuraconfIgnoreError` in each of them. If `hasuraconfLoadSeparately` is not set
 *   or is false, then `hasuraconfIgnoreError` then `hasuraconfIgnoreError` is expected to be set on the top level
 *   and this ignore definition will be used in case any error happens during the execution of the configuration.
 *   Usually the static configuraiton is a "bulk" one and so `hasuraconfLoadSeparately` is set to true and
 *   a `hasuraconfIgnoreError` is defined for each subsequent operation in `args`.
 * * hasuraconfIgnoreError: can be either a single object or a list of objects containing any or a combination of the
 *   following fields. If all the fields match the values of the error object received form Hasura, then the error
 *   will be ignored. If fields do not match the values coming from Hasura, then the error will be propagated to the
 *   caller.
 *     * description: text which is to be matched against the Hasura error's `description` field
 *     * message: text which is to be matched against the Hasura error's `message` field
 *     * status_code: text which is to be matched against the Hasura error's `message` field
 * * hasuraconfComment: you can add free text command explaining, for example, why yoy need to handle an error
 *
 * `hasuraconfIgnoreError` should usually be a single object however there are cases when Hasura sends different
 * errors for the same operation. For example:
 * ```
 * {
 *    "hasuraconfComment": "For add_remote_schema we get error reports incosistently https://github.com/hasura/graphql-engine/issues/3980. To work around we match alternative error messages.",
 *    "hasuraconfIgnoreError":[
 *      {
 *        "description": "Key (name)=(REMOTE_SCHEMA_UCPPS_CONNECTOR) already exists."
 *      },
 *      {
 *        "description": "remote schema with name \"REMOTE_SCHEMA_UCPPS_CONNECTOR\" already exists"
 *     }
 *   ]
 * }
 * ```
 *
 * If `hasuraconfIgnoreError` is an array the error will be ignored if any of the ignore objects match the Hasura error.
 */
class HasuraStaticConfigurator(
        var hasuraEndpoint: String,
        var hasuraAdminSecret: String? = null
) {

    fun loadStaticConf(staticConf: String) {
        val confJson = Json.parseToJsonElement(staticConf) as JsonObject
        var loadSeparately = false
        if ((confJson["type"] as JsonPrimitive).content == "bulk" &&
                confJson.containsKey("hasuraconfLoadSeparately") && (confJson["hasuraconfLoadSeparately"] as JsonPrimitive).boolean == true) {
            loadSeparately = true
        }

        // If loading separately then args must be an array of operations that we execute separately
        if (loadSeparately) {
            val args = confJson.get("args") as JsonArray
            for (o in args) {
                doLoadStaticInit(o as JsonObject)
            }
        } else {
            doLoadStaticInit(confJson)
        }
    }

    private fun doLoadStaticInit(confJson: JsonObject) {
        HasuraConfigurator.LOG.info("Executing static Hasura initialization JSON:")
        HasuraConfigurator.LOG.info(confJson.toString())
        val client = WebClient
                .builder()
                .baseUrl(hasuraEndpoint)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Hasura-Admin-Secret", hasuraAdminSecret)
                .build()
        // Make it synchronous for now
        try {
            val result = client.post().body(Mono.just<String>(Json.encodeToString(confJson)), String::class.java)
                    .retrieve().bodyToMono(String::class.java).block()
            HasuraConfigurator.LOG.info("Static Hasura initialization done {}", result)
        } catch (ex: WebClientResponseException) {

            // Check if we received an "expected" error, ie. one that matched the error defined in the
            // confJson's diwasIgnoreError field. If we have a match, we ignore this error. It must be an error
            // which is eg. about some value already set. In this case we can ignore this error safely.
            if (canIgnoreError(confJson, Json.parseToJsonElement(ex.responseBodyAsString) as JsonObject)) {
                HasuraConfigurator.LOG.info("Static Hasura configuration had error, but can be ignored: {}", ex.responseBodyAsString)
                return
            }
            HasuraConfigurator.LOG.error("Hasura configuration failed", ex)
            HasuraConfigurator.LOG.error("Response text: {}", ex.responseBodyAsString)
            throw ex
        } finally {
        }
    }

    private fun canIgnoreError(confJson: JsonObject, errorJson: JsonObject): Boolean {
        // Set to true by default. If a value is set for any of these we will evaluate those only
        var statusCodeMatches = true
        var messageMatches = true
        var descriptionMatches = true
        var attemptToIgnoreError = false
        if (confJson.containsKey("hasuraconfIgnoreError")) {
            attemptToIgnoreError = true
            val obj = confJson.get("hasuraconfIgnoreError")
            if (obj is JsonObject) {
                return canIgnoreErrorWithHasuraconfIgnoreError(obj as JsonObject, errorJson)
            } else {
                for (o in obj as JsonArray) {
                    if (canIgnoreErrorWithHasuraconfIgnoreError(o as JsonObject, errorJson)) {
                        return true
                    }
                }
            }
            return false
        }
        if (confJson.containsKey("hasuraconfIgnoreErrorStatusCode")) {
            attemptToIgnoreError = true
            statusCodeMatches = isErrorFieldMatches("statusCode", (confJson.get("hasuraconfIgnoreErrorStatusCode") as JsonPrimitive).content, errorJson)
        }
        if (confJson.containsKey("hasuraconfIgnoreErrorMessage")) {
            attemptToIgnoreError = true
            messageMatches = isErrorFieldMatches("message", (confJson.get("hasuraconfIgnoreErrorMessage") as JsonPrimitive).content, errorJson)
        }
        if (confJson.containsKey("hasuraconfIgnoreErrorDescription")) {
            attemptToIgnoreError = true
            descriptionMatches = isErrorFieldMatches("description", (confJson.get("hasuraconfIgnoreErrorDescription") as JsonPrimitive).content, errorJson)
        }
        return statusCodeMatches && messageMatches && descriptionMatches && attemptToIgnoreError
    }

    private fun canIgnoreErrorWithHasuraconfIgnoreError(ignoreErrorJson: JsonObject, errorJson: JsonObject): Boolean {
        var statusCodeMatches = true
        val messageMatches = true
        val descriptionMatches = true
        var attemptToIgnoreError = false
        if (ignoreErrorJson.containsKey("statusCode")) {
            attemptToIgnoreError = true
            statusCodeMatches = isErrorFieldMatches("statusCode", (ignoreErrorJson.get("statusCode") as JsonPrimitive).content, errorJson)
        }
        if (ignoreErrorJson.containsKey("message")) {
            attemptToIgnoreError = true
            statusCodeMatches = isErrorFieldMatches("message", (ignoreErrorJson.get("message") as JsonPrimitive).content, errorJson)
        }
        if (ignoreErrorJson.containsKey("description")) {
            attemptToIgnoreError = true
            statusCodeMatches = isErrorFieldMatches("description", (ignoreErrorJson.get("description")  as JsonPrimitive).content, errorJson)
        }
        return statusCodeMatches && messageMatches && descriptionMatches && attemptToIgnoreError
    }

    private fun isErrorFieldMatches(field: String, value: String, errorJson: JsonObject): Boolean {
        if (errorJson.containsKey("internal") && (errorJson.get("internal") as JsonObject).containsKey("error")) {
            val errorObj = (errorJson.get("internal") as JsonObject).get("error") as JsonObject
            if (field == "description" && (errorObj.get("description") as JsonPrimitive).content == value) {
                return true
            }
            if (field == "statusCode" && (errorObj.get("status_code") as JsonPrimitive).content == value) {
                return true
            }
            if (field == "message" && (errorObj.get("message") as JsonPrimitive).content == value) {
                return true
            }
        } else if (errorJson.containsKey("error")) {
            if (field == "description") {
                return (errorJson.get("error") as JsonPrimitive).content == value
            }
        }
        return false
    }
}