package com.beepsoft.hasuraconf.model


import com.beepsoft.hasuraconf.annotation.HasuraRootFields
import javax.persistence.*

/**
 */
@Entity
@Table(indexes = arrayOf(
        Index(columnList = "createdAt"),
        Index(columnList = "updatedAt"),
        Index(columnList = "id"))
)
@HasuraRootFields(
        select = "getThemes",
        selectByPk = "getATheme",
        selectAggregate = "getThemeAgregate",
        insert = "addSomeThemes",
        insertOne = "addOneTheme",
        update = "updateSomeThemes",
        updateByPk = "updateOneTheme",
        delete = "deleteSomeThemes",
        deleteByPk = "deleteOneTheme"
)
class Theme : BaseObject() {
    var mnemonic: String? = null
    var title: String? = null
    var description: String? = null

    var cssClassName: String? = null
    var minDayCount: Int? = null
    var maxDayCount: Int? = null
    @OneToMany(mappedBy = "theme")
    var themeLayouts: List<Layout>? = null

    @ElementCollection
    private val names: List<String>? = null
}
