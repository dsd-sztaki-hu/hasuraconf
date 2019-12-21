package com.beepsoft.hasuraconf.model


import java.io.Serializable
import javax.persistence.*

/**
 * A user.
 */
@Entity
@Table(indexes = arrayOf(
        Index(columnList = "createdAt"),
        Index(columnList = "updatedAt"),
        Index(columnList = "id"))
)
class CalendarUser : BaseObject(), Serializable {
    @Column(unique = true)
    var firebaseUserId: String? = null
    @Column(unique = true)
    var email: String? = null
    var nickname: String? = null
    var firstName: String? = null
    var lastName: String? = null
    //private  Set<AppRole> authorities;
    @Column(columnDefinition = "boolean default true", nullable = false)
    var enabled: Boolean = true
    @Column(unique = true)
    var username: String? = null
    var password: String? = null

}
