package com.beepsoft.hasuraconf

import io.hasura.metadata.v3.InputObjectType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

class HasuraActionGeneratorTest {

    @Test
    fun testActionGenerator()
    {
        val g = HasuraActionGenerator()
        val actionsAndCustomTypes = g.configureActions(listOf("com.beepsoft.hasuraconf.model.actions1"))
        val json = buildJsonObject {
            put("actions", Json.encodeToJsonElement(actionsAndCustomTypes.actions))
            put("custom_types", Json.encodeToJsonElement(actionsAndCustomTypes.customTypes))
        }.toString()

        //var actions = g.generateActionMetadata(listOf("com.beepsoft.hasuraconf.model.actions1")).toString()
        JSONAssert.assertEquals(
        //Assertions.assertEquals(
            """{"actions":[{"name":"createUserAndCalendar5","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"[UserAndCalendarOutput5!]","arguments":[{"name":"args","type":"UserAndCalendarInput!"}]}},{"name":"createUserAndCalendar4","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"UserAndCalendarOutput","arguments":[{"name":"args","type":"UserAndCalendarInput!"}]}},{"name":"createUserAndCalendar2","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"String","arguments":[{"name":"name","type":"String!"},{"name":"descriptions","type":"[String!]!"},{"name":"calendarTypes","type":"[CalendarType!]!"}]}},{"name":"createUserAndCalendar3","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"[String!]","arguments":[{"name":"args","type":"UserAndCalendarInput!"}]}},{"name":"createUserAndCalendar","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"String","arguments":[{"name":"userName","type":"String!"},{"name":"name","type":"String!"},{"name":"description","type":"String!"},{"name":"calendarType","type":"CalendarType!"}]}},{"name":"signUpWithExternalRestApi","definition":{"handler":"http://some.rest.endpoint","type":"mutation","kind":"synchronous","forward_client_headers":true,"request_transform":{"body":"{\"email\":{{${'$'}body.input.args.email}},\"password\":{{${'$'}body.input.args.password}}}","url":"{{${'$'}base_url}}/signup/email-password","content_type":"application/json","method":"POST","query_params":{},"template_engine":"Kriti"},"output_type":"SignUpWithExternalRestApiOutput","arguments":[{"name":"args","type":"SignUpWithExternalRestApiInput!"}]}}],"custom_types":{"input_objects":[{"name":"UserAndCalendarInput","fields":[{"name":"name","type":"String!"},{"name":"description","type":"String!"},{"name":"isPublic","type":"Boolean!"},{"name":"hasColors","type":"Boolean"}]},{"name":"SignUpWithExternalRestApiInput","fields":[{"name":"email","type":"String!"},{"name":"password","type":"String!"}]}],"objects":[{"name":"UserAndCalendarOutput5","description":"The description of UserAndCalendarOutput","fields":[{"description":"The user's name","name":"userFullName","type":"String!"},{"description":"The user's age","name":"userAge","type":"Int"},{"description":"Field is not defined in Kotlin as nullable, but explicitly set so","name":"explicitNullable","type":"String"},{"description":"Field is defined in Kotlin as nullable, but explicitly set to not nullable","name":"explicitlyNotNullable","type":"String!"},{"description":"User identifier","name":"userId","type":"bigint!"},{"name":"calendarId","type":"bigint!"},{"name":"differentCalendarId","type":"bigint!"}],"relationships":[{"name":"calendar","type":"object","remote_table":{"schema":"public","name":"calendar"},"field_mapping":{"calendarId":"id"}},{"name":"otherCalendar","type":"object","remote_table":{"schema":"public","name":"calendar"},"field_mapping":{"differentCalendarId":"id"}}]},{"name":"UserAndCalendarOutput","description":"The description of UserAndCalendarOutput","fields":[{"description":"The user's name","name":"userFullName","type":"String!"},{"description":"The user's age","name":"userAge","type":"Int"},{"description":"Field is not defined in Kotlin as nullable, but explicitly set so","name":"explicitNullable","type":"String"},{"description":"Field is defined in Kotlin as nullable, but explicitly set to not nullable","name":"explicitlyNotNullable","type":"String!"},{"description":"User identifier","name":"userId","type":"bigint!"},{"name":"calendarId","type":"bigint!"},{"name":"differentCalendarId","type":"bigint!"}],"relationships":[{"name":"calendar","type":"object","remote_table":{"schema":"public","name":"calendar"},"field_mapping":{"calendarId":"id"}},{"name":"otherCalendar","type":"object","remote_table":{"schema":"public","name":"calendar"},"field_mapping":{"differentCalendarId":"id"}}]},{"name":"SignUpWithExternalRestApiOutput","fields":[{"name":"mfa","type":"String!"},{"name":"session","type":"String!"}]}],"scalars":[{"name":"bigint","description":"bigint type"}],"enums":[{"name":"CalendarType","values":[{"value":"PRIVATE"},{"value":"PUBLIC"},{"value":"SHARED"}]}]}}""",
            json,
            true
        )
    }
}
