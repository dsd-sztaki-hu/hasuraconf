package com.beepsoft.hasuraconf.model

import javax.persistence.Entity

/**
 * For testing clashes when generating plural names. Her ethe root name will clash and HasuraConfigurator is
 * expected to add -es postfixes.
 */
@Entity
class Series : BaseObject() {
    var title: String? = null
}