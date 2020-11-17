package com.beepsoft.hasuraconf

import com.beepsoft.hasuraconf.annotation.*
import com.google.common.net.HttpHeaders
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import net.pearx.kasechange.CaseFormat
import net.pearx.kasechange.toCamelCase
import net.pearx.kasechange.toCase
import org.apache.commons.text.CaseUtils
import org.atteo.evo.inflector.English
import org.hibernate.SessionFactory
import org.hibernate.internal.SessionFactoryImpl
import org.hibernate.metamodel.spi.MetamodelImplementor
import org.hibernate.persister.collection.BasicCollectionPersister
import org.hibernate.persister.entity.AbstractEntityPersister
import org.hibernate.type.*
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.io.PrintWriter
import java.lang.reflect.Field
import javax.persistence.EntityManagerFactory
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.metamodel.EntityType
import kotlin.Comparator

/**
 * Creates JSON to initialize Hasura. The JSON:
 *
 *  * adds tracking to all Hibernate mapped tables
 *  * adds custom field and relationship names to match names used in the Java Entity classes (Hasura
 * would you by default the Postgresql column names)
 *
 *
 *
 * We only generate relationships that are actually defined in the entities. Hasura, normally, would create, eg.
 * inverse relationships as well, but we only generate relationships hat are actually defned in code.
 * This is the reason why you would see a number of "Untracked foreign-key relations" suggested by Hasura after init.json
 * has been applied to it.
 *
 *
 *
 * init.json will contains following kind of API calls:
 *
 * https://docs.hasura.io/1.0/graphql/manual/api-reference/schema-metadata-api/table-view.html#set-table-custom-fields
 *
 * Track table:
 *
 * <pre>
 * {
 * "type": "add_existing_table_or_view",
 * "args": {
 * "name": "operation",
 * "schema": "public"
 * }
 * }
 *  </pre>
 *
 *
 * Custom fields for a tracked table:
 *
 * <pre>
 * {
 * "type": "set_table_custom_fields",
 * "version": 2,
 * "args": {
 * "table": "operation",
 * "custom_root_fields": {
 * "select": "Operations",
 * "select_by_pk": "Operation",
 * "select_aggregate": "OperationAggregate",
 * "insert": "AddOperations",
 * "update": "UpdateOperations",
 * "delete": "DeleteOperations"
 * },
 * "custom_column_names": {
 * "id": "operationid"
 * }
 * }
 * }
</pre> *
 *
 * Object relationship with mapping done on the same table:
 *
 * <pre>
 * {
 * "type": "create_object_relationship",
 * "args": {
 * "name": "task",
 * "table": {
 * "name": "operation",
 * "schema": "public"
 * },
 * "using": {
 * "foreign_key_constraint_on": "task_id"
 * }
 * }
 * }
</pre> *
 *
 * Object relationship with other side doing the mapping (ie. with mappedBy ...):
 *
 * <pre>
</pre> *
 *
 *
 *
 * Array relationship:
 * <pre>
</pre> *
 */
class HasuraConfigurator(
        var entityManagerFactory: EntityManagerFactory,
        var confFile: String?,
        var schemaName: String,
        var loadConf: Boolean,
        var hasuraEndpoint: String,
        var hasuraAdminSecret: String?,
        var schemaFile: String?,
        var schemaVersion: String,
        var customPropsFieldName: String,
        var ignoreJsonSchema: Boolean = false,
        var rootFieldNameProvider: RootFieldNameProvider = DefaultRootFieldNameProvider()
) {

    companion object {
        // @Suppress("JAVA_CLASS_ON_COMPANION")
        // @JvmStatic
        private val LOG = getLogger(this::class.java.enclosingClass)
    }

    inner class CascadeDeleteFields(var table: String, var field: String, var joinedTable: String)

    var confJson: String? = null
        private set // the setter is private and has the default implementation

    var jsonSchema: String? = null
        private set // the setter is private and has the default implementation

    var metadataJsonObject: JsonObject = JsonObject(mutableMapOf())
        private set // the setter is private and has the default implementation

    var metadataJson: String? = null
        private set // the setter is private and has the default implementation

    private var sessionFactoryImpl: SessionFactory
    private var metaModel: MetamodelImplementor
    private var permissionAnnotationProcessor: PermissionAnnotationProcessor

    private lateinit var tableNames: MutableSet<String>
    private lateinit var entityClasses: MutableSet<Class<out Any>>
    private lateinit var enumTables: MutableSet<String>
    private lateinit var cascadeDeleteFields: MutableSet<CascadeDeleteFields>
    private lateinit var jsonSchemaGenerator: HasuraJsonSchemaGenerator
    private lateinit var generatedRelationships:  MutableSet<String>
    private lateinit var generatedCustomFieldNamesForManyToManyJoinTables:  MutableSet<String>

    private lateinit var manyToManyEntities: MutableSet<ManyToManyEntity>

    // Acrtual postgresql types for some SQL types. Hibernate uses the key fields when generating
    // tables, however Postgresql uses the "values" of postgresqlNames and so does Hasura when
    // generating graphql schema based on the DB schema.
    // https://hasura.io/docs/1.0/graphql/manual/api-reference/postgresql-types.html
    // TODO: should check these some more ...
    private val postgresqlNames = mapOf(
            "int2" to "Int",
            "int4" to "Int",
            "int8" to "bigint",
            "int" to "Int",
            "serial2" to "Int",
            "serial4" to "Int",
            "serial8" to "bigserial",
            "bool" to "Bool",
            "date" to "Date",
            "float4" to "Float",
            "text" to "text",
            "varchar(\$l)" to "String",
            "time" to "time",
            "timetz" to "time",
            "timestamp" to "timestamp",
            "timestamptz" to "timestamp",
            "uuid" to "uuid"
    );

    init {
        sessionFactoryImpl = entityManagerFactory.unwrap(SessionFactoryImpl::class.java)
        metaModel = sessionFactoryImpl.metamodel as MetamodelImplementor
        permissionAnnotationProcessor = PermissionAnnotationProcessor(entityManagerFactory)
    }
//    @Autowired
//    fun setEntityManagerFactory(entityManagerFactory: EntityManagerFactory) {
//        this.entityManagerFactory = entityManagerFactory
//        sessionFactoryImpl = entityManagerFactory.unwrap(SessionFactory::class.java) as SessionFactoryImpl
//        metaModel = sessionFactoryImpl.metamodel as MetamodelImplementor
//        permissionAnnotationProcessor = PermissionAnnotationProcessor(entityManagerFactory)
//    }

    @Throws(HasuraConfiguratorException::class)
    fun configureNew()
    {
        jsonSchemaGenerator = HasuraJsonSchemaGenerator(schemaVersion, customPropsFieldName)
        manyToManyEntities = mutableSetOf()
        generatedCustomFieldNamesForManyToManyJoinTables = mutableSetOf()

        // Get metaModel.entities sorted by name. We do this sorting to make result more predictable (eg. for testing)
        val entities = sortedSetOf(
                Comparator { o1, o2 ->  o1.name.compareTo(o2.name)},
                *metaModel.entities.toTypedArray() )

        val tables = buildJsonArray {
            for (entity in entities) {
                // Ignore subclasses of classes with @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
                // In this case all subclasse's fields will be stored in the parent's table and therefore subclasses
                // cannot have root operations.
                if (entity.parentHasSingleTableInheritance()) {
                    continue
                }

                add(configureEntity(entity, entities))
            }
            for (m2m in manyToManyEntities) {
                configureManyToManyEntity(m2m)?.let {
                    add(it)
                }
            }
        }

        metadataJsonObject = buildJsonObject {
            put("version", 2)
            put("tables", tables)
        }
        metadataJson = Json.encodeToString(metadataJsonObject).reformatJson()
    }

    private fun configureEntity(entity: EntityType<*>, entities: Set<EntityType<*>>) : JsonObject
    {
        val relatedEntities = entity.relatedEntities(entities)
        val targetEntityClassMetadata = metaModel.entityPersister(entity.javaType.typeName) as AbstractEntityPersister
        val tableName = targetEntityClassMetadata.tableName
        val keyKolumnName = targetEntityClassMetadata.keyColumnNames[0]

        // Add ID field
        val f = Utils.findDeclaredFieldUsingReflection(entity.javaType, targetEntityClassMetadata.identifierPropertyName)
        jsonSchemaGenerator.addSpecValue(f!!,
                HasuraSpecPropValues(graphqlType = graphqlTypeFor(targetEntityClassMetadata.identifierType, targetEntityClassMetadata)))

        var entityName = entity.name
        // Remove inner $ from the name of inner classes
        entityName = entityName.replace("\\$".toRegex(), "")
        var entityNameLower = entityName.toString()
        entityNameLower = Character.toLowerCase(entityNameLower[0]).toString() + entityNameLower.substring(1)

        // Get the HasuraRootFields and may reset entityName
        var rootFields = entity.javaType.getAnnotation(HasuraRootFields::class.java)
        if (rootFields != null && rootFields.baseName.isNotBlank()) {
            entityName = rootFields.baseName
        }

        val rootFieldNames = generateRootFieldNames(rootFields, entityName, entityNameLower, tableName)

        jsonSchemaGenerator.addSpecValue(entity.javaType,
                HasuraSpecTypeValues(
                        graphqlType=tableName,
                        idProp=keyKolumnName,
                        rootFieldNames = rootFieldNames))



        val tableJson = buildJsonObject {
            put("table", buildJsonObject {
                put("schema", schemaName)
                put("name", tableName)
            })
            put("configuration", buildJsonObject{
                put("custom_root_fields", configureRootFieldNames(rootFieldNames))
                put("custom_column_names", configureCustomColumnNames(relatedEntities))
            })
            val objRel = configureObjectRelationships(relatedEntities)
            if (!objRel.isEmpty()) {
                put("object_relationships", objRel)
            }
            val arrRel = configureArrayRelationships(relatedEntities)
            if (!arrRel.isEmpty()) {
                put("array_relationships", arrRel)
            }

            val entityClass = entity.javaType
            if (Enum::class.java.isAssignableFrom(entityClass) && entityClass.isAnnotationPresent(HasuraEnum::class.java)) {
                put("is_enum", true)
            }

            configurePermissions(entity, this)
        }

        return tableJson
    }

    private fun configurePermissions(entity: EntityType<*>, builder: JsonObjectBuilder)
    {
        builder.apply {
            val permissions = permissionAnnotationProcessor.process(entity)

            val inserts = permissions.filter { it.operation==HasuraOperation.INSERT }.map { permissionData ->
                permissionData.toJsonObject()
            }
            if (!inserts.isEmpty()) {
                put("insert_permissions", JsonArray(inserts))
            }

            val selects = permissions.filter { it.operation==HasuraOperation.SELECT }.map { permissionData ->
                permissionData.toJsonObject()
            }
            if (!selects.isEmpty()) {
                put("select_permissions", JsonArray(selects))
            }

            val updates = permissions.filter { it.operation==HasuraOperation.UPDATE }.map { permissionData ->
                permissionData.toJsonObject()
            }
            if (!updates.isEmpty()) {
                put("update_permissions", JsonArray(updates))
            }

            val deletes = permissions.filter { it.operation==HasuraOperation.DELETE }.map { permissionData ->
                permissionData.toJsonObject()
            }
            if (!deletes.isEmpty()) {
                put("delete_permissions", JsonArray(deletes))
            }
        }
    }

    private fun configureRootFieldNames(rootFieldNames: RootFieldNames) : JsonObject
    {
        return buildJsonObject {
            put("select",           "${rootFieldNames.select}")
            put("select_by_pk",     "${rootFieldNames.selectByPk}")
            put("select_aggregate", "${rootFieldNames.selectAggregate}")
            put("insert",           "${rootFieldNames.insert}")
            put("insert_one",       "${rootFieldNames.insertOne}")
            put("update",           "${rootFieldNames.update}")
            put("update_by_pk",     "${rootFieldNames.updateByPk}")
            put("delete",           "${rootFieldNames.delete}")
            put("delete_by_pk",     "${rootFieldNames.deleteByPk}")
        }
    }

    private fun configureManyToManyEntity(m2m: ManyToManyEntity) : JsonObject?
    {
        val tableName = m2m.join.tableName
        var entityName = tableName.toCase(CaseFormat.CAPITALIZED_CAMEL);
        var keyColumn = m2m.join.keyColumnNames[0]
        var keyColumnType = m2m.join.keyType
        var keyColumnAlias = keyColumn.toCamelCase();
        var relatedColumnName = m2m.join.elementColumnNames[0]
        var relatedColumnType = m2m.join.elementType
        var relatedColumnNameAlias = relatedColumnName.toCamelCase()

        // Make sure we don't generate custom field name customization more than once for any table.
        // This can happen in case of many-to-many join tables when generating for inverse join as well.
        if (generatedCustomFieldNamesForManyToManyJoinTables.contains(tableName)) {
            return null
        }
        generatedCustomFieldNamesForManyToManyJoinTables.add(tableName)

        // Get the HasuraAlias and may reset entityName
        var alias = m2m.field.getAnnotation(HasuraAlias::class.java);
        var rootFields = if(alias != null) alias.rootFieldAliases else null
        if (rootFields != null) {
            if (rootFields.baseName.isNotBlank()) {
                entityName = rootFields.baseName
            }
        }
        if (alias != null) {
            if (alias.keyColumnAlias.isNotBlank()) {
                keyColumnAlias = alias.keyColumnAlias
            }
            else if (alias.joinColumnAlias.isNotBlank()) {
                keyColumnAlias = alias.joinColumnAlias
            }

            if (alias.relatedColumnAlias.isNotBlank()) {
                relatedColumnNameAlias = alias.relatedColumnAlias
            }
            else if (alias.inverseJoinColumnAlias.isNotBlank()) {
                relatedColumnNameAlias = alias.inverseJoinColumnAlias
            }
        }

        // Remove inner $ from the name of inner classes
        entityName = entityName.replace("\\$".toRegex(), "")
        // Copy
        var entityNameLower = entityName.toString()
        entityNameLower = Character.toLowerCase(entityNameLower[0]).toString() + entityNameLower.substring(1)

        // arrayRel only allows accessing the join table ID fields. Now add navigation to the
        // related entity
        val relatedTableName = (m2m.join.elementType as ManyToOneType).getAssociatedJoinable(sessionFactoryImpl as SessionFactoryImpl?).tableName;
        var joinFieldName = relatedTableName.toCamelCase();
        if (alias != null && alias.joinFieldAlias.isNotBlank()) {
            joinFieldName = alias.joinFieldAlias;
        }

        val classMetadata = metaModel.entityPersister(m2m.entity.javaType.typeName) as AbstractEntityPersister
        val keyTableName = classMetadata.tableName
        val keyFieldName = keyTableName.toCamelCase()

        val rootFieldNames = generateRootFieldNames(rootFields, entityName, entityNameLower, tableName)

        val tableJson = buildJsonObject {
            put("table", buildJsonObject {
                put("schema", schemaName)
                put("name", tableName)
            })
            put("configuration", buildJsonObject{
                put("custom_root_fields", configureRootFieldNames(rootFieldNames))
                putJsonObject("custom_column_names") {
                    put(keyColumn, keyColumnAlias)
                    put(relatedColumnName, relatedColumnNameAlias)
                    // Add index columns names, ie. @OrderColumns
                    m2m.join.indexColumnNames?.forEach {
                        put(it, it.toCamelCase())
                    }
                }
            })
            putJsonArray("object_relationships") {
                addJsonObject {
                    put("name",  joinFieldName)
                    putJsonObject("using") {
                        put("foreign_key_constraint_on", relatedColumnName)
                    }
                }
            }
        }

        with(m2m) {
            jsonSchemaGenerator.addSpecValue(field,
                    HasuraSpecPropValues(
                            relation = "many-to-many",
                            type = tableName.toCase(CaseFormat.CAPITALIZED_CAMEL),
                            reference = relatedColumnNameAlias,
                            parentReference = keyColumnAlias,
                            item = joinFieldName)
            )

            jsonSchemaGenerator.addJoinType(JoinType(
                    name = tableName.toCase(CaseFormat.CAPITALIZED_CAMEL),
                    tableName = tableName,
                    fromIdName = keyColumnAlias,
                    fromIdType = jsonSchemaTypeFor(join.keyType, classMetadata),
                    fromIdGraphqlType = graphqlTypeFor(join.keyType, classMetadata),
                    fromAccessor = keyFieldName,
                    fromAccessorType = entity.name,
                    toIdName = relatedColumnNameAlias,
                    toIdType = jsonSchemaTypeFor(relatedColumnType, classMetadata),
                    toIdGraphqlType = graphqlTypeFor(relatedColumnType, classMetadata),
                    toAccessor = joinFieldName,
                    toAccessorType = relatedTableName.toCase(CaseFormat.CAPITALIZED_CAMEL),
                    orderField = join.indexColumnNames?.let {
                        it[0].toCamelCase()
                    },
                    orderFieldType = join.indexColumnNames?.let {
                        jsonSchemaTypeFor(join.indexType, classMetadata)
                    },
                    orderFieldGraphqlType = join.indexColumnNames?.let {
                        graphqlTypeFor(join.indexType, classMetadata)
                    },
                    rootFieldNames = rootFieldNames
            ))
        }

        return tableJson
    }

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
            val join: BasicCollectionPersister,
            val field: Field,
    )


    private fun processProperties(relatedEntities: List<EntityType<*>>, processor: (params: ProcessorParams) -> Unit)
    {
        relatedEntities.forEach { anEntity ->
            val classMetadata = metaModel.entityPersister(anEntity.javaType.typeName) as AbstractEntityPersister
            val propNames = classMetadata.propertyNames
            for (propName in propNames) {
                // Handle @HasuraIgnoreRelationship annotation on field
                val f = Utils.findDeclaredFieldUsingReflection(anEntity.javaType, propName)
                if (f!!.isAnnotationPresent(HasuraIgnoreRelationship::class.java)) {
                    continue
                }

                var finalPropName = propName

                // Only look for simple (non component types)
                val type = classMetadata.toType(propName)
                if (type is ComponentType) {
                    val componentType = type as ComponentType
                    val tup = (type as ComponentType).componentTuplizer
                    val propNames = classMetadata.getPropertyColumnNames(propName)
                    var result = false
                    propNames.forEachIndexed { index, columnName ->
                        val field = tup.getGetter(index).getMember()
                        if (field is Field) {
                            // Reset propName to the embedded field name
                            finalPropName = (field as Field).name
                            // Now we may alias field name according to @HasuraAlias annotation
                            var hasuraAlias = f.getAnnotation(HasuraAlias::class.java)
                            if (hasuraAlias != null && hasuraAlias.fieldAlias.isNotBlank()) {
                                finalPropName = hasuraAlias.fieldAlias
                            }

                            val columnType = componentType.subtypes[index]
                            //println("$columnName --> ${field.name}")
                            processor(ProcessorParams(anEntity, classMetadata, field, columnName, columnType, finalPropName))
                        }
                        else {
                            throw HasuraConfiguratorException("Unable to handle embeded class ${anEntity} and getter ${field} ")
                        }
                    }
                }
                else {
                    // Now we may alias field name according to @HasuraAlias annotation
                    var hasuraAlias = f.getAnnotation(com.beepsoft.hasuraconf.annotation.HasuraAlias::class.java)
                    if (hasuraAlias != null && hasuraAlias.fieldAlias.isNotBlank()) {
                        finalPropName = hasuraAlias.fieldAlias
                    }

                    val columnName = classMetadata.getPropertyColumnNames(propName)[0]
                    val columnType = classMetadata.getPropertyType(propName)
                    processor(ProcessorParams(anEntity, classMetadata, f, columnName, columnType, finalPropName))
                }
            }
        }

    }

    private fun configureCustomColumnNames(relatedEntities: List<EntityType<*>>): JsonElement
    {
        val customColumnNames = buildJsonObject {
            processProperties(relatedEntities) { params ->
                if (!params.columnType.isAssociationType && params.propName != params.columnName) {
                    put(params.columnName, params.propName)
                }
                // Special case: for many-to-one or one-to-one, generate alias for the ID fields as well
                if (params.columnType.isAssociationType && !params.columnType.isCollectionType) {
                    val assocType = params.columnType as AssociationType
                    val fkDir = assocType.foreignKeyDirection
                    if (fkDir === ForeignKeyDirection.FROM_PARENT) {
                        // Also add customization for the ID field name
                        val camelCasedIdName = CaseUtils.toCamelCase(params.columnName, false, '_')
                        put(params.columnName, camelCasedIdName)
                    }
                }

            }
        }
        return customColumnNames
    }

    private fun configureObjectRelationships(relatedEntities: List<EntityType<*>>): JsonArray
    {
        val objectRelationships = buildJsonArray {
            processProperties(relatedEntities) { params ->
                if (params.columnType.isAssociationType && !params.columnType.isCollectionType) {
                    val assocType = params.columnType as AssociationType
                    val fkDir = assocType.foreignKeyDirection
                    if (fkDir === ForeignKeyDirection.FROM_PARENT) {
                        add(buildJsonObject {
                            put("name", params.propName)
                            putJsonObject("using") {
                                put("foreign_key_constraint_on", params.columnName)
                            }
                        })
                        val camelCasedIdName = CaseUtils.toCamelCase(params.columnName, false, '_')
                        jsonSchemaGenerator.addSpecValue(params.entity.javaType,
                                HasuraSpecTypeValues(
                                        referenceProp = HasuraReferenceProp(name=camelCasedIdName, type=jsonSchemaTypeFor(params.columnType, params.classMetadata)),
                                        rootFieldNames = EmptyRootFieldNames
                                ))

                        if (assocType is ManyToOneType && !assocType.isLogicalOneToOne) {
                            jsonSchemaGenerator.addSpecValue(params.field,
                                    HasuraSpecPropValues(relation = "many-to-one",
                                            reference = camelCasedIdName,
                                            referenceType = jsonSchemaTypeFor(params.columnType, params.classMetadata)
                                    )
                            )
                        }
                        else {
                            jsonSchemaGenerator.addSpecValue(params.field,
                                    HasuraSpecPropValues(relation = "one-to-one",
                                            reference = camelCasedIdName,
                                            referenceType = jsonSchemaTypeFor(params.columnType, params.classMetadata)
                                    )
                            )
                        }
                    }
                    else { // TO_PARENT, ie. assocition is mapped by the other side
                        val join = assocType.getAssociatedJoinable(sessionFactoryImpl as SessionFactoryImpl?)
                        val keyColumn = join.keyColumnNames[0]
                        val mappedBy = assocType.rhsUniqueKeyPropertyName
                        // In reality mappedBy should always be set. It is either the implicit name or a specific
                        // mappedBy=<foreign property name> and so it cannot be null. I'm not sure about it so I
                        // leave here a fallback of the default Hiberante tableName_keyColumn foreign key.
                        val mappedId = if (mappedBy != null) (join as AbstractEntityPersister).getPropertyColumnNames(mappedBy)[0]
                                        else "${params.classMetadata.tableName}_${keyColumn}"
                        add(buildJsonObject {
                            put("name", params.propName)
                            putJsonObject("using") {
                                putJsonObject("manual_configuration") {
                                    putJsonObject("remote_table") {
                                        put("name", join.tableName)
                                        put("schema", schemaName)
                                    }
                                    putJsonObject("column_mapping") {
                                        put(keyColumn, mappedId)
                                    }
                                }
                            }
                        })

                        val oneToOne = params.field.getAnnotation(OneToOne::class.java)
                        oneToOne?.let {
                            jsonSchemaGenerator.addSpecValue(params.field,
                                    HasuraSpecPropValues(relation="one-to-one", mappedBy=oneToOne.mappedBy))

                        }

                    }
                }
            }
        }
        return objectRelationships
    }

    private fun configureArrayRelationships(relatedEntities: List<EntityType<*>>): JsonArray
    {
        val arrayRelationships = buildJsonArray {
            processProperties(relatedEntities) { params ->
                if (params.columnType.isCollectionType) {
                    val collType = params.columnType as CollectionType
                    val join = collType.getAssociatedJoinable(sessionFactoryImpl as SessionFactoryImpl?)
                    val keyColumn = join.keyColumnNames[0]
                    //tableNames.add(join.tableName)

                    add(buildJsonObject {
                        put("name", params.propName)
                        putJsonObject("using") {
                            putJsonObject("foreign_key_constraint_on") {
                                put("column", keyColumn)
                                putJsonObject("table") {
                                    put("name", join.tableName)
                                    put("schema", schemaName)
                                }
                            }
                        }
                    })

                    // BasicCollectionPersister - despite the name - is for many-to-many associations
                    if (join is BasicCollectionPersister) {
                        if (join.isManyToMany) {
                            // Collect many-to-many join tables and process them later
                            manyToManyEntities.add(ManyToManyEntity(params.entity, join, params.field))
                        }
                    }
                    if (params.field.isAnnotationPresent(OneToMany::class.java)) {
                        val oneToMany = params.field.getAnnotation(OneToMany::class.java)
                        val parentRef = CaseUtils.toCamelCase(keyColumn, false, '_')
                        jsonSchemaGenerator.addSpecValue(params.field,
                                HasuraSpecPropValues(
                                        relation="one-to-many",
                                        mappedBy=oneToMany.mappedBy,
                                        parentReference=parentRef))

                    }
                }
            }
        }
        return arrayRelationships
    }

    /**
     * Creates hasura-conf.json containing table tracking and field/relationship name customizations
     * at bean creation time automatically.
     */
    @Throws(HasuraConfiguratorException::class)
    fun configure() {
        confJson = null
        jsonSchema = null
        tableNames = mutableSetOf<String>()
        entityClasses = mutableSetOf<Class<out Any>>()
        enumTables = mutableSetOf<String>()
        cascadeDeleteFields = mutableSetOf<CascadeDeleteFields>()
        jsonSchemaGenerator = HasuraJsonSchemaGenerator(schemaVersion, customPropsFieldName)
        generatedRelationships = mutableSetOf<String>()
        generatedCustomFieldNamesForManyToManyJoinTables = mutableSetOf<String>()


        // Get metaModel.entities sorted by name. We do this sorting to make result more predictable (eg. for testing)
        val entities = sortedSetOf(
                Comparator { o1, o2 ->  o1.name.compareTo(o2.name)},
                *metaModel.entities.toTypedArray() )
        // Add custom field and relationship names for each table
        val customColumNamesRelationships = StringBuilder()
        val permissions = StringBuilder()
        var added = false
        for (entity in entities) {
            // Ignore subclasses of classes with @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
            // In this case all subclasse's fields will be stored in the parent's table and therefore subclasses
            // cannot have root operations.
            if (entity.parentHasSingleTableInheritance()) {
                continue
            }
            val customFieldNameJSONBuilder = StringBuilder()
            if (added) {
                customColumNamesRelationships.append(",\n")
            }
            val custom = generateEntityCustomization(entity, entities) ?: continue
            customColumNamesRelationships.append(custom)
            customColumNamesRelationships.append(customFieldNameJSONBuilder)
            added = true
            // If it is an enum class that is mapped with a ManyToOne, then we consider it a Hasura enum table case
            // and will generate set_table_is_enum
            val entityClass = entity.javaType
            if (Enum::class.java.isAssignableFrom(entityClass) && entityClass.isAnnotationPresent(HasuraEnum::class.java)) {
                val classMetadata = metaModel.entityPersister(entity.javaType.typeName) as AbstractEntityPersister
                val tableName = classMetadata.tableName
                enumTables.add(tableName)
            }
            collectCascadeDeleteCandidates(entity)
            val perms = generatePermissions(entity)
            if (perms.length != 0 && permissions.length != 0) {
                permissions.append(",")
            }
            permissions.append(perms)
        }
        // Add tracking for all tables collected, and make these the first calls in the bulk JSON
        // Note: we have to do this in the end since we had to collect all entities and join tables
        val bulk = StringBuilder()
        for (tableName in tableNames) {
            if (bulk.length == 0) {
                bulk.append(
                        """
                        {
                            "type": "bulk",
                            "args": [
                            {"type":"clear_metadata","args":{}},                            
                        """
                )
            } else {
                bulk.append(",\n")
            }
            val trackTableJSON =
                    """
						{
                    		"type": "track_table",
                    		"args": {
                    			"schema": "${schemaName}",
                    			"name": "${tableName}"
                    		}
                    	}                        
                    """
            bulk.append(trackTableJSON)
            // Handle enum table
            if (enumTables.contains(tableName)) {
                val tabelIsEnumJSON =
                        """
							,
							{
                        		"type": "set_table_is_enum",
                        		"args": {
                        			"table": {
                        				"schema": "${schemaName}",
                        				"name": "${tableName}"
                         			},
                        			"is_enum": true
                        		}
                        	}                            
                        """
                bulk.append(tabelIsEnumJSON)
            }
        }
        if (customColumNamesRelationships.length != 0) {
            bulk.append(",\n")
            bulk.append(customColumNamesRelationships)
        }
        if (permissions.length != 0) {
            bulk.append(",\n")
            bulk.append(permissions)
        }
        val triggers = generateCascadeDeleteTriggers()
        if (triggers.length != 0) {
            bulk.append(",\n")
            bulk.append(triggers)
        }
        bulk.append("\n\t]\n")
        bulk.append("}\n")

        if (!ignoreJsonSchema) {
            jsonSchema = jsonSchemaGenerator.generateSchema(*entityClasses.toTypedArray()).toString().reformatJson()
            schemaFile?.let {
                PrintWriter(it).use { out -> out.println(jsonSchema) }
            }
        }

        confJson = bulk.toString().reformatJson()
        confFile?.let {
            PrintWriter(it).use { out -> out.println(confJson) }
            if (loadConf) {
                loadConfScriptIntoHasura()
            }

        }

    }

    private fun collectCascadeDeleteCandidates(entity: EntityType<*>) {
        val entityClass = entity.javaType
        val classMetadata = metaModel.entityPersister(entity.javaType.typeName) as AbstractEntityPersister
        val propertyNames = classMetadata.propertyNames
        for (propertyName in propertyNames) {
            val f = Utils.findDeclaredFieldUsingReflection(entityClass, propertyName)
            if (f!!.isAnnotationPresent(HasuraGenerateCascadeDeleteTrigger::class.java)) {
                val fieldMetadata = metaModel.entityPersister(f.type.typeName) as AbstractEntityPersister
                val cdf = CascadeDeleteFields(classMetadata.tableName, classMetadata.getPropertyColumnNames(propertyName)[0], fieldMetadata.tableName)
                cascadeDeleteFields.add(cdf)
            }
        }
    }

    private fun generateCascadeDeleteTriggers(): String {
        val sb = StringBuilder()
        for (cdf in cascadeDeleteFields) {
            val template =
                    """
                        DROP TRIGGER IF EXISTS ${cdf.table}_${cdf.field}_cascade_delete_trigger ON ${cdf.table};;
                        DROP FUNCTION  IF EXISTS ${cdf.table}_${cdf.field}_cascade_delete();
                        CREATE FUNCTION ${cdf.table}_${cdf.field}_cascade_delete() RETURNS trigger AS
                        ${'$'}body${'$'}
                        BEGIN
                            IF TG_WHEN <> 'AFTER' OR TG_OP <> 'DELETE' THEN
                                RAISE EXCEPTION '${cdf.table}_${cdf.field}_cascade_delete may only run as a AFTER DELETE trigger';
                            END IF;
                        
                            DELETE FROM ${cdf.joinedTable} where id=OLD.${cdf.field};
                            RETURN OLD;
                        END;
                        ${'$'}body${'$'}
                        LANGUAGE plpgsql;;
                        CREATE TRIGGER ${cdf.table}_${cdf.field}_cascade_delete_trigger AFTER DELETE ON ${cdf.table}
                            FOR EACH ROW EXECUTE PROCEDURE ${cdf.table}_${cdf.field}_cascade_delete();;                       
                    """.trimIndent()
            val trigger = template.replace("\n", " ")
            if (sb.length != 0) {
                sb.append(",\n")
            }
            sb.append(
                """
                    {
                        "type": "run_sql",
                        "args": {
                            "sql": "${trigger}"
                        }
                    } 
                """
            )
        }
        return sb.toString()
    }

    private fun generatePermissions(entity: EntityType<*>): String
    {
        val permissions = permissionAnnotationProcessor.process(entity)

        val permissionJSONBuilder = StringBuilder()
        permissions.forEachIndexed { index, permissionData ->
            if (index > 0) {
                permissionJSONBuilder.append(",")
            }
            permissionJSONBuilder.append(permissionData.toHasuraJson(schemaName))
        }
        return permissionJSONBuilder.toString()
    }

    /**
     * Generates customization for a given `entity`.
     * @param entity
     * @return JSON to initialize the entity in Hasura
     */
    private fun generateEntityCustomization(entity: EntityType<*>, entities: Set<EntityType<*>>): String? {
        // entity and all related entities.
        val relatedEntities = entity.relatedEntities(entities)
        val targetEntityClassMetadata = metaModel.entityPersister(entity.javaType.typeName) as AbstractEntityPersister
        val tableName = targetEntityClassMetadata.tableName
        val keyKolumnName = targetEntityClassMetadata.keyColumnNames[0]
        tableNames.add(tableName)
        entityClasses.add(entity.javaType)

        // Add ID field
        val f = Utils.findDeclaredFieldUsingReflection(entity.javaType, targetEntityClassMetadata.identifierPropertyName)
        jsonSchemaGenerator.addSpecValue(f!!,
                HasuraSpecPropValues(graphqlType = graphqlTypeFor(targetEntityClassMetadata.identifierType, targetEntityClassMetadata)))

        var entityName = entity.name

        // Get the HasuraRootFields and may reset entityName
        var rootFields = entity.javaType.getAnnotation(HasuraRootFields::class.java)
        if (rootFields != null && rootFields.baseName.isNotBlank()) {
            entityName = rootFields.baseName
        }

        val customRootFieldColumnNameJSONBuilder = StringBuilder()
        // Remove inner $ from the name of inner classes
        entityName = entityName.replace("\\$".toRegex(), "")
        // Copy
        var entityNameLower = entityName.toString()
        entityNameLower = Character.toLowerCase(entityNameLower[0]).toString() + entityNameLower.substring(1)
        val rootFieldNames = generateRootFieldNames(rootFields, entityName, entityNameLower, tableName)

        jsonSchemaGenerator.addSpecValue(entity.javaType,
                HasuraSpecTypeValues(
                        graphqlType=tableName,
                        idProp=keyKolumnName,
                        rootFieldNames = rootFieldNames))

        customRootFieldColumnNameJSONBuilder.append(
                """
                    ${generateSetTableCustomFields(tableName, rootFieldNames)}
                    "custom_column_names": {                 
                """
        )
        val customRelationshipNameJSONBuilder = StringBuilder()
        // Iterate over all entities that need to be handled together with this entity and add property customization
        // for each related entity as well. We have more than one elem in relatedEntities if entity has
        // @Inheritance(strategy = InheritanceType.SINGLE_TABLE) and so we need to handle all subclass entities in the
        // context of this entity
        var propAdded = false
        relatedEntities.forEach {anEntity ->
            val entityClassMetadata = metaModel.entityPersister(anEntity.javaType.typeName) as AbstractEntityPersister
            val propNames = entityClassMetadata.propertyNames
            for (propName in propNames) {
                val added = addCustomFieldNameOrRef(
                        anEntity,
                        entityClassMetadata,
                        propAdded,
                        tableName,
                        propName,
                        customRootFieldColumnNameJSONBuilder,
                        customRelationshipNameJSONBuilder)
                if (propAdded != true && added == true) {
                    propAdded = true
                }
            }
        }
        customRootFieldColumnNameJSONBuilder.append(
                "\n\t\t\t}\n"
                        + "\t\t}\n"
                        + "\t}"
        )
        // Add optional relationship renames
        if (customRelationshipNameJSONBuilder.length != 0) {
            customRootFieldColumnNameJSONBuilder.append(",\n")
            customRootFieldColumnNameJSONBuilder.append(customRelationshipNameJSONBuilder)
        }
        return customRootFieldColumnNameJSONBuilder.toString()
    }

    /**
     * Adds a custom field name to `customFieldNameJSONBuilder` or a relationship with a custom name
     * to `customRelationshipNameJSONBuilder`.
     * @param classMetadata
     * @param tableName
     * @param propName
     * @param customFieldNameJSONBuilder
     * @param customRelationshipNameJSONBuilder
     * @return true if a custom field name has been added to `customFieldNameJSONBuilder` or false otherwise
     * (custom field name is not added, or relationship has been added)
     */
    private fun addCustomFieldNameOrRef(
            entity: EntityType<*>,
            classMetadata: AbstractEntityPersister,
            propAdded: Boolean,
            tableName: String,
            propNameIn: String,
            customFieldNameJSONBuilder: StringBuilder,
            customRelationshipNameJSONBuilder: StringBuilder): Boolean
    {
        var propName = propNameIn;

        // Handle @HasuraIgnoreRelationship annotation on field
        val f = Utils.findDeclaredFieldUsingReflection(entity.javaType, propName)
        if (f!!.isAnnotationPresent(HasuraIgnoreRelationship::class.java)) {
            return false
        }

        fun doAddCustomFieldNameOrRef(field: Field, columnName: String, columnType: Type, propType: Type) : Boolean
        {
            // Now we may alias field name according to @HasuraAlias annotation
            var hasuraAlias = f.getAnnotation(HasuraAlias::class.java)
            if (hasuraAlias != null && hasuraAlias.fieldAlias.isNotBlank()) {
                propName = hasuraAlias.fieldAlias
            }

            //
            // If it is an association type, add an array or object relationship
            //
            if (propType.isAssociationType) {
                if (propType.isCollectionType) {
                    val collType = propType as CollectionType
                    val join = collType.getAssociatedJoinable(sessionFactoryImpl as SessionFactoryImpl?)
                    val keyColumn = join.keyColumnNames[0]
                    tableNames.add(join.tableName)
                    val arrayRel =
                            """
                            {
                                "type": "create_array_relationship",
                                "args": {
                                    "name": "${propName}",
                                    "table": {
                                        "name": "${tableName}",
                                        "schema": "${schemaName}"
                                    },
                                    "using": {
                                        "foreign_key_constraint_on": {
                                            "table": {
                                                "name": "${join.tableName}",
                                                "schema": "${schemaName}"
                                            },
                                            "column": "${keyColumn}"
                                        }
                                    }
                                }
                            }                            
                        """
                    if (!generatedRelationships.contains(arrayRel)) {
                        generatedRelationships.add(arrayRel)
                        if (customRelationshipNameJSONBuilder.isNotEmpty()) {
                            customRelationshipNameJSONBuilder.append(",\n")
                        }
                        customRelationshipNameJSONBuilder.append(arrayRel)
                    }

                    // BasicCollectionPersister - despite the name - is for many-to-many associations
                    if (join is BasicCollectionPersister) {
                        if (join.isManyToMany) {
                            var res = handleManyToManyJoinTable(entity, join, f);
                            if (res != null) {
                                if (customRelationshipNameJSONBuilder.length > 0) {
                                    customRelationshipNameJSONBuilder.append(",\n")
                                }
                                customRelationshipNameJSONBuilder.append(
                                        """
                                        ${res}
                                    """
                                )
                            }
                        }
                    }

                    if (f.isAnnotationPresent(OneToMany::class.java)) {
                        val oneToMany = f.getAnnotation(OneToMany::class.java)
                        val parentRef = CaseUtils.toCamelCase(keyColumn, false, '_')
                        jsonSchemaGenerator.addSpecValue(f,
                                HasuraSpecPropValues(
                                        relation="one-to-many",
                                        mappedBy=oneToMany.mappedBy,
                                        parentReference=parentRef))

                    }
                } else {
                    val assocType = propType as AssociationType
                    val fkDir = assocType.foreignKeyDirection
                    if (fkDir === ForeignKeyDirection.FROM_PARENT) {
                        val objectRel =
                                """
                                {
                                    "type": "create_object_relationship",
                                    "args": {
                                        "name": "${propName}",
                                        "table": {
                                            "name": "${tableName}",
                                            "schema": "${schemaName}"
                                        },
                                        "using": {
                                            "foreign_key_constraint_on": "${columnName}"
                                        }
                                    }
                                }                                
                            """
                        // In case of @Inheritance(strategy = InheritanceType.SINGLE_TABLE) for each subclass
                        // we may get here to generate create_object_relationship, but if we already have it for
                        // any subclass then we should not create_object_relationship anymore
                        if (!generatedRelationships.contains(objectRel)) {
                            generatedRelationships.add(objectRel)
                            if (customRelationshipNameJSONBuilder.isNotEmpty()) {
                                customRelationshipNameJSONBuilder.append(",\n")
                            }
                            customRelationshipNameJSONBuilder.append(objectRel)
                        }
                        // Also add customization for the ID field name
                        val camelCasedIdName = CaseUtils.toCamelCase(columnName, false, '_')
                        if (propAdded) {
                            customFieldNameJSONBuilder.append(",\n")
                        }
                        customFieldNameJSONBuilder.append("\t\t\t\t\"$columnName\": \"$camelCasedIdName\"")

                        // Here just add the reference prop (adding EmptyRootFieldNames as we don't need it here actually)
                        jsonSchemaGenerator.addSpecValue(entity.javaType,
                                HasuraSpecTypeValues(
                                        referenceProp = HasuraReferenceProp(name=camelCasedIdName, type=jsonSchemaTypeFor(columnType, classMetadata)),
                                        rootFieldNames = EmptyRootFieldNames
                                ))

                        if (assocType is ManyToOneType && !assocType.isLogicalOneToOne) {
                            jsonSchemaGenerator.addSpecValue(f,
                                    HasuraSpecPropValues(relation = "many-to-one",
                                            reference = camelCasedIdName,
                                            referenceType = jsonSchemaTypeFor(columnType, classMetadata)
                                    )
                            )
                        }
                        else {
                            jsonSchemaGenerator.addSpecValue(f,
                                    HasuraSpecPropValues(relation = "one-to-one",
                                            reference = camelCasedIdName,
                                            referenceType = jsonSchemaTypeFor(columnType, classMetadata)
                                    )
                            )
                        }
                        return true
                    } else { // TO_PARENT, ie. assocition is mapped by the other side
                        val join = assocType.getAssociatedJoinable(sessionFactoryImpl as SessionFactoryImpl?)
                        val keyColumn = join.keyColumnNames[0]
                        val mappedBy = assocType.rhsUniqueKeyPropertyName
                        // In reality mappedBy should always be set. It is either the implicit name or a specific
                        // mappedBy=<foreign property name> and so it cannot be null. I'm not sure about it so I
                        // leave here a fallback of the default Hiberante tableName_keyColumn foreign key.
                        val mappedId = if (mappedBy != null) (join as AbstractEntityPersister).getPropertyColumnNames(mappedBy)[0]
                                        else "${tableName}_${keyColumn}"
                        val objectRel =
                                """
                                {
                                    "type": "create_object_relationship",
                                    "args": {
                                        "name": "${propName}",
                                        "table": {
                                            "name": "${tableName}",
                                            "schema": "${schemaName}"
                                        },
                                        "using": {
                                            "manual_configuration": {
                                                "remote_table": {
                                                    "name": "${join.tableName}",
                                                    "schema": "${schemaName}"
                                                },
                                                "column_mapping": {
                                                    "${keyColumn}": "${mappedId}"
                                                }
                                            }
                                        }
                                    }
                                }                                
                            """
                        if (!generatedRelationships.contains(objectRel)) {
                            generatedRelationships.add(objectRel)
                            if (customRelationshipNameJSONBuilder.isNotEmpty()) {
                                customRelationshipNameJSONBuilder.append(",\n")
                            }
                            customRelationshipNameJSONBuilder.append(objectRel)
                        }

                        val oneToOne = f.getAnnotation(OneToOne::class.java)
                        oneToOne?.let {
                            jsonSchemaGenerator.addSpecValue(f,
                                    HasuraSpecPropValues(relation="one-to-one", mappedBy=oneToOne.mappedBy))

                        }
                    }

                }
            } else if (columnName != propName) {
                if (propAdded) {
                    customFieldNameJSONBuilder.append(",\n")
                }
                customFieldNameJSONBuilder.append("\t\t\t\t\"$columnName\": \"$propName\"")
                return true
            }
            return false
        }

        // In case of @Embedded class there will be a single property with multiple property column names
        val type = classMetadata.toType(propName)
        if (type is ComponentType) {
            val componentType = type as ComponentType
            val tup = (type as ComponentType).componentTuplizer
            val propNames = classMetadata.getPropertyColumnNames(propName)
            var result = false
            propNames.forEachIndexed { index, columnName ->
                val field = tup.getGetter(index).getMember()
                if (field is Field) {
                    // Reset propName to the embedded field name
                    propName = (field as Field).name
                    // TODO: why do we have separate columnType and propType?
                    val columnType = componentType.subtypes[index]
                    val propType = componentType.subtypes[index]
                    //println("$columnName --> ${field.name}")
                    val ret = doAddCustomFieldNameOrRef(field, columnName, columnType, propType)
                    // If any doAddCustomFieldNameOrRef returns true, make final result true
                    if (ret) {
                        result = ret
                    }
                }
                else {
                    throw HasuraConfiguratorException("Unable to handle embeded class ${entity} and getter ${field} ")
                }
            }
            return result
        }
        else {
            val columnName = classMetadata.getPropertyColumnNames(propName)[0]
            // TODO: why do we have separate columnType and propType?
            val columnType = classMetadata.getPropertyType(propName)
            val propType = classMetadata.getPropertyType(propName)
            return doAddCustomFieldNameOrRef(f, columnName, columnType, propType)
        }
    }

    private fun jsonSchemaTypeFor(columnType: Type, classMetadata: AbstractEntityPersister): String {

        fun toJsonSchmeType(actualType: Type) : String
        {
            if (actualType is LongType || actualType is IntegerType || actualType is ShortType
                    || actualType is BigDecimalType || actualType is BigIntegerType) {
                return "integer"
            }
            else if (actualType is StringType) {
                return "string";
            }
            return "<UNKNOWN TYPE>";
        }

        if (columnType is ManyToOneType) {
            val refType = columnType.getIdentifierOrUniqueKeyType(classMetadata.factory)
            return toJsonSchmeType(refType)
        }
        // TODO:
        else {
            return toJsonSchmeType(columnType)
        }
        return "<UNKNOWN TYPE>";
    }

    private fun graphqlTypeFor(columnType: Type, classMetadata: AbstractEntityPersister): String {
        val sqlType = columnType.sqlTypes(sessionFactoryImpl as SessionFactoryImpl)[0];
        val keyTypeName = classMetadata.factory.jdbcServices.dialect.getTypeName(sqlType)
        if (postgresqlNames[keyTypeName] == null) {
            throw Error("No postgresql alias found for type $keyTypeName. Graphql type cannot be calculated.")
        }
        return postgresqlNames[keyTypeName]!!
    }


    /**
     * Many-to-many relationships are represented with a joint table, however this table has no Java
     * representation therefore no hasura configuration coul dbe generated for them the usual reflection
     * driven way. Instead we need to generate these based on the
     */
    private fun handleManyToManyJoinTable(entity: EntityType<*>, join: BasicCollectionPersister, field: Field): String?
    {
        val tableName = join.tableName
        var entityName = tableName.toCase(CaseFormat.CAPITALIZED_CAMEL);
        var keyColumn = join.keyColumnNames[0]
        var keyColumnType = join.keyType
        var keyColumnAlias = keyColumn.toCamelCase();
        var relatedColumnName = join.elementColumnNames[0]
        var relatedColumnType = join.elementType
        var relatedColumnNameAlias = relatedColumnName.toCamelCase()

        // Make sure we don't generate custom field name customization more than once for any table.
        // This can happen in case of many-to-many join tables when generating for inverse join as well.
        if (generatedCustomFieldNamesForManyToManyJoinTables.contains(tableName)) {
            return null
        }
        generatedCustomFieldNamesForManyToManyJoinTables.add(tableName)

        // Get the HasuraAlias and may reset entityName
        var alias = field.getAnnotation(HasuraAlias::class.java);
        var rootFields = if(alias != null) alias.rootFieldAliases else null
        if (rootFields != null) {
            if (rootFields.baseName.isNotBlank()) {
                entityName = rootFields.baseName
            }
        }
        if (alias != null) {
            if (alias.keyColumnAlias.isNotBlank()) {
                keyColumnAlias = alias.keyColumnAlias
            }
            else if (alias.joinColumnAlias.isNotBlank()) {
                keyColumnAlias = alias.joinColumnAlias
            }

            if (alias.relatedColumnAlias.isNotBlank()) {
                relatedColumnNameAlias = alias.relatedColumnAlias
            }
            else if (alias.inverseJoinColumnAlias.isNotBlank()) {
                relatedColumnNameAlias = alias.inverseJoinColumnAlias
            }
        }

        val customRootFieldColumnNameJSONBuilder = StringBuilder()
        tableNames.add(tableName)
        // Remove inner $ from the name of inner classes
        entityName = entityName.replace("\\$".toRegex(), "")
        // Copy
        var entityNameLower = entityName.toString()
        entityNameLower = Character.toLowerCase(entityNameLower[0]).toString() + entityNameLower.substring(1)

        // arrayRel only allows accessing the join table ID fields. Now add navigation to the
        // related entity
        val relatedTableName = (join.elementType as ManyToOneType).getAssociatedJoinable(sessionFactoryImpl as SessionFactoryImpl?).tableName;
        var joinFieldName = relatedTableName.toCamelCase();
        if (alias != null && alias.joinFieldAlias.isNotBlank()) {
            joinFieldName = alias.joinFieldAlias;
        }

        val classMetadata = metaModel.entityPersister(entity.javaType.typeName) as AbstractEntityPersister
        val keyTableName = classMetadata.tableName
        val keyFieldName = keyTableName.toCamelCase()

        val rootFieldNames = generateRootFieldNames(rootFields, entityName, entityNameLower, tableName)

        val objectRel = """
                    {
                        "type": "create_object_relationship",
                        "args": {
                            "name": "${joinFieldName}",
                            "table": {
                                "name": "${join.tableName}",
                                "schema": "${schemaName}"
                            },
                            "using": {
                                "foreign_key_constraint_on": "${relatedColumnName}"
                            }
                        }
                    }
                    """
        if (!generatedRelationships.contains(objectRel)) {
            generatedRelationships.add(objectRel)
            if (customRootFieldColumnNameJSONBuilder.length > 0) {
                customRootFieldColumnNameJSONBuilder.append(",")
            }
            customRootFieldColumnNameJSONBuilder.append(objectRel)
        }

        if (customRootFieldColumnNameJSONBuilder.length > 0) {
            customRootFieldColumnNameJSONBuilder.append(",")
        }
        customRootFieldColumnNameJSONBuilder.append(
                """
                    ${generateSetTableCustomFields(tableName, rootFieldNames)}
                    "custom_column_names": {
                         "${keyColumn}": "${keyColumnAlias}",
                         "${relatedColumnName}": "${relatedColumnNameAlias}"
                """
        )
        // Add index columns names, ie. @OrderColumns
        join.indexColumnNames?.forEach { customRootFieldColumnNameJSONBuilder.append(
                """
                   ,"${it}": "${it.toCamelCase()}" 
                """
        ) }

        // Add ending curly brackets
        customRootFieldColumnNameJSONBuilder.append(
                """
                            }
                         }
                    }                    
                """
        )

        jsonSchemaGenerator.addSpecValue(field,
                HasuraSpecPropValues(
                        relation="many-to-many",
                        type=tableName.toCase(CaseFormat.CAPITALIZED_CAMEL),
                        reference=relatedColumnNameAlias,
                        parentReference=keyColumnAlias,
                        item=joinFieldName)
                )

        jsonSchemaGenerator.addJoinType(JoinType(
                name = tableName.toCase(CaseFormat.CAPITALIZED_CAMEL),
                tableName = tableName,
                fromIdName = keyColumnAlias,
                fromIdType = jsonSchemaTypeFor(join.keyType, classMetadata),
                fromIdGraphqlType = graphqlTypeFor(join.keyType, classMetadata),
                fromAccessor = keyFieldName,
                fromAccessorType= entity.name,
                toIdName = relatedColumnNameAlias,
                toIdType = jsonSchemaTypeFor(relatedColumnType, classMetadata),
                toIdGraphqlType = graphqlTypeFor(relatedColumnType, classMetadata),
                toAccessor = joinFieldName,
                toAccessorType = relatedTableName.toCase(CaseFormat.CAPITALIZED_CAMEL),
                orderField = join.indexColumnNames?.let {
                    it[0].toCamelCase()
                },
                orderFieldType = join.indexColumnNames?.let{
                    jsonSchemaTypeFor(join.indexType, classMetadata)
                },
                orderFieldGraphqlType = join.indexColumnNames?.let{
                    graphqlTypeFor(join.indexType, classMetadata)
                },
                rootFieldNames = rootFieldNames
        ))


//        println(customFieldNameJSONBuilder)
        return customRootFieldColumnNameJSONBuilder.toString()
    }

    private fun generateRootFieldNames(
            rootFields: HasuraRootFields?,
            entityName: String,
            entityNameLower: String,
            tableName: String) : RootFieldNames
    {
        val rootFieldNames = RootFieldNames(
                select = "${if (rootFields != null && rootFields.select.isNotBlank()) rootFields.select else rootFieldNameProvider.rootFieldFor("select", entityName, entityNameLower, tableName)}",
                selectByPk = "${if (rootFields != null && rootFields.selectByPk.isNotBlank()) rootFields.selectByPk else rootFieldNameProvider.rootFieldFor("selectByPk", entityName, entityNameLower, tableName)}",
                selectAggregate = "${if (rootFields != null && rootFields.selectAggregate.isNotBlank()) rootFields.selectAggregate else rootFieldNameProvider.rootFieldFor("selectAggregate", entityName, entityNameLower, tableName)}",
                insert = "${if (rootFields != null && rootFields.insert.isNotBlank()) rootFields.insert else rootFieldNameProvider.rootFieldFor("insert", entityName, entityNameLower, tableName)}",
                insertOne =  "${if (rootFields != null && rootFields.insertOne.isNotBlank()) rootFields.insertOne else rootFieldNameProvider.rootFieldFor("insertOne", entityName, entityNameLower, tableName)}",
                update = "${if (rootFields != null && rootFields.update.isNotBlank()) rootFields.update else rootFieldNameProvider.rootFieldFor("update", entityName, entityNameLower, tableName)}",
                updateByPk = "${if (rootFields != null && rootFields.updateByPk.isNotBlank()) rootFields.updateByPk else rootFieldNameProvider.rootFieldFor("updateByPk", entityName, entityNameLower, tableName)}",
                delete = "${if (rootFields != null && rootFields.delete.isNotBlank()) rootFields.delete else rootFieldNameProvider.rootFieldFor("delete", entityName, entityNameLower, tableName)}",
                deleteByPk = "${if (rootFields != null && rootFields.deleteByPk.isNotBlank()) rootFields.deleteByPk else rootFieldNameProvider.rootFieldFor("deleteByPk", entityName, entityNameLower, tableName)}"
        )

        // If the original field name was plural then we may have name clashes.
        // Eg. for a class named BookSeries we would have bookSeries for both select and selectByPk.
        // We fix that by adding an "es" to the plural operations.
        if (rootFieldNames.select == rootFieldNames.selectByPk) {
            LOG.warn("Generated plural name for select and selectByPk names '${rootFieldNames.select}' clash, "+
                     "adding '-es' postfix to 'select'. " +
                     "Consider using @HasuraRootFields on class ${entityName}")
            rootFieldNames.select += "es"
        }
        if (rootFieldNames.insert == rootFieldNames.insertOne) {
            LOG.warn("Generated plural name for insert and insertOne names '${rootFieldNames.insert}' clash, "+
                    "adding '-es' postfix to 'insert'. " +
                    "Consider using @HasuraRootFields on class ${entityName}")
            rootFieldNames.insert += "es"
        }
        if (rootFieldNames.update == rootFieldNames.updateByPk) {
            LOG.warn("Generated plural name for update and updateByPk names '${rootFieldNames.update}' clash, "+
                    "adding '-es' postfix to 'update'. " +
                    "Consider using @HasuraRootFields on class ${entityName}")
            rootFieldNames.update += "es"
        }
        if (rootFieldNames.delete == rootFieldNames.deleteByPk) {
            LOG.warn("Generated plural name for delete and deleteByPk names '${rootFieldNames.delete}' clash, "+
                    "adding '-es' postfix to 'delete'. " +
                    "Consider using @HasuraRootFields on class ${entityName}")
            rootFieldNames.delete += "es"
        }

        return rootFieldNames;
    }

    private fun generateSetTableCustomFields(
            tableName: String,
            rootFieldNames: RootFieldNames) : String
    {
        val customFields =  """
                    {
                        "type": "set_table_custom_fields",
                        "version": 2,
                        "args": {
                            "table": "${tableName}",
                            "schema": "${schemaName}",
                            "custom_root_fields": {
                                "select":           "${rootFieldNames.select}",
                                "select_by_pk":     "${rootFieldNames.selectByPk}",
                                "select_aggregate": "${rootFieldNames.selectAggregate}",
                                "insert":           "${rootFieldNames.insert}",
                                "insert_one":       "${rootFieldNames.insertOne}",
                                "update":           "${rootFieldNames.update}",
                                "update_by_pk":     "${rootFieldNames.updateByPk}", 
                                "delete":           "${rootFieldNames.delete}",
                                "delete_by_pk":     "${rootFieldNames.deleteByPk}"
                            },
                """
        return customFields
    }

    /**
     *
     */
    private fun loadConfScriptIntoHasura() {
        LOG.info("Executing Hasura initialization JSON from {}. This may take a while ...", confFile)
        val client = WebClient
                .builder()
                .baseUrl(hasuraEndpoint)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Hasura-Admin-Secret", hasuraAdminSecret)
                .build()
        val request = client.post()
                .body<String, Mono<String>>(Mono.just(confJson!!), String::class.java)
                .retrieve()
                .bodyToMono(String::class.java)
        // Make it synchronous for now
        try {
            val result = request.block()
            LOG.info("Hasura initialization done {}", result)
        } catch (ex: WebClientResponseException) {
            LOG.error("Hasura initialization failed", ex)
            LOG.error("Response text: {}", ex.responseBodyAsString)
            throw ex
        }
        //        result.subscribe(
//                value -> System.out.println(value),
//                error -> error.printStackTrace(),
//                () -> System.out.println("completed without a value")
//        );
    }

}

interface RootFieldNameProvider {
    fun rootFieldFor(fieldName: String, entityName: String, entityNameLower: String, tableName: String) : String
}

open class DefaultRootFieldNameProvider : RootFieldNameProvider
{
    override fun rootFieldFor(fieldName: String, entityName: String, entityNameLower: String, tableName: String) : String {
        return when(fieldName) {
            "select" -> English.plural(entityNameLower)
            "selectByPk" -> entityNameLower
            "selectAggregate" -> entityNameLower+"Aggregate"
            "insert" -> "create"+English.plural(entityName)
            "insertOne" -> "create"+entityName
            "update" -> "update"+English.plural(entityName)
            "updateByPk" -> "update"+entityName
            "delete" -> "delete"+English.plural(entityName)
            "deleteByPk" -> "delete"+entityName
            else -> throw HasuraConfiguratorException("Unknown root field name: $fieldName")
        }
    }
}

/**
 * Root field aliases for a specific type
 */
class RootFieldNames(
        var select : String,
        var selectByPk : String,
        var selectAggregate : String,
        var insert : String,
        var insertOne : String,
        var update : String,
        var updateByPk : String,
        var delete : String,
        var deleteByPk : String
)

/**
 * A dummy, empty RootFieldNames instance
 */
val EmptyRootFieldNames = RootFieldNames("", "", "", "", "", "", "", "", "")