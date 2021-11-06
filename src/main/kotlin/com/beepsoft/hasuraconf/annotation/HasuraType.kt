package com.beepsoft.hasuraconf.annotation

/**
 * Explicit typename to use for a Class instead of the class's Java/Kotlin name
 */
@Target(AnnotationTarget.CLASS)
annotation class HasuraType (
    /**
     * Override field name in Graphql with tis value if the field's Java/Kotlin name is not suitable.
     */
    val name: String = "",
    /**
     * Same as name.
     */
    val value: String = "",

)
