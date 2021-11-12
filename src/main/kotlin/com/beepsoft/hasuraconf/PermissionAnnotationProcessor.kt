package com.beepsoft.hasuraconf

import blue.endless.jankson.Jankson
import blue.endless.jankson.impl.SyntaxError
import com.beepsoft.hasuraconf.annotation.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.hibernate.internal.SessionFactoryImpl
import org.hibernate.metamodel.spi.MetamodelImplementor
import org.hibernate.persister.entity.AbstractEntityPersister
import org.hibernate.type.CollectionType
import javax.persistence.EntityManagerFactory
import javax.persistence.metamodel.EntityType
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf


/**
 * Creates a list of {@code PermissionData} for an entity annotated with hasura related permission annotations.
 * {@link process} seached for annotations {@code @HauraPermission, @HasuraReadPermission, @HasuraInsertPermission,
 * @HasuraUpdatePermission, @HasuraDeletePermission} or any annotation that is meta annotated with these annotations.
 */
class PermissionAnnotationProcessor(entityManagerFactory: EntityManagerFactory)
{
    companion object {
        // @Suppress("JAVA_CLASS_ON_COMPANION")
        // @JvmStatic
        private val LOG = getLogger(this::class.java.enclosingClass)
    }

    private var metaModel: MetamodelImplementor

    init {
        val sessionFactoryImpl = entityManagerFactory.unwrap(SessionFactoryImpl::class.java)
        metaModel = sessionFactoryImpl.metamodel as MetamodelImplementor
    }

    /**
     * Creates a PermissionData for the given annotation. {@code permissionAnnot} is expected to be one of
     * the annotation {@code @HauraPermission, @HasuraReadPermission, @HasuraInsertPermission,
     * @HasuraUpdatePermission, @HasuraDeletePermission}. Annotation properties will be accessed via reflection,
     * so actually any annotation would be suitable, which provides the same properties as those listed above.
     */
    private fun createPermissionData(
        entity: EntityType<*>,
        tableName: String,
        classMetadata: AbstractEntityPersister,
        permissionAnnot: Annotation): PermissionData
    {
        return PermissionData(
                tableName,
                permissionAnnot.valueOf("role"),
                permissionAnnot.valueOf("operation"),
                calcJson(
                        permissionAnnot.valueOf("json"),
                        permissionAnnot.valueOf("jsonFile"),
                        entity),
                calcFinalFields(
                        permissionAnnot.valueOf("fields"),
                        permissionAnnot.valueOf("excludeFields"),
                        entity),
                createPresetFieldsMap(entity, classMetadata, permissionAnnot.valueOf("fieldPresets")),
                permissionAnnot.valueOf("allowAggregations")
        )
    }

    private fun createPresetFieldsMap(entity: EntityType<*>, classMetadata: AbstractEntityPersister, fieldPresets: HasuraFieldPresets) =
        fieldPresets.value.map {
            try {
                if (it.column.length != 0) {
                    var found = false
                    classMetadata.attributes.forEach {attr ->
                        val col = classMetadata.getPropertyColumnNames(attr.name)
                        if (col.size > 0) {
                            if (col[0] == it.column) {
                                found = true
                                return@forEach
                            }
                        }
                    }
                    if (found) {
                        it.column to it.value
                    }
                    else {
                        throw HasuraConfiguratorException("Column ${it.column} doesn't exist on ${entity.name}'s table")
                    }
                }
                else {
                    val col = classMetadata.getPropertyColumnNames(it.field)
                    if (col.size == 0) {
                        throw HasuraConfiguratorException("Field ${it.field} is not mapped into ${entity.name}'s table, so it cannot be set")
                    }
                    // Check for collection types, ie ManyToMany or OneToMany, these won't have an actual column
                    // on this table, and could not be set.
                    val propType = classMetadata.getPropertyType(it.field)
                    if (propType.isCollectionType) {
                        throw HasuraConfiguratorException("Field ${it.field} is a collection type and is not mapped into ${entity.name}'s table, so it cannot be set")
                    }
                    col[0] to it.value
                }
            }
            catch (ex: Exception) {
                if (!(ex is HasuraConfiguratorException)) {
                    throw HasuraConfiguratorException("Field ${it.field} doesn't exist on class ${entity.name}")
                }
                throw ex
            }
        }.toMap()


    private fun KClass<*>.isSubclassOf(base: Array<KClass<*>>): Boolean =
            base.firstOrNull { this.isSubclassOf(it) } != null

    /**
     * Calculates the actual list of fields to be included in the premission JSON based on the allowed {@code fields}
     * and the {@code excludeFields}
     */
    private fun calcFinalFields(fields: Array<String>, excludeFields: Array<String>, entity: EntityType<*>): List<String>
    {
        // If no include/exlude given, then we will allow all fields
        if (fields.size == 0 && excludeFields.size == 0) {
            return mutableListOf<String>()
        }

        val classMetadata = metaModel.entityPersister(entity.javaType.typeName) as AbstractEntityPersister
        val propertyNames = mutableListOf<String>()
        propertyNames.addAll(classMetadata.propertyNames)
        propertyNames.add(classMetadata.identifierPropertyName)

        val finalFields = mutableSetOf<String>()
        finalFields.addAll(fields)
        if (finalFields.size == 0) {
            finalFields.addAll(propertyNames)
        }

        finalFields.removeAll(excludeFields)

        val columns = mutableListOf<String>()
        for (propertyName in finalFields) {
            // Handle @HasuraIgnoreRelationship annotation on field
            val f = Utils.findDeclaredFieldUsingReflection(entity.javaType, propertyName)
            if (f!!.isAnnotationPresent(HasuraIgnoreRelationship::class.java)) {
                continue;
            }
            // Only handle own properties of object, ie. own fields. collection types are not owned
            if (!(classMetadata.toType(propertyName) is CollectionType)) {
                val columnName = classMetadata.getPropertyColumnNames(propertyName)[0]
                columns.add(columnName)
//            println("${propertyName} --> ${columnName}")
//            val f = Utils.findDeclaredFieldUsingReflection(entityClass, propertyName)
//            if (f!!.isAnnotationPresent(HasuraGenerateCascadeDeleteTrigger::class.java)) {
//                val fieldMetadata = metaModel.entityPersister(f.type.typeName) as AbstractEntityPersister
//                val cdf = CascadeDeleteFields(classMetadata.tableName, classMetadata.getPropertyColumnNames(propertyName)[0], fieldMetadata.tableName)
//                cascadeDeleteFields.add(cdf)
//            }
            }
        }
        return columns
    }

    private val atIncludeRegex = "[\"']\\s*@include\\((.*?)\\)\\s*[\"']".toRegex()


    private fun resolveIncludes(json: String, entity: EntityType<*>): String
    {
        // Does actual @include resolving recursively processing @includes in included files
        fun doResolveIncludes(json: String, jsonFile: String?, entity: EntityType<*>?): String
        {
            var resolvedJson = json+"" // make a copy

            atIncludeRegex.findAll(json).forEach {
                val inc = it.groupValues[0]
                val file = it.groupValues[1]
                try {
                    var includedJson = HasuraConfigurator::class.java.getResource(file).readText()
                    // Recursively process includes in the included json
                    includedJson = doResolveIncludes(includedJson, file, null)
                    resolvedJson = resolvedJson.replace(inc, includedJson)
                }
                catch (ex: Throwable) {
                    if (entity != null) {
                        throw HasuraConfiguratorException("Unable to load include ${inc} for entity ${entity}", ex)
                    }
                    else {
                        throw HasuraConfiguratorException("Unable to load include ${inc} in file ${jsonFile!!}", ex)
                    }
                }
            }
            return resolvedJson
        }

        return doResolveIncludes(json, null, entity)
    }

    /**
     * Calculates final JSON. The JSON is generated based on {@code json} or if empty, based on the contents of
     * {@code jsonFile}. We support a relaxed JSON syntax (JSON5/HJSON). The input json may contain reference to
     * other files via {@code @include(/path/to/file)} this way the permission JSON can be defined modularly where
     * JSON fragments can be reused.
     */
    private fun calcJson(jsonString: String, jsonFile: String, entity: EntityType<*>): String
    {
        var json = jsonString.trim()
        if (json.length == 0 && jsonFile.length != 0) {
            try {
                json = HasuraConfigurator::class.java.getResource(jsonFile).readText()
            }
            catch(ex: Throwable) {
                throw HasuraConfiguratorException("Unable to load from jsonFile ${jsonFile} for entity ${entity}", ex)
            }
        }

        if (json.trim().length == 0) {
            LOG.warn("Neither json nor jsonFile defined for hasura permission annotation on ${entity.javaType}")
            return json
            //throw HasuraConfiguratorException("Neither json nor jsonFile defined for hasura permission annotation on ${entity}");
        }
        try {
            json = resolveIncludes(json, entity)
            val configObject = Jankson
                    .builder()
                    .build()
                    .load(json)

            //This will strip comments and regularize the file, but emit newlines and indents for readability
            val processed = configObject.toJson(false, true)
            return processed
        } catch (error: SyntaxError) {
            throw HasuraConfiguratorException("Syntax error in hasura permission annotation on ${entity.javaType}: ${error.completeMessage}")
        }
    }

    fun process(entity: EntityType<*>): List<PermissionData> {
        val permissions = mutableListOf<PermissionData>()
        val classMetadata = metaModel.entityPersister(entity.javaType.typeName) as AbstractEntityPersister
        val tableName = classMetadata.tableName

        entity.javaType.annotations.forEach {
            // Direct @HasuraPermission
            // Note: we use isSubclassOf because the annotation maybe a proxy in which case direct
            // equality check would not work.
            if (it::class.isSubclassOf(HasuraPermission::class)) {
                permissions.add(createPermissionData(entity, tableName, classMetadata, it))
            }
            else if (it::class.isSubclassOf(HasuraPermissions::class)) {
                (it as HasuraPermissions).value.forEach {
                    permissions.add(createPermissionData(entity, tableName, classMetadata, it))
                }
            }
            else {
                // Meta @HasuraPermission
                it.annotationClass.annotations.forEach {
                    if (it::class.isSubclassOf(HasuraPermission::class)) {
                        permissions.add(createPermissionData(entity, tableName, classMetadata, it))
                    }
                    else if (it::class.isSubclassOf(HasuraPermissions::class)) {
                        (it as HasuraPermissions).value.forEach {
                            permissions.add(createPermissionData(entity, tableName, classMetadata, it))
                        }
                    }
                }
            }
        }

        return permissions
    }
}

data class PermissionData (
    val table: String,
    val role: String,
    val operation: HasuraOperation,
    val json: String,
    val columns: List<String>,
    val fieldPresets: Map<String, String>,
    val allowAggregations: Boolean,
) {
    fun toJsonObject(): JsonObject {
        return buildJsonObject {
            put("role", role)
            putJsonObject("permission") {
                fun Map<String, String>.toFieldPresetsJson()
                {
                    putJsonObject("set") {
                        fieldPresets.forEach() {
                            put(it.key, it.value)
                        }
                    }
                }

                // No columns for delete
                if (operation != HasuraOperation.DELETE) {
                    if (columns.isEmpty()) {
                        put("columns", "*")
                    } else {
                        putJsonArray("columns") {
                            columns.distinct().forEach { add(it) }
                        }
                    }
                }

                if (operation == HasuraOperation.SELECT) {
                    put("allow_aggregations", allowAggregations)
                }

                if (operation == HasuraOperation.UPDATE) {
                    put("check", JsonNull)
                    fieldPresets?.let { fieldPresets.toFieldPresetsJson() }
                }
                if (operation == HasuraOperation.INSERT) {
                    if (json.isNotEmpty()) {
                        put("check", Json.parseToJsonElement(json))
                    } else {
                        put("check", Json.parseToJsonElement("{}"))
                    }
                    fieldPresets?.let { fieldPresets.toFieldPresetsJson() }
                }
                else {
                    if (json.isNotEmpty()) {
                        put("filter", Json.parseToJsonElement(json))
                    } else {
                        put("filter", Json.parseToJsonElement("{}"))
                    }
                }
            }
        }
    }

    fun toHasuraApiJson(schema: String = "public") : String {
        val jsonObj = buildJsonObject {
            put("type", "create_${operation.name.toLowerCase()}_permission")
            putJsonObject("args") {
                putJsonObject("table") {
                    put("name", table)
                    put("schema", schema)
                }
                toJsonObject().forEach { k, v ->
                    put(k, v)
                }
            }

        }
        return Json.encodeToString(jsonObj)
    }
}

