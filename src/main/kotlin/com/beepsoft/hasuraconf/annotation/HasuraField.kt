package com.beepsoft.hasuraconf.annotation

/**
 * Explcit typename to use for a function value parameter or field
 */
@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.PROPERTY_GETTER,
)
@Retention
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
