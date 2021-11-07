package com.beepsoft.hasuraconf.model.actions3

import com.beepsoft.hasuraconf.annotation.HasuraAction
import com.beepsoft.hasuraconf.annotation.HasuraField
import com.beepsoft.hasuraconf.annotation.HasuraFieldMapping
import com.beepsoft.hasuraconf.annotation.HasuraRelationship
import com.beepsoft.hasuraconf.model.Calendar

// From the README.md

@HasuraAction(
    handler = "{{HANDLER_URL}}"
)
fun createUserAndCalendar(
    args: UserAndCalendarInput
): String {
    TODO()
}

data class UserAndCalendarInput(
    val userName: String,
    val name: String,
    val description: String
)

@HasuraAction(
    handler = "{{HANDLER_URL}}",
)
fun createUserAndCalendar2(
    name: String,
    description: String,
    calendarType: CalendarType
): UserAndCalendar
{
    TODO()
}

data class UserAndCalendar(
    var userName: String,

    @HasuraField(type="bigint!")
    var userId: Long,

    @HasuraRelationship
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

enum class CalendarType {
    PRIVATE,
    PUBLIC,
    SHARED
}
