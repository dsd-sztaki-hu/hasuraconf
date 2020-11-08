package com.beepsoft.hasuraconf.model

import javax.persistence.Entity

/**
 * Subclass of Day to test usage of @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
 */
@Entity
class SpecialDay : Day() {

    var isSpecialDay: Boolean? = true

}