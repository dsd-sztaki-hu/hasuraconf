package com.beepsoft.hasuraconf

import com.beepsoft.hasuraconf.annotation.HasuraRootFields
import org.hibernate.persister.collection.BasicCollectionPersister
import org.hibernate.persister.entity.AbstractEntityPersister
import org.hibernate.type.Type
import java.lang.reflect.Field
import javax.persistence.metamodel.EntityType

data class ProcessorParams(
    val entity: EntityType<*>,
    val classMetadata: AbstractEntityPersister,
    val field: Field,
    val columnName: String,
    val columnType: Type,
    val propName: String
)

data class ManyToManyEntity(
    val entity: EntityType<*>,
    val join1: BasicCollectionPersister,
    var join2: BasicCollectionPersister? = null,
    val field1: Field,
    var field2: Field? = null,
)

data class M2MData(
    val join: BasicCollectionPersister,
    val field: Field,
    val tableName: String,
    val entityName: String,
    val entityNameLower: String,
    val keyColumn: String,
    val keyColumnAlias: String,
    val keyColumnType: Type,
    val relatedColumnName: String,
    val relatedColumnNameAlias: String,
    val relatedColumnType: Type,
    var joinFieldName: String,
    val relatedTableName: String,
    val keyFieldName: String,
    val rootFields: HasuraRootFields?
)
