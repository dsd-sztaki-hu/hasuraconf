package com.beepsoft.hasuraconf.model


import com.beepsoft.hasuraconf.annotation.HasuraOperation
import com.beepsoft.hasuraconf.annotation.HasuraPermission
import com.beepsoft.hasuraconf.annotation.HasuraPermissions
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import javax.persistence.*

/**
 * A day (template) in a calendar
 */
@Entity
@Table(indexes = arrayOf(
        Index(columnList = "createdAt"),
        Index(columnList = "updatedAt"),
        Index(columnList = "id"))
)
@CalendarBasedPermissions
class Day : BaseObject() {

    /** The calendar it belongs to  */
    @ManyToOne(optional = false)
    var calendar: Calendar? = null

    /** Title  */
    var title: String? = null

    /** Description  */
    var description: String? = null

    // Note: made it OneToMany because of current limitations of Hasura with nested inserts for one-to-one relationships
    // https://github.com/hasura/graphql-engine/pull/2852
    /** Event displayed when opening the day.  */
    @OneToMany(mappedBy = "day", cascade = [CascadeType.ALL])
    @OnDelete(action=OnDeleteAction.CASCADE)
    var events: List<Event>? = null

    /** Order/position of the day in the series of days.  */
    var pos: Int = 0

    /** Handler class of condition. TBD  */
    var conditionClass: String? = null

    /** Parameters of condition calculation. TBD */
    var conditionParams: String? = null

    /** Theme specific configuration for the Day.  */
    var themeConfig: String? = null
}
