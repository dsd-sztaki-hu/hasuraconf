package com.beepsoft.hasura.actions

import kotlinx.serialization.Contextual

/**
 * Exception thrown from graphql actions
 */
class ActionErrorException(
    var errorMessage: String,
    var errorCode: String? = null,
    var errorData: Map<String, @Contextual Any>? = null
) : RuntimeException(errorMessage)

