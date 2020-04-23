package com.beepsoft.hasuraconf.annotation


/**
 * Marks a field to be treated as read-only in the json schema. If `exceptAtCreation` is true readOnly
 * will be generated but also in the `hasura` extension will be added an `allowWriteAtCreation: true`
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FIELD)
annotation class ReadOnly (
        val exceptAtCreation : Boolean = false
)
