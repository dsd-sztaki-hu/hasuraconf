package com.beepsoft.hasuraconf.actions

import com.beepsoft.hasura.actions.ActionPayload
import com.beepsoft.hasuraconf.annotation.HasuraAction
import com.beepsoft.hasuraconf.annotation.HasuraField
import com.beepsoft.hasuraconf.annotation.HasuraIgnoreField
import com.beepsoft.hasuraconf.annotation.HasuraIgnoreParameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlinx.serialization.json.*
import kotlinx.serialization.json.decodeFromJsonElement
import javax.servlet.http.HttpServletRequest


@RestController
class StartTaskAction(
    @PersistenceContext
    val em: EntityManager
) {

    @HasuraAction(
        handler = "{{ACTION_HANDLER_ENDPOINT}}",
        comment = "Start a task",
        outputType = StartTaskOutput::class,
        timeout = 150,
        permissions = ["public"],
    )
    @PostMapping("/actions/startTask")
    @Transactional
    fun startTask(
        @RequestBody args:StartTaskInput,
        @HasuraIgnoreParameter
        req: HttpServletRequest): ResponseEntity<StartTaskOutput>
    {
        return ResponseEntity(StartTaskOutput("ACCEPTED", args.taskId.toString()), HttpStatus.OK)
    }

}

data class StartTaskInput(
    @HasuraField(description = "ID of the task to start")
    val taskId: Long,

    // This is ignored from graphql, but at runtime we will receive the raw Hasura action payload in this field
    // provided by the ActionsFilter
    @HasuraIgnoreField
    val actionPayload: ActionPayload
)

// This is the same as DiwasStartTaskResult but as a graphql output type
@Serializable
data class StartTaskOutput(
    @HasuraField(description = "ACCEPTED if task is tarted and CANCELLED if not, for example, if no free user is available")
    val status: String,

    @HasuraField(description = "ID of the executed task used for referencing this execution in further calls")
    val executedTaskId: String? = null,

    @HasuraField(description = "Reason of cancellation")
    val statusReason: String?  = null,

    @HasuraField(description = "Explanation of  case it is not ACCEPTED")
    val statusMessage: String?  = null
)

