package com.beepsoft.hasuraconf.model

import com.beepsoft.hasuraconf.annotation.HasuraEnum
import javax.persistence.*

/**
 * Ownership of a specific calendar. Many user may have ownership to the same calendar with different role
 * (owner, editor, viewer).
 */
@Entity
@Table(indexes = arrayOf(
        Index(columnList = "createdAt"),
        Index(columnList = "updatedAt"),
        Index(columnList = "id"))
)
class CalendarRole : BaseObject() {

    @ManyToOne(optional = false)
    var calendar: Calendar? = null

    @ManyToOne(optional = false)
    var user: CalendarUser? = null

    @ManyToOne
    var role: RoleType? = null

    @Entity
    @Table(name = "calendar_role_type")
    @HasuraEnum
    enum class RoleType(
            @Column(columnDefinition = "TEXT")
            var description: String) {
        OWNER("Main owner (creator) of the calendar with admin rights"),
        EDITOR("May edit the calendar"),
        VIEWER("May view the calendar");

        @Id
        @Column(columnDefinition = "TEXT")
        var value = toString()

    }
}
