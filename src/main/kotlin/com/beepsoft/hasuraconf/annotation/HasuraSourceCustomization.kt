package com.beepsoft.hasuraconf.annotation

import io.hasura.metadata.v3.CustomRootFields
import io.hasura.metadata.v3.SourceTypeCustomization
import kotlinx.serialization.SerialName
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Target(AnnotationTarget.CLASS)
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraSourceCustomization(

    val rootFields: HasuraCustomRootFields = HasuraCustomRootFields(),

    val typeName: HasuraSourceTypeCustomization = HasuraSourceTypeCustomization(),

    val namingConvention: HasuraNamingConvention = HasuraNamingConvention.HASURA_DEFAULT

)

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraCustomRootFields (
    /**
     * Customise the `delete_<table-name>` root field
     */
    val delete: String = "",

    /**
     * Customise the `delete_<table-name>_by_pk` root field
     */
    val deleteByPk: String = "",

    /**
     * Customise the `insert_<table-name>` root field
     */
    val insert: String = "",

    /**
     * Customise the `insert_<table-name>_one` root field
     */
    val insertOne: String = "",

    /**
     * Customise the `<table-name>` root field
     */
    val select: String = "",

    /**
     * Customise the `<table-name>_aggregate` root field
     */
    @SerialName("select_aggregate")
    val selectAggregate: String = "",

    /**
     * Customise the `<table-name>_by_pk` root field
     */
    @SerialName("select_by_pk")
    val selectByPk: String = "",

    /**
     * Customise the `update_<table-name>` root field
     */
    val update: String = "",

    /**
     * Customise the `update_<table-name>_by_pk` root field
     */
    @SerialName("update_by_pk")
    val updateByPk: String = ""
)

@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraSourceTypeCustomization (
    val prefix: String = "",
    val suffix: String = "",
)

enum class HasuraNamingConvention {
    HASURA_DEFAULT,
    GRAPHQL_DEFAULT
}
