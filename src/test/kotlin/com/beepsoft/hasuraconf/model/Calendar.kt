package com.beepsoft.hasuraconf.model

import com.beepsoft.hasuraconf.annotation.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import javax.persistence.*

/**
 */
@Entity
@Table(indexes = arrayOf(
        Index(columnList = "createdAt"),
        Index(columnList = "updatedAt"),
        Index(columnList = "id"))
)
@HasuraPermissions(
        [
            HasuraPermission(
                    operation = HasuraOperation.INSERT,
                    role = "USER",
                    fieldPresets = HasuraFieldPresets([
                        HasuraFieldPreset(field = "localeLang", value = "en")
                    ])
            ),
            HasuraPermission(
                    operation = HasuraOperation.SELECT,
                    role = "USER",
                    json = "{roles: { user_id: { _eq: 'X-Hasura-User-Id' } }}",
                    allowAggregations = true
            ),
            HasuraPermission(
                    operation = HasuraOperation.UPDATE,
                    role = "USER",
                    excludeFields = ["tag", "localeLang", "previousVersion"],
                    jsonFile = "/permissions/update_permission_fragment.json"),
            HasuraPermission(
                    operation = HasuraOperation.DELETE,
                    role = "USER",
                    jsonFile = "/permissions/delete_permission_fragment.json")
        ]
)
class Calendar : BaseObject() {

    /** Creator/owner of the calendar  */
    @OneToMany(mappedBy = "calendar", cascade = [CascadeType.ALL])
    @OnDelete(action = OnDeleteAction.CASCADE)
    var roles: List<CalendarRole>? = null

    /** Title of the calendar  */
    var title: String? = null

    /** Short description of the calendar  */
    var description: String? = null

    /** Locale language  */
    var localeLang: String? = null

    /** Locale country  */
    var localeCountry: String? = null

    /** Boxes making up the calendar  */
    @OneToMany(mappedBy = "calendar", cascade = [CascadeType.ALL])
    @OnDelete(action = OnDeleteAction.CASCADE)
    var days = mutableListOf<Day>()

    /** Availability of the calendar.  */
    @ManyToOne
    var availability: Availability? = null

    /** Published or not.  */
    @Column(columnDefinition = "boolean default false", nullable = false)
    var published: Boolean = false

    /** Version number of calendar.  */
    var version: Int? = null

    /** Previous version.  */
    @OneToOne
    @HasuraGenerateCascadeDeleteTrigger
    var previousVersion: Calendar? = null

    /** Next version.  */
    @OneToOne
    @HasuraGenerateCascadeDeleteTrigger
    var nextVersion: Calendar? = null

    /** Theme descriptor of the calendar.  */
    @OneToOne(optional = true)
    var theme: Theme? = null

    /** Theme specific JSON config.  */
    var themeConfig: String? = null

    @ManyToMany
    // TODO: this should not be required, somehow we should collect definitions from both sides of the
    // relationship and merge those together into a single one
    @OrderColumn(name = "calendar_order", nullable = false)
    @JoinTable(name = "user_calendar", joinColumns = arrayOf(JoinColumn(name = "the_calendar_id")), inverseJoinColumns = arrayOf(JoinColumn(name = "the_user_id")))
    @OnDelete(action = OnDeleteAction.CASCADE)
    var users: List<CalendarUser>? = null

    // For testing edge case
    @ManyToMany
    // If joinColumnAlias or inverseJoinColumnAlias is used, it must be also specifed at the other end of the
    // join. See at children
    @HasuraAlias(joinColumnAlias = "theParent", inverseJoinColumnAlias = "theChild", joinFieldAlias = "parent")
    var parents: List<Calendar> = mutableListOf()

    @ManyToMany(mappedBy = "parents")
    @HasuraAlias(joinColumnAlias = "theParent", inverseJoinColumnAlias = "theChild", joinFieldAlias = "child")
    var children: List<Calendar> = mutableListOf()


    @Entity
    @Table(name = "calendar_availability")
    @HasuraEnum
    enum class Availability(
            @Column(columnDefinition = "TEXT")
            var description: String) {
        PRIVATE("Only users with explicit role have read/write access to the calendar"),
        PUBLIC("Anyone has read access to the calendar");


        @Id
        @Column(columnDefinition = "TEXT")
        var value = toString()
    }


}
