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

        println("Generated actions:\n"+json)
        //var actions = g.generateActionMetadata(listOf("com.beepsoft.hasuraconf.model.actions1")).toString()
        JSONAssert.assertEquals(
        //Assertions.assertEquals(
            """{"actions":[{"definition":{"arguments":[{"name":"args","type":"UserAndCalendarInput!"}],"forward_client_headers":true,"handler":"{{HANDLER_URL}}","kind":"synchronous","output_type":"[UserAndCalendarOutput5!]","type":"mutation"},"name":"createUserAndCalendar5"},{"definition":{"arguments":[{"name":"args","type":"UserAndCalendarInput!"}],"forward_client_headers":true,"handler":"{{HANDLER_URL}}","kind":"synchronous","output_type":"UserAndCalendarOutput","type":"mutation"},"name":"createUserAndCalendar4"},{"definition":{"arguments":[{"name":"name","type":"String!"},{"name":"descriptions","type":"[String!]!"},{"name":"calendarTypes","type":"[CalendarType!]!"}],"forward_client_headers":true,"handler":"{{HANDLER_URL}}","kind":"synchronous","output_type":"String","type":"mutation"},"name":"createUserAndCalendar2"},{"definition":{"arguments":[{"name":"args","type":"UserAndCalendarInput!"}],"forward_client_headers":true,"handler":"{{HANDLER_URL}}","kind":"synchronous","output_type":"[String!]","type":"mutation"},"name":"createUserAndCalendar3"},{"definition":{"arguments":[{"name":"userName","type":"String!"},{"name":"name","type":"String!"},{"name":"description","type":"String!"},{"name":"calendarType","type":"CalendarType!"}],"forward_client_headers":true,"handler":"{{HANDLER_URL}}","kind":"synchronous","output_type":"String","type":"mutation"},"name":"createUserAndCalendar"},{"definition":{"arguments":[{"name":"args","type":"SignUpWithExternalRestApiInput!"}],"forward_client_headers":true,"handler":"http://some.rest.endpoint","kind":"synchronous","output_type":"SignUpWithExternalRestApiOutput","type":"mutation","request_transform":{"version":1,"method":"POST","url":"{{${'$'}base_url}}/signup/email-password","body":"{\"email\":{{${'$'}body.input.args.email}},\"password\":{{${'$'}body.input.args.password}}}","query_params":{},"template_engine":"Kriti"}},"name":"signUpWithExternalRestApi"}],"custom_types":{"enums":[{"name":"CalendarType","values":[{"value":"PRIVATE"},{"value":"PUBLIC"},{"value":"SHARED"}]}],"input_objects":[{"fields":[{"name":"name","type":"String!"},{"name":"description","type":"String!"},{"name":"isPublic","type":"Boolean!"},{"name":"hasColors","type":"Boolean"}],"name":"UserAndCalendarInput"},{"fields":[{"name":"email","type":"String!"},{"name":"password","type":"String!"}],"name":"SignUpWithExternalRestApiInput"}],"objects":[{"description":"The description of UserAndCalendarOutput","fields":[{"description":"The user's name","name":"userFullName","type":"String!"},{"description":"The user's age","name":"userAge","type":"Int"},{"description":"Field is not defined in Kotlin as nullable, but explicitly set so","name":"explicitNullable","type":"String"},{"description":"Field is defined in Kotlin as nullable, but explicitly set to not nullable","name":"explicitlyNotNullable","type":"String!"},{"description":"User identifier","name":"userId","type":"bigint!"},{"name":"calendarId","type":"bigint!"},{"name":"differentCalendarId","type":"bigint!"}],"name":"UserAndCalendarOutput5","relationships":[{"field_mapping":{"calendarId":"id"},"name":"calendar","remote_table":{"name":"calendar","schema":"public"},"type":"object"},{"field_mapping":{"differentCalendarId":"id"},"name":"otherCalendar","remote_table":{"name":"calendar","schema":"public"},"type":"object"}]},{"description":"The description of UserAndCalendarOutput","fields":[{"description":"The user's name","name":"userFullName","type":"String!"},{"description":"The user's age","name":"userAge","type":"Int"},{"description":"Field is not defined in Kotlin as nullable, but explicitly set so","name":"explicitNullable","type":"String"},{"description":"Field is defined in Kotlin as nullable, but explicitly set to not nullable","name":"explicitlyNotNullable","type":"String!"},{"description":"User identifier","name":"userId","type":"bigint!"},{"name":"calendarId","type":"bigint!"},{"name":"differentCalendarId","type":"bigint!"}],"name":"UserAndCalendarOutput","relationships":[{"field_mapping":{"calendarId":"id"},"name":"calendar","remote_table":{"name":"calendar","schema":"public"},"type":"object"},{"field_mapping":{"differentCalendarId":"id"},"name":"otherCalendar","remote_table":{"name":"calendar","schema":"public"},"type":"object"}]},{"fields":[{"name":"mfa","type":"String!"},{"name":"session","type":"String!"}],"name":"SignUpWithExternalRestApiOutput"}],"scalars":[{"description":"bigint type","name":"bigint"}]}}""",
            json,
            true
        )
    }
}
