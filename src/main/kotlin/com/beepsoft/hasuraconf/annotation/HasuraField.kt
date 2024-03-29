package com.beepsoft.hasuraconf.annotation

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Explicit typename to use for a function value parameter or field
 */
@Target(AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraField (
    /**
     * Override field name in Graphql with tis value if the field's Java/Kotlin name is not suitable.
     */
    val name: String = "",

    /**
     * Description of field for users of the graphql schema
     */
    val description: String = "",

    /**
     * Same as name.
     */
    val value: String = "",

    /**
     * Type name of the field in case its Java/Kotlin type is not suitable.
     */
    val type: String = "",

    /**
     * Description for type if it is a scalar type. This is used in the graphql schema as comment
     */
    val typeDescription: String = "",

    /**
     * If TRUE, the field is nullable, ie. no value needs to be provided or its value van be null. If FALSE then
     * the field must be provided / will always be available in result. If UNSET then the action generator tries to
     * figure out from the kotlin reflections. If it is not available then will default to true.
     */
    val nullable: Nullable = Nullable.UNSET
)

/**
 * Relationship to another table from an action return value field.
 *
 * If the field refers to a Hasura managed entity then a `@HasuraRelationship()` is enough and HasuraActionGenerator
 * calculates the values for name, remoteTable, remoteSchema, and fieldMappings. The `name` will be the field name,
 * and the actual graphql name of the field will be the field name + "Id" appended. Ie: calendar --> calendarId. This,
 * however can be overridden using `graphqlFieldName`.
 *
 * If @HasuraRelationship is put on a field with a simple type (Long, String, etc), then the field will be used as is
 * in graphql and a `name` for the relationship maybe specified, otherwise the field's name will be used. In case of
 * a simple type `remoteTable`, `remoteSchema`, and `fieldMappings` must be explicitly specified.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraRelationship (
    /**
     * Name of relationship
     */
    val name: String = "",

    /**
     * Source name
     */
    val source: String = "default",

    /**
     * Remote table to join
     */
    val remoteTable: String = "",
    /**
     * Remote schame of remote table.
     */
    val remoteSchema: String = "public",

    /**
     * In case field references a Hasura managed entity and don't want to use the automatically generated
     * graphql field name (ie. fieldName --> fieldNameId) then it can be overriden with this value.
     */
    val graphqlFieldName: String = "",

    /**
     * Explicit graphql type to generate for this field
     */
    val graphqlFieldType: String = "",

    /**
     * Description of the fields type. Mostly useful for scalar type definitions.
     */
    val graphqlFieldTypeDescription: String = "",

    /**
     * Type of the relationship
     */
    val type: HasuraRelationshipType = HasuraRelationshipType.OBJECT,

    /**
     * Fields to use for joining
     */
    val fieldMappings: Array<HasuraFieldMapping> = []
)

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraFieldMapping (
    val fromField: String = "",
    val toField: String = ""
)

enum class HasuraRelationshipType {
    OBJECT,
    ARRAY
}

enum class Nullable {
    UNSET,
    TRUE,
    FALSE
}
