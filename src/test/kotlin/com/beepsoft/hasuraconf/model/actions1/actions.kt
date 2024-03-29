package com.beepsoft.hasuraconf.model.actions1

import com.beepsoft.hasuraconf.annotation.*
import com.beepsoft.hasuraconf.model.Calendar
import org.springframework.http.ResponseEntity

// createUserAndCalendar and createUserAndCalendar2 have same input params, but will have differenet input types
// because the params are all primitives
@HasuraAction(
    handler = "{{HANDLER_URL}}"
)
fun createUserAndCalendar(
    userName: String,
    name: String,
    description: String,
    calendarType: CalendarType
): String {
    TODO()
}

@HasuraAction(
    handler = "{{HANDLER_URL}}"
)
fun createUserAndCalendar2(
    name: String,
    descriptions: Array<String>,
    calendarTypes: Array<CalendarType>
): String {
    TODO()
}

// createUserAndCalendar3 and createUserAndCalendar4 will use the same UserAndCalendarInput type but their output
// types will differ
@HasuraAction(
    handler = "{{HANDLER_URL}}"
)
fun createUserAndCalendar3(
    args: UserAndCalendarInput,
    @HasuraIgnoreParameter
    ignorable: String,
): Array<String> {
    TODO()
}

class ActionTest {
    @HasuraAction(
        handler = "{{HANDLER_URL}}",
        outputType = UserAndCalendar::class
    )
    fun createUserAndCalendar4(
        args: UserAndCalendarInput,
    ): ResponseEntity<UserAndCalendar>
    {
        TODO()
    }

    @HasuraAction(
        handler = "{{HANDLER_URL}}",
        outputTypeName = "UserAndCalendarOutput5"
    )
    fun createUserAndCalendar5(
        args: UserAndCalendarInput,
    ): Array<UserAndCalendar>
    {
        TODO()
    }

}

@HasuraType("UserAndCalendarOutput", description="The description of UserAndCalendarOutput")
data class UserAndCalendar(
    @HasuraField(description = "The user's name")
    var userFullName: String,

    @HasuraField(description = "The user's age")
    var userAge: Int?,

    @HasuraField(description = "Field is not defined in Kotlin as nullable, but explicitly set so", nullable = Nullable.TRUE)
    var explicitNullable: String,

    @HasuraField(description = "Field is defined in Kotlin as nullable, but explicitly set to not nullable", nullable = Nullable.FALSE)
    var explicitlyNotNullable: String?,

    @HasuraField(type="bigint!", description = "User identifier", typeDescription = "bigint type")
    var userId: Long,
    @HasuraRelationship(
        name="calendar",
        remoteTable = "calendar",
        graphqlFieldType = "bigint!",
        fieldMappings = [
            HasuraFieldMapping(fromField="calendarId", toField="id")
        ]
    )
    var calendar: Calendar,
    @HasuraRelationship(
        name="otherCalendar",
        remoteTable = "calendar",
        fieldMappings = [
            HasuraFieldMapping(fromField="differentCalendarId", toField="id")
        ]
    )
    var differentCalendarId: Long
)

data class UserAndCalendarInput(
    val name: String,
    val description: String,
    val isPublic: Boolean,
    val hasColors: Boolean?,
    @HasuraIgnoreField
    val runtimeField: Map<String, Any>
)


enum class CalendarType {
    PRIVATE,
    PUBLIC,
    SHARED
}


@HasuraAction(
    handler = "http://some.rest.endpoint",
    requestTransform = HasuraRequestTransform(
        url = "{{\$base_url}}/signup/email-password",
        method = HasuraHttpMethod.POST,
        body = """
            {
                "email": {{${'$'}body.input.args.email}},
                "password": {{${'$'}body.input.args.password}}
            }
        """
    )
)
fun signUpWithExternalRestApi(
    args: SignUpWithExternalRestApiInput
): SignUpWithExternalRestApiOutput {
    TODO()
}

data class SignUpWithExternalRestApiInput(
    val email: String,
    val password: String
)

data class SignUpWithExternalRestApiOutput(
    val mfa: String,
    val session: String
)
