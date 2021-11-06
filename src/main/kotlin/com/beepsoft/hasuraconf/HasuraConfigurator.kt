package com.beepsoft.hasuraconf

import com.beepsoft.hasuraconf.annotation.*
import com.google.common.net.HttpHeaders
import kotlinx.serialization.decodeFromString
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
        var loadConf: Boolean,
        var metadataJsonFile: String?,
        var loadMetadata: Boolean,
        var cascadeDeleteJsonFile: String?,
        var loadCascadeDelete: Boolean,
        var schemaName: String,
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
        public val LOG = getLogger(this::class.java.enclosingClass)

        // Acrtual postgresql types for some SQL types. Hibernate uses the key fields when generating
        // tables, however Postgresql uses the "values" of postgresqlNames and so does Hasura when
        // generating graphql schema based on the DB schema.
        // https://hasura.io/docs/1.0/graphql/manual/api-reference/postgresql-types.html
        // TODO: should check these some more ...
        public val postgresqlNames = mapOf(
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
        )
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

    var cascadeDeleteJson: String? = null
        private set // the setter is private and has the default implementation

    private var sessionFactoryImpl: SessionFactory
    private var metaModel: MetamodelImplementor
    private var permissionAnnotationProcessor: PermissionAnnotationProcessor

    private lateinit var jsonSchemaGenerator: HasuraJsonSchemaGenerator

    private lateinit var tableNames: MutableSet<String>
    private lateinit var entityClasses: MutableSet<Class<out Any>>
    private lateinit var cascadeDeleteFields: MutableSet<CascadeDeleteFields>
    private lateinit var manyToManyEntities: MutableMap<String, ManyToManyEntity>
    private lateinit var extraTableNames: MutableSet<String>

    init {
        sessionFactoryImpl = entityManagerFactory.unwrap(SessionFactoryImpl::class.java)
        metaModel = sessionFactoryImpl.metamodel as MetamodelImplementor
        permissionAnnotationProcessor = PermissionAnnotationProcessor(entityManagerFactory)
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
            val join1: BasicCollectionPersister,
            var join2: BasicCollectionPersister? = null,
            val field1: Field,
            var field2: Field? = null,
    )

    /**
     * Creates hasura-conf.json containing table tracking and field/relationship name customizations
     * at bean creation time automatically.
     */
    @Throws(HasuraConfiguratorException::class)
    fun configure()
    {
        jsonSchemaGenerator = HasuraJsonSchemaGenerator(schemaVersion, customPropsFieldName)
        cascadeDeleteFields = mutableSetOf<CascadeDeleteFields>()
        manyToManyEntities = mutableMapOf()
        extraTableNames = mutableSetOf()
        tableNames = mutableSetOf()
        entityClasses = mutableSetOf<Class<out Any>>()

        // Get metaModel.entities sorted by name. We do this sorting to make result more predictable (eg. for testing)
        val entities = sortedSetOf(
                Comparator { o1, o2 ->  o1.name.compareTo(o2.name)},
                *metaModel.entities.toTypedArray() )

        val tables = buildJsonArray {
            // Add config for entities that have Jave class mappings
            for (entity in entities) {
                // Ignore subclasses of classes with @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
                // In this case all subclasse's fields will be stored in the parent's table and therefore subclasses
                // cannot have root operations.
                if (entity.parentHasSingleTableInheritance()) {
                    continue
                }

                add(configureEntity(entity, entities))
            }
            // Add config for many-to-many join tables, which have no Java class representation but still
            // want to have access to them
            for (m2m in manyToManyEntities.values) {
                configureManyToManyEntity(m2m)?.let {
                    add(it)
                }
            }

            // Add tracking to extra table names. For now these could be join tables of array relations
            for (tableName in extraTableNames) {
                // if tableName is also present in standard tableNames, it means it was either a Java mappes
                // entity or an m2m join table which we handle previously
                if (tableNames.contains(tableName)) {
                    continue
                }
                // It is really an extra table name, add it
                add(buildJsonObject {
                    putJsonObject("table") {
                        put("schema", schemaName)
                        put("name", tableName)
                    }
                })
            }
        }

        metadataJsonObject = buildJsonObject {
            put("version", 2)
            put("tables", tables)
        }
        metadataJson = Json.encodeToString(metadataJsonObject).reformatJson()


        if (metadataJsonFile != null && metadataJsonFile != "null" && metadataJsonFile!!.trim() != "") {
            PrintWriter(metadataJsonFile).use { out -> out.println(metadataJson) }
        }
        if (loadMetadata && metadataJson != null) {
            loadMetadataIntoHasura()
        }

        if (cascadeDeleteFields.isNotEmpty()) {
            cascadeDeleteJson = Json.encodeToString(buildJsonObject {
                put("type", "bulk")
                put("args", configureCascadeDeleteTriggers())
            }).reformatJson()

            cascadeDeleteJsonFile?.let {
                PrintWriter(it).use { out -> out.println(cascadeDeleteJson) }
            }

            if (loadCascadeDelete && cascadeDeleteJson != null) {
                loadCascadeDeleteIntoHasura()
            }
        }

        // This creates confJson
        createBulkConfJson()

        if (confFile != null && confFile != "null" && confFile!!.trim() != "") {
            PrintWriter(confFile).use { out -> out.println(confJson) }
        }
        if (loadConf && confJson != null) {
            loadConfIntoHasura()
        }

        if (!ignoreJsonSchema) {
            jsonSchema = jsonSchemaGenerator.generateSchema(*entityClasses.toTypedArray()).toString().reformatJson()
            if (schemaFile != null && schemaFile != "null" && schemaFile!!.trim() != "") {
                PrintWriter(schemaFile).use { out -> out.println(jsonSchema) }
            }
        }
    }

    /**
     * Takes the metadataJson and creates a Metadata API Json
     */
    private fun createBulkConfJson()
    {
        val apiJson = buildJsonObject {
            put("type", "bulk")
            putJsonArray("args") {
                // {
                //    "type" : "clear_metadata",
                //    "args" : { }
                //  }
                add(buildJsonObject {
                    put("type", "clear_metadata")
                    put("args", buildJsonObject{})
                })

                // track_table
                // set_table_is_enum
                (metadataJsonObject["tables"] as JsonArray).forEach { table ->
                    val tableObject = table as JsonObject

                    // {
                    //    "type" : "track_table",
                    //    "args" : {
                    //      "schema" : "public",
                    //      "name" : "calendar_availability"
                    //    }
                    //  }
                    add(buildJsonObject {
                        put("type", "track_table")
                        put("args", tableObject["table"]!!.clone())
                    })

                    // {
                    //    "type" : "set_table_is_enum",
                    //    "args" : {
                    //      "table" : {
                    //        "schema" : "public",
                    //        "name" : "calendar_availability"
                    //      },
                    //      "is_enum" : true
                    //    }
                    //  }
                    // If it is an enum table set_table_is_enum
                    if (table["is_enum"] != null && (table["is_enum"] as JsonPrimitive).boolean == true) {
                        add(buildJsonObject {
                            put("type", "set_table_is_enum")
                            put("args", buildJsonObject {
                                put("table", tableObject["table"]!!.clone())
                                put("is_enum", true)
                            })
                        })
                    }
                }

                // set_table_custom_fields
                // create_object_relationship
                // create_array_relationship
                // create_insert_permission
                // create_select_permission
                // create_update_permission
                // create_delete_permission
                (metadataJsonObject["tables"] as JsonArray).forEach { table ->
                    // {
                    //    "type" : "set_table_custom_fields",
                    //    "version" : 2,
                    //    "args" : {
                    //      "table" : "book_series",
                    //      "schema" : "public",
                    //      "custom_root_fields" : {
                    //        "select" : "bookSeriesMulti",
                    //        "select_by_pk" : "bookSeries",
                    //        "select_aggregate" : "bookSeriesAggregate",
                    //        "insert" : "createBookSeriesMulti",
                    //        "insert_one" : "createBookSeries",
                    //        "update" : "updateBookSeriesMulti",
                    //        "update_by_pk" : "updateBookSeries",
                    //        "delete" : "deleteBookSeriesMulti",
                    //        "delete_by_pk" : "deleteBookSeries"
                    //      },
                    //      "custom_column_names" : {
                    //        "created_at" : "createdAt",
                    //        "updated_at" : "updatedAt"
                    //      }
                    //    }
                    //  }
                    val tableObject = table as JsonObject

                    if (tableObject["configuration"] != null) {
                        add(buildJsonObject {
                            put("type", "set_table_custom_fields")
                            put("version", 2)
                            put("args", buildJsonObject {
                                val nameAndSchema = tableObject["table"]!! as JsonObject
                                put("table", nameAndSchema["name"]!!)
                                put("schema", nameAndSchema["schema"]!!)

                                // Copy custom_root_fields and custom_column_names from configuration to the
                                // api operation
                                val rootFieldsAndColumNames = tableObject["configuration"]!!.clone() as JsonObject
                                rootFieldsAndColumNames.forEach { k, v ->
                                    this.put(k, v)
                                }
                            })
                        })
                    }

                    // Same logic for:
                    // create_object_relationship
                    // create_array_relationship
                    // create_insert_permission
                    // create_select_permission
                    // create_update_permission
                    // create_delete_permission
                    fun addAllFromArray(metaJsonName: String, metaApiJsonName: String)
                    {
                        (tableObject[metaJsonName] as JsonArray?)?.let {elems ->
                            elems.forEach {elem ->
                                add(buildJsonObject {
                                    put("type", metaApiJsonName)
                                    put("args", buildJsonObject {
                                        put("table", (table as JsonObject)["table"]!!.clone())
                                        // add rrole, permission
                                        val elemClone = elem.clone() as JsonObject
                                        elemClone.forEach { k, v ->
                                            this.put(k, v)
                                        }

                                    })

                                })
                            }
                        }
                    }

                    // {
                    //    "type" : "create_object_relationship",
                    //    "args" : {
                    //      "table" : {
                    //        "name" : "calendar",
                    //        "schema" : "public"
                    //      },
                    //      "name" : "availability",
                    //      "using" : {
                    //        "foreign_key_constraint_on" : "availability_value"
                    //      }
                    //    }
                    //  }
                    addAllFromArray("object_relationships", "create_object_relationship")


                    // {
                    //    "type" : "create_array_relationship",
                    //    "args" : {
                    //      "table" : {
                    //        "name" : "calendar",
                    //        "schema" : "public"
                    //      },
                    //      "name" : "children",
                    //      "using" : {
                    //        "foreign_key_constraint_on" : {
                    //          "table" : {
                    //            "name" : "calendar_parents",
                    //            "schema" : "public"
                    //          },
                    //          "column" : "parents_id"
                    //        }
                    //      }
                    //    }
                    //  }
                    addAllFromArray("array_relationships", "create_array_relationship")

                    // {
                    //    "type" : "create_insert_permission",
                    //    "args" : {
                    //      "table" : {
                    //        "name" : "calendar",
                    //        "schema" : "public"
                    //      },
                    //      "role" : "USER",
                    //      "permission" : {
                    //        "set" : { },
                    //        "columns" : "*",
                    //        "allow_aggregations" : true,
                    //        "check" : { }
                    //      }
                    //    }
                    //  }
                    addAllFromArray("insert_permissions", "create_insert_permission")

                    // {
                    //    "type" : "create_select_permission",
                    //    "args" : {
                    //      "table" : {
                    //        "name" : "calendar",
                    //        "schema" : "public"
                    //      },
                    //      "role" : "USER",
                    //      "permission" : {
                    //        "set" : { },
                    //        "columns" : "*",
                    //        "allow_aggregations" : true,
                    //        "filter" : {
                    //          "roles" : {
                    //            "user_id" : {
                    //              "_eq" : "X-Hasura-User-Id"
                    //            }
                    //          }
                    //        }
                    //      }
                    //    }
                    //  }
                    addAllFromArray("select_permissions", "create_select_permission")

                    // {
                    //    "type" : "create_update_permission",
                    //    "args" : {
                    //      "table" : {
                    //        "name" : "calendar",
                    //        "schema" : "public"
                    //      },
                    //      "role" : "USER",
                    //      "permission" : {
                    //        "set" : { },
                    //        "columns" : "*",
                    //        "allow_aggregations" : true,
                    //        "filter" : {
                    //          "_and" : [ {
                    //            "roles" : {
                    //              "user_id" : {
                    //                "_eq" : "X-Hasura-User-Id"
                    //              }
                    //            }
                    //          }, {
                    //            "roles" : {
                    //              "role_value" : {
                    //                "_in" : [ "OWNER", "EDITOR" ]
                    //              }
                    //            }
                    //          } ]
                    //        }
                    //      }
                    //    }
                    //  }
                    addAllFromArray("update_permissions", "create_update_permission")

                    // {
                    //    "type" : "create_delete_permission",
                    //    "args" : {
                    //      "table" : {
                    //        "name" : "calendar",
                    //        "schema" : "public"
                    //      },
                    //      "role" : "USER",
                    //      "permission" : {
                    //        "set" : { },
                    //        "columns" : [ "created_at", "updated_at", "availability_value", "id", "description", "locale_country", "next_version_id", "published", "theme_id", "theme_config", "title", "version" ],
                    //        "allow_aggregations" : true,
                    //        "filter" : {
                    //          "_and" : [ {
                    //            "roles" : {
                    //              "user_id" : {
                    //                "_eq" : "X-Hasura-User-Id"
                    //              }
                    //            }
                    //          }, {
                    //            "roles" : {
                    //              "role_value" : {
                    //                "_in" : [ "OWNER", "EDITOR" ]
                    //              }
                    //            }
                    //          } ]
                    //        }
                    //      }
                    //    }
                    //  }
                    addAllFromArray("delete_permissions", "create_delete_permission")
                }

                // {
                //    "type" : "run_sql",
                //    "args" : {
                //      "sql" : "DROP TRIGGER IF EXISTS calendar_next_version_id_cascade_delete_trigger ON calendar;; DROP FUNCTION  IF EXISTS calendar_next_version_id_cascade_delete(); CREATE FUNCTION calendar_next_version_id_cascade_delete() RETURNS trigger AS $body$ BEGIN     IF TG_WHEN <> 'AFTER' OR TG_OP <> 'DELETE' THEN         RAISE EXCEPTION 'calendar_next_version_id_cascade_delete may only run as a AFTER DELETE trigger';     END IF;      DELETE FROM calendar where id=OLD.next_version_id;     RETURN OLD; END; $body$ LANGUAGE plpgsql;; CREATE TRIGGER calendar_next_version_id_cascade_delete_trigger AFTER DELETE ON calendar     FOR EACH ROW EXECUTE PROCEDURE calendar_next_version_id_cascade_delete();;                       "
                //    }
                //  }
                configureCascadeDeleteTriggers().forEach {
                    add(it)
                }
            }
        }
        confJson = Json.encodeToString(apiJson).reformatJson()
    }

    private fun JsonElement.clone(): JsonElement =
            Json.decodeFromString(Json.encodeToString(this))

    private fun configureEntity(entity: EntityType<*>, entities: Set<EntityType<*>>) : JsonObject
    {
        val relatedEntities = entity.relatedEntities(entities)
        val targetEntityClassMetadata = metaModel.entityPersister(entity.javaType.typeName) as AbstractEntityPersister
        val tableName = targetEntityClassMetadata.tableName
        val keyKolumnName = targetEntityClassMetadata.keyColumnNames[0]

        tableNames.add(tableName)

        // Add ID field
        val f = Utils.findDeclaredFieldUsingReflection(entity.javaType, targetEntityClassMetadata.identifierPropertyName)
        jsonSchemaGenerator.addSpecValue(f!!,
                HasuraSpecPropValues(graphqlType = graphqlTypeFor(targetEntityClassMetadata.identifierType, targetEntityClassMetadata)))

        entityClasses.add(entity.javaType)

        var entityName = entity.name

        // Get the HasuraRootFields and may reset entityName
        var rootFields = entity.javaType.getAnnotation(HasuraRootFields::class.java)
        if (rootFields != null && rootFields.baseName.isNotBlank()) {
            entityName = rootFields.baseName
        }

        // Remove inner $ from the name of inner classes
        entityName = entityName.replace("\\$".toRegex(), "")
        var entityNameLower = entityName.toString()
        entityNameLower = Character.toLowerCase(entityNameLower[0]).toString() + entityNameLower.substring(1)

        val rootFieldNames = generateRootFieldNames(rootFields, entityName, entityNameLower, tableName)

        jsonSchemaGenerator.addSpecValue(entity.javaType,
                HasuraSpecTypeValues(
                        graphqlType=tableName,
                        idProp=keyKolumnName,
                        rootFieldNames = rootFieldNames))

        collectCascadeDeleteCandidates(entity)

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

    private fun configureCascadeDeleteTriggers(): JsonArray
    {
        val triggers = buildJsonArray {
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

                add(buildJsonObject {
                    put("type", "run_sql")
                    putJsonObject("args") {
                        put("sql", trigger)
                    }
                })
            }
        }
        return triggers
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

    internal data class M2MData(
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

    /**
     * Calculates all kinds of ManyToManyEntity related values
     */
    internal fun ManyToManyEntity.toM2MData(field: Field, join: BasicCollectionPersister) : M2MData
    {
        val tableName = join.tableName
        var entityName = tableName.toCase(CaseFormat.CAPITALIZED_CAMEL);
        var keyColumn = join.keyColumnNames[0]
        var keyColumnType = join.keyType
        var keyColumnAlias = keyColumn.toCamelCase();
        var relatedColumnName = join.elementColumnNames[0]
        var relatedColumnType = join.elementType
        var relatedColumnNameAlias = relatedColumnName.toCamelCase()

        tableNames.add(tableName)

        // Get the HasuraAlias and may reset entityName
        var alias = field.getAnnotation(HasuraAlias::class.java)
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
        val relatedTableName = (join.elementType as ManyToOneType).getAssociatedJoinable(sessionFactoryImpl as SessionFactoryImpl?).tableName;
        var joinFieldName = relatedTableName.toCamelCase();
        if (alias != null && alias.joinFieldAlias.isNotBlank()) {
            joinFieldName = alias.joinFieldAlias;
        }

        val classMetadata = metaModel.entityPersister(this.entity.javaType.typeName) as AbstractEntityPersister
        val keyTableName = classMetadata.tableName
        val keyFieldName = keyTableName.toCamelCase()

        return M2MData(
                join,
                field,
                tableName,
                entityName,
                entityNameLower,
                keyColumn,
                keyColumnAlias,
                keyColumnType,
                relatedColumnName,
                relatedColumnNameAlias,
                relatedColumnType,
                joinFieldName,
                relatedTableName,
                keyFieldName,
                rootFields
        )
    }

    /**
     * Configures a ManyToManyEntity. Such entities have no Java representation but nevertheless we
     * generate configuration for these join tables. These tables have two side and usually accessed from both
     * ends. The difficulty to manage them is that we need to generate the object relationship for both sides
     * and also generate JSON Schema for both sides while we need to generate the common parts once. The way
     * we do it is that for the common part (custom_root_fields, custom_column_names) we generate M2MDate for
     * ManyToManyEntity.field1 and ManyToManyEntity.join1. The @HasuraAlias on such join entities should be
     * declared the same on both side (except for the joinFieldAlias), so it doesn't really matter for the
     * common definitions which join side is field1/join1 or field2/join2. Now, once the common parts are
     * defined we need to generate the object relationship for the two sides. For side1 we reuse the
     * M2MData created for the common parts, and for field2/join2 we generate a new M2MData.
     */
    private fun configureManyToManyEntity(m2m: ManyToManyEntity) : JsonObject?
    {
        val tableJson = buildJsonObject {
            // Get M2MData for the common definition parts
            var m2mData = m2m.toM2MData(m2m.field1, m2m.join1)
            with(m2mData) {
                val rootFieldNames = generateRootFieldNames(rootFields, entityName, entityNameLower, tableName)

                put("table", buildJsonObject {
                    put("schema", schemaName)
                    put("name", tableName)
                })
                put("configuration", buildJsonObject {
                    put("custom_root_fields", configureRootFieldNames(rootFieldNames))
                    putJsonObject("custom_column_names") {
                        put(keyColumn, keyColumnAlias)
                        put(relatedColumnName, relatedColumnNameAlias)
                        // Add index columns names, ie. @OrderColumns
                        m2m.join1.indexColumnNames?.forEach {
                            put(it, it.toCamelCase())
                        }
                        if (m2m.join2 != null) {
                            m2m.join2!!.indexColumnNames?.forEach {
                                put(it, it.toCamelCase())
                            }
                        }
                    }
                })
            }

            // Generate a object_relationships element and also add necessary HasuraSpecPropValues
            // and JoinType to jsonSchemaGenerator using this m2mData
            fun addObjRel(m2mData: M2MData, jsonArrayBuilder: JsonArrayBuilder) {
                with(m2mData) {
                    jsonArrayBuilder.add(buildJsonObject {
                            put("name",  joinFieldName)
                            putJsonObject("using") {
                                put("foreign_key_constraint_on", relatedColumnName)
                            }
                        }
                    )

                    val rootFieldNames = generateRootFieldNames(rootFields, entityName, entityNameLower, tableName)
                    val classMetadata = metaModel.entityPersister(m2m.entity.javaType.typeName) as AbstractEntityPersister

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
                            fromAccessorType = m2m.entity.name,
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
            }

            putJsonArray("object_relationships") {
                // Add for field1 (note: we reuse m2mData use for common partts as well)
                addObjRel(m2mData, this)
                val joinFieldName1 = m2mData.joinFieldName
                // Add for field2
                if (m2m.field2 != null) {
                    val m2mData = m2m.toM2MData(m2m.field2!!, m2m.join2!!)
                    if (m2mData.joinFieldName == joinFieldName1) {
                        LOG.warn("Many-to-many table '${m2mData.tableName}' recursively joins the same table (${m2mData.relatedTableName}). Consider using @HasuraAlias(..., joinFieldAlias=\"\") on both ends of the relationship on fields: '${m2m.field1}' and '${m2m.field2}'" )
                        m2mData.joinFieldName += "2"
                    }
                    addObjRel(m2mData, this)
                }
            }
        }
        return tableJson
    }

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
                    val componentType = type
                    val tup = type.componentTuplizer
                    val columnNames = classMetadata.getPropertyColumnNames(propName)
                    columnNames.forEachIndexed { index, columnName ->
                        val field = tup.getGetter(index).getMember()
                        if (field is Field) {
                            // Reset propName to the embedded field name
                            finalPropName = field.name
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
            val added = mutableSetOf<String>()
            processProperties(relatedEntities) { params ->
                if (added.contains(params.propName)) {
                    return@processProperties
                }
                added.add(params.propName)
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
            val added = mutableSetOf<String>()
            processProperties(relatedEntities) { params ->
                if (added.contains(params.propName)) {
                    return@processProperties
                }
                added.add(params.propName)
                if (params.columnType.isCollectionType) {
                    val collType = params.columnType as CollectionType
                    val join = collType.getAssociatedJoinable(sessionFactoryImpl as SessionFactoryImpl?)
                    val keyColumn = join.keyColumnNames[0]
                    extraTableNames.add(join.tableName)

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
                            // If there's already a ManyToManyEntity for this table, then we just found the
                            // other end of the join, add this as join2
                            val existing = manyToManyEntities.get(join.tableName)
                            if (existing != null) {
                                existing.join2 = join
                                existing.field2 = params.field
                            }
                            else {
                                manyToManyEntities.put(join.tableName, ManyToManyEntity(params.entity, join, null, params.field, null))

                            }
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
    }

    private fun graphqlTypeFor(columnType: Type, classMetadata: AbstractEntityPersister): String {
        val sqlType = columnType.sqlTypes(sessionFactoryImpl as SessionFactoryImpl)[0];
        val keyTypeName = classMetadata.factory.jdbcServices.dialect.getTypeName(sqlType)
        if (postgresqlNames[keyTypeName] == null) {
            throw Error("No postgresql alias found for type $keyTypeName. Graphql type cannot be calculated.")
        }
        return postgresqlNames[keyTypeName]!!
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

    private fun loadIntoHasura(json: String) {
        val client = WebClient
                .builder()
                .baseUrl(hasuraEndpoint)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Hasura-Admin-Secret", hasuraAdminSecret)
                .build()
        val request = client.post()
                .body<String, Mono<String>>(Mono.just(json), String::class.java)
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

    private fun loadConfIntoHasura() {
        LOG.info("Executing Hasura bulk initialization JSON. This operation could be slow, consider using metadataJson instead ...")
        loadIntoHasura(confJson!!)
    }

    private fun loadMetadataIntoHasura() {
        LOG.info("Executing replace_metadata with metadata JSON.")

        val replaceMetadata = Json.encodeToString(buildJsonObject {
            // replace_metadata
            put("type", "replace_metadata")
            put("args", metadataJsonObject)
        })

        loadIntoHasura(replaceMetadata!!)
    }

    private fun loadCascadeDeleteIntoHasura() {
        LOG.info("Executing Hasura cascade delete JSON.")
        loadIntoHasura(cascadeDeleteJson!!)
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
