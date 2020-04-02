package com.beepsoft.hasuraconf.annotation

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Custom root field aliases as defined in
 * https://hasura.io/docs/1.0/graphql/manual/api-reference/schema-metadata-api/table-view.html
 */
@Target(AnnotationTarget.CLASS)
annotation class HasuraRootFields (
        /**
         * Instead of a name derived from the table name by turning the snake case name to CamelCase use this
         * name as the base name for generating root fields. The name should be a CamelCaseName.
         */
        val baseName: String = "",

        val select : String = "",
        val selectByPk : String = "",
        val selectAggregate : String = "",
        val insert : String = "",
        val insertOne : String = "",
        val update : String = "",
        val updateByPk : String = "",
        val delete : String = "",
        val deleteByPk : String = ""
)
