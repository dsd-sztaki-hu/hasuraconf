package com.beepsoft.hasuraconf.annotation

/**
 * Explcit typename to use for a function value parameter or field
 */
@Target(AnnotationTarget.FIELD)
annotation class HasuraField (
    /**
     * Override field name in Graphql with tis value if the field's Java/Kotlin name is not suitable.
     */
    val name: String = "",
    /**
     * Same as name.
     */
    val value: String = "",

    /**
     * Type name of the field in case its Java/Kotlin type is not suitable.
     */
    val type: String = "",
)
