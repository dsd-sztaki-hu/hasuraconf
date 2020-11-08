package com.beepsoft.hasuraconf.model

import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
class EmbeddedList {

    /**
     * Storage for the list.
     */
    @Column(columnDefinition = "TEXT")
    private val list: String? = null

    /**
     * Number of elements in the list.
     */
    private val count = 0

    /**
     * Other (unstructured, or json, etc.) information related to the list (eg. sort order).
     */
    @Column(columnDefinition = "TEXT")
    private val aux: String? = null

    @Column(columnDefinition = "TEXT")
    private val complexFieldName: String? = null

}