package com.beepsoft.hasuraconf.model

import javax.persistence.*

/**
 * Layout of a theme for a specific kind of device
 */
@Entity
@Table(indexes = arrayOf(
        Index(columnList = "createdAt"),
        Index(columnList = "updatedAt"),
        Index(columnList = "id"))
)
class Layout : BaseObject() {
    var mnemonic: String? = null
    var title: String? = null
    var description: String? = null
    @Lob
    var data: ByteArray? = null

    @ManyToOne
    var theme: Theme? = null

    var mediaQuery: String? = null
    var minWidth: Int? = null
    var maxWidth: Int? = null
    var minHeight: Int? = null
    var maxHeight: Int? = null

    var userAgentRegexp: String? = null
}
