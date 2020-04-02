package com.beepsoft.hasuraconf.annotation

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Define custom aliases for field names.
 * The basic alias is for column/field names via  {@code fieldAlias}. By default we use the fields name as specified in
 * Java, but with this it can be redefined.
 *
 * Special handling of {@code @ManyToMany} annotated fields where a join table is created that is not mapped to a Java
 * class. In this case the ID's od the two ends of the join (@{code joinColumnAlias/keyColumnAlias} and
 * @{code inverseJoinColumnAlias/relatedColumnAlias}) can be redefined, as well as the object reference name
 * via {@code joinFieldAlias}. These join tables also have their own root fields, which can be customized using
 * a {@code @HasuraRootFields} annotation set in the {@code rootFieldAliases} parameter.
 */
@Target(AnnotationTarget.FIELD)
annotation class HasuraAlias (
        /**
         * Instead of using the field name as defined in Java, use this alias
         */
        val fieldAlias : String = "",

        /**
         * In case of many-to-many associations use this explicit name instead of the one generated
         * based on the in the join column / key column name (which is also settable via {@code @JoinTable(joinColumn=...)}
         */
        val joinColumnAlias : String = "",

        /**
         * Same as joinColumnName.
         */
        val keyColumnAlias : String = "",

        /**
         *   In case of many-to-many associations use this explicit name instead of the one generated
         *   based on the in the inverse join / related column column name (which is also settable via {@code @JoinTable(inverseJoinColumn=...)}
         */
        val inverseJoinColumnAlias: String = "",

        /**
         * Same as inverseJoinColumnName
         */
        val relatedColumnAlias : String = "",

        /**
         * In case of many-to-many association the object reference's name is derived from the joined table's name
         * (by converting it from snake_case to camelCase). In most cases this is satisfactory, but sometimes you want
         * to override this behaviour. `joinFieldAlias` can be used in this case
         */
        val joinFieldAlias : String = "",

        /**
         * In case of @ManyToMany join tables allowd defining custom root field aliases.
         */
        val rootFieldAliases: HasuraRootFields = HasuraRootFields()
)
