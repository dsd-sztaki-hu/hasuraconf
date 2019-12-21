package com.beepsoft.hasuraconf.model


import javax.persistence.*

/**
 * Event to be displayed in a Day.
 */
@Entity
@Table(indexes = arrayOf(
        Index(columnList = "createdAt"),
        Index(columnList = "updatedAt"),
        Index(columnList = "id"))
)
class Event : BaseObject() {

    /** The Calendar it belongs to.  */
    @ManyToOne(optional = false)
    var calendar: Calendar? = null

    @ManyToOne(optional = false)
    var day: Day? = null

    /** If content is not locally stored, reference to the content interpretable according to the contentType.
     * Eg. an URL or embeded javascript.  */
    var reference: String? = null

    /** Type identifier of a content type. TODO: maybe mime type should be used?  */
    var contentType: Int? = null

    /** Event data stored locally.  */
    // https://cloud.google.com/java/getting-started/using-cloud-storage
    var content: ByteArray? = null

}
