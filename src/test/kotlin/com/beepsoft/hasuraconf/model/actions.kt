package com.beepsoft.hasuraconf.model

import com.beepsoft.hasuraconf.annotation.HasuraAction
import com.beepsoft.hasuraconf.annotation.HasuraField

// createUserAndCalendar and createUserAndCalendar2 have same input params, but will have differenet input types
// because the params are all primitives
@HasuraAction(
    handler = "{{HANDLER_URL}}"
)
fun createUserAndCalendar(
    name: String,
    description: String
): String {
    TODO()
}

@HasuraAction(
    handler = "{{HANDLER_URL}}"
)
fun createUserAndCalendar2(
    name: String,
    description: String,
): String {
    TODO()
}

// createUserAndCalendar3 and createUserAndCalendar4 will use the same UserAndCalendarInput type but their output
// types will differ
@HasuraAction(
    handler = "{{HANDLER_URL}}"
)
fun createUserAndCalendar3(
    args: UserAndCalendarInput
): String {
    TODO()
}

class ActionTest {
    @HasuraAction(
        handler = "{{HANDLER_URL}}"
    )
    fun createUserAndCalendar4(
        args: UserAndCalendarInput,
    ): UserAndCalendarOutput
    {
        TODO()
    }
}

abstract class UserAndCalendarOutput {
    lateinit var userName: String
    @HasuraField(type = "bigint!!!!!")
    var userId: Long = 0
    var calendarId: Long = 0
}


data class UserAndCalendarInput(
    val name: String,
    val description: String
)
