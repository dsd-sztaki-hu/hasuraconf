package com.beepsoft.hasuraconf.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*
import javax.persistence.*

/**
 * BaseObject for any entity.
 */
@MappedSuperclass
abstract class BaseObject {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    var id: Long? = null

    var createdAt: Date? = null
    var updatedAt: Date? = null


    @Column(unique = true)
    var tag: String? = null

    @PrePersist
    fun onSave() {
        val now = Date()
        if (createdAt == null) {
            createdAt = now
        } else {
            updatedAt = now
        }
    }


    fun getSummary(lang: String?): String {
        return javaClass.simpleName + "-" + id
    }

    @JsonIgnore
    fun getSummary(): String {
        return getSummary("hu"/*RequestContext.getLanguage()*/)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BaseObject

        if (id != other.id) return false
        if (tag != other.tag) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (tag?.hashCode() ?: 0)
        return result
    }

}
