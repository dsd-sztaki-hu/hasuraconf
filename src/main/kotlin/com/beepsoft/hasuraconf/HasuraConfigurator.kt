package com.beepsoft.hasuraconf

import com.beepsoft.hasuraconf.annotation.*
import com.google.common.net.HttpHeaders
import org.apache.commons.text.CaseUtils
import org.atteo.evo.inflector.English
import org.hibernate.SessionFactory
import org.hibernate.internal.SessionFactoryImpl
import org.hibernate.metamodel.spi.MetamodelImplementor
import org.hibernate.persister.entity.AbstractEntityPersister
import org.hibernate.type.AssociationType
import org.hibernate.type.CollectionType
import org.hibernate.type.ForeignKeyDirection
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.io.PrintWriter
import javax.persistence.EntityManagerFactory
import javax.persistence.metamodel.EntityType

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
        var hasuraAdminSecret: String?
) {

    companion object {
        // @Suppress("JAVA_CLASS_ON_COMPANION")
        // @JvmStatic
        private val LOG = getLogger(this::class.java.enclosingClass)
    }

    inner class CascadeDeleteFields(var table: String, var field: String, var joinedTable: String)

    var confJson: String? = null
        private set // the setter is private and has the default implementation

    private var sessionFactoryImpl: SessionFactory
    private var metaModel: MetamodelImplementor
    private var permissionAnnotationProcessor: PermissionAnnotationProcessor

    private lateinit var tableNames: MutableSet<String>
    private lateinit var enumTables: MutableSet<String>
    private lateinit var cascadeDeleteFields: MutableSet<CascadeDeleteFields>


    init {
        sessionFactoryImpl = entityManagerFactory.unwrap(SessionFactory::class.java) as SessionFactoryImpl
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

    /**
     * Creates hasura-conf.json containing table tracking and field/relationship name customizations
     * at bean creation time automatically.
     */
    @Throws(HasuraConfiguratorException::class)
    fun configure() {
        confJson = null
        tableNames = mutableSetOf<String>()
        enumTables = mutableSetOf<String>()
        cascadeDeleteFields = mutableSetOf<CascadeDeleteFields>()

        val entities = metaModel.entities
        // Add custom field and relationship names for each table
        val customColumNamesRelationships = StringBuilder()
        val permissions = StringBuilder()
        var added = false
        for (entity in entities) {
            val customFieldNameJSONBuilder = StringBuilder()
            if (added) {
                customColumNamesRelationships.append(",\n")
            }
            customColumNamesRelationships.append(generateEntityCustomization(entity))
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
        confJson = bulk.toString().reformatJson()

        if (confFile != null) {
            PrintWriter(confFile!!).use { out -> out.println(confJson) }
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
    private fun generateEntityCustomization(entity: EntityType<*>): String {
        val classMetadata = metaModel.entityPersister(entity.javaType.typeName) as AbstractEntityPersister
        val tableName = classMetadata.tableName
        var entityName = entity.name
        val customFieldNameJSONBuilder = StringBuilder()
        tableNames.add(tableName)
        // Remove inner $ from the name of inner classes
        entityName = entityName.replace("\\$".toRegex(), "")
        // Copy
        var entityNameLower = entityName.toString()
        entityNameLower = Character.toLowerCase(entityNameLower[0]).toString() + entityNameLower.substring(1)
        customFieldNameJSONBuilder.append(
                """
                    {
                        "type": "set_table_custom_fields",
                        "version": 2,
                        "args": {
                            "table": "${tableName}",
                            "schema": "${schemaName}",
                            "custom_root_fields": {
                                "select": "${English.plural(entityNameLower)}",
                                "select_by_pk": "${entityNameLower}",
                                "select_aggregate": "${entityNameLower}Aggregate",
                                "insert": "create${English.plural(entityName)}",
                                "update": "update${English.plural(entityName)}",
                                "delete": "delete${English.plural(entityName)}"
                            },
                            "custom_column_names": {                 
                """
        )
        val customRelationshipNameJSONBuilder = StringBuilder()
        val propNames = classMetadata.propertyNames
        var propAdded = false
        for (propName in propNames) {
            val added = addCustomFieldNameOrRef(propAdded, classMetadata, tableName, propName, customFieldNameJSONBuilder, customRelationshipNameJSONBuilder)
            if (propAdded != true && added == true) {
                propAdded = true
            }
        }
        customFieldNameJSONBuilder.append(
                "\n\t\t\t}\n"
                        + "\t\t}\n"
                        + "\t}"
        )
        // Add optional relationship renames
        if (customRelationshipNameJSONBuilder.length != 0) {
            customFieldNameJSONBuilder.append(",\n")
            customFieldNameJSONBuilder.append(customRelationshipNameJSONBuilder)
        }
        return customFieldNameJSONBuilder.toString()
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
            propAdded: Boolean,
            classMetadata: AbstractEntityPersister,
            tableName: String,
            propName: String,
            customFieldNameJSONBuilder: StringBuilder,
            customRelationshipNameJSONBuilder: StringBuilder): Boolean {
        val columnName = classMetadata.getPropertyColumnNames(propName)[0]
        val propType = classMetadata.getPropertyType(propName)
        //
        // If it is an association type, add an array or object relationship
        //
        if (propType.isAssociationType) {
            if (customRelationshipNameJSONBuilder.length != 0) {
                customRelationshipNameJSONBuilder.append(",\n")
            }
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
                customRelationshipNameJSONBuilder.append(arrayRel)
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
                    customRelationshipNameJSONBuilder.append(objectRel)
                    // Also add customization for the ID field name
                    val camelCasedIdName = CaseUtils.toCamelCase(columnName, false, '_')
                    if (propAdded) {
                        customFieldNameJSONBuilder.append(",\n")
                    }
                    customFieldNameJSONBuilder.append("\t\t\t\t\"$columnName\": \"$camelCasedIdName\"")
                    return true
                } else { // TO_PARENT, ie. assocition is mapped by the other side
                    val join = assocType.getAssociatedJoinable(sessionFactoryImpl as SessionFactoryImpl?)
//                    val keyColumn = join.keyColumnNames[0]
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
                                                    "id": "${tableName}_id"
                                                }
                                            }
                                        }
                                    }
                                }                                
                            """
//                            "{\n" +
//                            "\t\"type\": \"create_object_relationship\",\n" +
//                            "\t\"args\": {\n" +
//                            "\t\t\"name\": \"" + propName + "\",\n" +
//                            "\t\t\"table\": {\n" +
//                            "\t\t\t\"name\": \"" + tableName + "\",\n" +
//                            "\t\t\t\"schema\": \"" + schemaName + "\"\n" +
//                            "\t\t},\n" +
//                            "\t\t\"using\": {\n" +
//                            "\t\t\t\"manual_configuration\": {\n" +
//                            "\t\t\t\t\"remote_table\": {\n" +
//                            "\t\t\t\t\t\"name\": \"" + join.tableName + "\",\n" +
//                            "\t\t\t\t\t\"schema\": \"" + schemaName + "\"\n" +
//                            "\t\t\t\t},\n" +
//                            "\t\t\t\t\"column_mapping\": {\n" +
//                            // This worked in case of array relations, but here it is always 'id' ...
//                            //"\t\t\t\t\t\"id\": \""+keyColumn+"\"\n" +
//                            // So instead this is rather hackish and assumes that table names + "_id"
//                            // is the mapping side's value
//                            "\t\t\t\t\t\"id\": \"" + tableName + "_id\"\n" +
//                            "\t\t\t\t}\n" +
//                            "\t\t\t}\n" +
//                            "\t\t}\n" +
//                            "\t}\n" +
//                            "}"
                    customRelationshipNameJSONBuilder.append(objectRel)
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
