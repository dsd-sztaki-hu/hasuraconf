package com.beepsoft.hasuraconf.annotation

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Definition of a computed field, the value of which is the result of running an SQL function with the
 * a record of the current table as a parameter.
 * https://hasura.io/docs/latest/graphql/core/databases/postgres/schema/computed-fields.html
 *
 * @HasuraComputedField must be placed on a transient field so that Hibernate won't generate an actual db column
 * for this field. The field is merely used to provide the name for the computed field as well to add
 * @HasuraComputedField with definition details.
 */
@Target(AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraComputedField (

    /**
     * Comment for graphql schema.
     */
    val comment: String = "",

    /**
     * Name of the argument which accepts a table row type.
     * If omitted, the first argument is considered a table argument
     */
    val tableArgument: String = "",

    /**
     * Name of the argument which accepts the Hasura session object as a JSON/JSONB value.
     * If omitted, the Hasura session object is not passed to the function
     */
    val sessionArgument: String = "",

    /**
     * The SQL function to be exposed.
     */
    val functionName: String,

    /**
     * Schema of the function to be exposed.
     */
    val functionSchema: String = "",

    /**
     * The SQL function definition. If not provided, then expected to be available in the database
     * already. If provided HasuraConfigurator.sqlFunctionDefinitions will contain at the end the
     * "run_sql" commands that can be run on Hasura to generate the functions.
     */
    val functionDefinition: String = "",
)
