package com.beepsoft.hasuraconf

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class HasuraActionGeneratorTest {

    @Test
    fun testActionGenerator()
    {
        val g = HasuraActionGenerator()
        var actions = g.generateActionMetadata(listOf("com.beepsoft.hasuraconf.model.actions1")).toString()
        Assertions.assertEquals(
            """{"actions":[{"name":"createUserAndCalendar3","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"[String]","arguments":[{"name":"args","type":"UserAndCalendarInput"}]}},{"name":"createUserAndCalendar4","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"UserAndCalendarOutput","arguments":[{"name":"args","type":"UserAndCalendarInput"}]}},{"name":"createUserAndCalendar5","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"[UserAndCalendarOutput5!]","arguments":[{"name":"args","type":"UserAndCalendarInput"}]}},{"name":"createUserAndCalendar2","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"String","arguments":[{"name":"name","type":"String"},{"name":"descriptions","type":"[String]"},{"name":"calendarTypes","type":"[CalendarType]"}]}},{"name":"createUserAndCalendar","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"String","arguments":[{"name":"userName","type":"String"},{"name":"name","type":"String"},{"name":"description","type":"String"},{"name":"calendarType","type":"CalendarType"}]}}],"custom_types":{"input_objects":[{"name":"UserAndCalendarInput","fields":[{"name":"name","type":"String"},{"name":"description","type":"String"}]}],"objects":[{"name":"UserAndCalendarOutput","description":"The description of UserAndCalendarOutput","fields":[{"description":"The user's name","name":"userName","type":"String"},{"name":"userId","type":"bigint!"},{"name":"calendarId","type":"bigint!"},{"name":"differentCalendarId","type":"bigint"}],"relationships":[{"name":"calendar","type":"object","remote_table":{"name":"calendar","schema":"public"},"field_mappings":[{"calendarId":"id"}]},{"name":"otherCalendar","type":"object","remote_table":{"name":"calendar","schema":"public"},"field_mappings":[{"differentCalendarId":"id"}]}]},{"name":"UserAndCalendarOutput5","description":"The description of UserAndCalendarOutput","fields":[{"description":"The user's name","name":"userName","type":"String"},{"name":"userId","type":"bigint!"},{"name":"calendarId","type":"bigint!"},{"name":"differentCalendarId","type":"bigint"}],"relationships":[{"name":"calendar","type":"object","remote_table":{"name":"calendar","schema":"public"},"field_mappings":[{"calendarId":"id"}]},{"name":"otherCalendar","type":"object","remote_table":{"name":"calendar","schema":"public"},"field_mappings":[{"differentCalendarId":"id"}]}]}],"scalars":[],"enums":[{"name":"CalendarType","values":[{"name":"PRIVATE"},{"name":"PUBLIC"},{"name":"SHARED"}]}]}}""",
            actions
        )
    }
}
