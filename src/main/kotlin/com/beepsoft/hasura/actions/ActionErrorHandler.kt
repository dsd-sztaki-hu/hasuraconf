package com.beepsoft.hasura.actions

import kotlinx.serialization.Contextual
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice


/**
 * Handle ActionErrorException thrown from actions
 *
 * Note: @RestControllerAdvice must be defined in the same file as the class name otherwise Spring won't pick it up!
 */
@RestControllerAdvice
class ActionErrorHandler {
    @ExceptionHandler(ActionErrorException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleError(ex: ActionErrorException): ResponseEntity<HasuraActionError> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(HasuraActionError(
            message = ex.errorMessage ?: ex.toString(),
            extensions = buildMap {
                ex.errorCode?.let {
                    put("code", ex.errorCode!!)
                }
                ex.errorData?.let {
                    put("errorData", ex.errorData!!)
                }
                put("exception", ex.stackTraceToString())
            }
        ))
    }
}

data class HasuraActionError(
    val message: String,
    val extensions: Map<String, @Contextual Any>?
)
