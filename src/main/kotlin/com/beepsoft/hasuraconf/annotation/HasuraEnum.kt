package com.beepsoft.hasuraconf.annotation


/**
 * Marks a class that should be considered a HasuraEnum. The class itself must be a java Enum type and
 * fields using the enum should @ManyToOne map the @HasuraEnum type field.
 *
 * Note: always use a @Table(name = "some_alternative_name") on the enum annotated with HasuraEnum
 * otherwise an extra create_object_relationship would be generated into hasura-init.json.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
annotation class HasuraEnum
