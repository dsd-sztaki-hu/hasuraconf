package com.beepsoft.hasuraconf.annotation

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Explicit typename to use for a Class instead of the class's Java/Kotlin name
 */
@Target(AnnotationTarget.CLASS)
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraType (
    /**
     * Override field name in Graphql with this value if the field's Java/Kotlin name is not suitable.
     */
    val name: String = "",

    /***
     * Description of the type for the graphql schema.
     */
    val description: String = "",

    /**
     * Same as name.
     */
    val value: String = "",

    /**
     * Relationships for some of the felds of the type. @HasuraRelationship maybe also defined at the @HasuraField
     * level.
     */
    val relationships: Array<HasuraRelationship> = []
)
