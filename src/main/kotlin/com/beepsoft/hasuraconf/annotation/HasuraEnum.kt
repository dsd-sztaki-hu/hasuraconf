package com.beepsoft.hasuraconf.annotation

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy


/**
 * Marks a class that should be considered a HasuraEnum. The class itself must be a java Enum type and
 * fields using the enum should @ManyToOne map the @HasuraEnum type field.
 *
 * @HasuraEnum may be place on a Java enum but be aware that in this case Hibernate won't work properly
 * as you cannot define a no-arg constructor. If you just plan to use hasuraconf to generate the Hasura metadata
 * but won't actualy work with Hibernate then using enum is fine.
 *
 * If however yo want to work with Hubernate you should create a class with a value and comment field
 * and define constant fields annotated with @HasuraEnumValue.
 * Note: always use a @Table(name = "some_alternative_name") on the enum annotated with HasuraEnum
 * otherwise an extra create_object_relationship would be generated into hasura-init.json.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraEnum


/**
 * Must be placed on a `final static String ENUM_VALUE="Some value"` or `const val ENUM_VALUE="Some value"` field.
 */
@Target(AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraEnumValue
