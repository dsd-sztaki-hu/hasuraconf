package com.beepsoft.hasuraconf.model

import com.beepsoft.hasuraconf.annotation.HasuraRootFields
import javax.persistence.Entity

/**
 * For testing clashes when generating plural names. Here we have aliases to avoid the clash.
 */
@Entity
@HasuraRootFields(
        select = "bookSeriesMulti",
        update = "updateBookSeriesMulti",
        insert = "createBookSeriesMulti",
        delete = "deleteBookSeriesMulti"
)
class BookSeries : BaseObject() {
    var title: String? = null
}