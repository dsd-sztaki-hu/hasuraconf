package com.beepsoft.hasuraconf.model


import javax.persistence.*

/**
 */
@Entity
@Table(indexes = arrayOf(
        Index(columnList = "createdAt"),
        Index(columnList = "updatedAt"),
        Index(columnList = "id"))
)
class Theme : BaseObject() {
    var mnemonic: String? = null
    var title: String? = null
    var description: String? = null

    var cssClassName: String? = null
    var minDayCount: Int? = null
    var maxDayCount: Int? = null
    @OneToMany
    var themeLayouts: List<Layout>? = null

}
