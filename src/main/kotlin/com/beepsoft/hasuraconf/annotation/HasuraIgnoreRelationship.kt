package com.beepsoft.hasuraconf.annotation

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy


/**
 * Relationship fields  (ie. @OneToOne, @OneToMany, @ManyToOne, @ManyToMany) marked with @HasuraIgnoreRelationship
 * will not be tracked. This allows defining relationships in classes purely for side effects of Hibernate
 * DDL. Eg. When both ends of a ManyToMany relationship require a ON DELETE CASCADE while one end of the
 * many-to-many may not required to be tracked. In this case you can define the @ManyToMany as usual and add
 * @HasuraIgnoreRelationship to not generate hasura specific tracking for the relationship
 */
@Target(AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraIgnoreRelationship
