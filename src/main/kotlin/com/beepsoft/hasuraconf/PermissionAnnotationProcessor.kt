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

    private fun createPresetFieldsMap(m2mData: HasuraConfigurator.M2MData, fieldPresets: List<HasuraFieldPresetValues>) =
        fieldPresets.map {
            if(it.column.isNotEmpty() && it.column != NOTSET) {
                if (it.column == m2mData.keyColumn) {
                    m2mData.keyColumn to it.value
                }
                else if (it.column == m2mData.relatedColumnName) {
                    m2mData.relatedColumnName to it.value
                }
                else {
                    throw HasuraConfiguratorException("Column ${it.column} doesn't exist on ${m2mData.field}'s join table. Did you mean ${m2mData.keyColumn} or ${m2mData.relatedColumnName}")
                }
            }
            else {
                if (it.field == m2mData.keyFieldName || it.field == m2mData.keyColumnAlias) {
                    m2mData.keyColumn to it.value
                }
                else if (it.field == m2mData.joinFieldName || it.field == m2mData.relatedColumnNameAlias) {
                    m2mData.relatedColumnName to it.value
                }
                else {
                    throw HasuraConfiguratorException("Field ${it.field} doesn't exist on ${m2mData.field}'s join table. " +
                            "Did you mean ${m2mData.keyFieldName}, ${m2mData.keyColumnAlias}, ${m2mData.joinFieldName} or ${m2mData.relatedColumnNameAlias} ")
                }
            }
        }.toMap()

    private fun createPresetFieldsMap(entity: EntityType<*>, classMetadata: AbstractEntityPersister, fieldPresets: List<HasuraFieldPresetValues>) =
        fieldPresets.map {
            try {
                if (it.column.length != 0 && it.column != NOTSET) {
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

    private fun calcFinalFields(fields: List<String>, excludeFields: List<String>, m2mData: HasuraConfigurator.M2MData): List<String>
    {
        // If no include/exclude given, then we will allow all fields
        if (fields.size == 0 && excludeFields.size == 0) {
            return listOf()
        }
        val propertyNames = listOf(m2mData.keyFieldName, m2mData.keyColumnAlias, m2mData.joinFieldName, m2mData.relatedColumnNameAlias)
        val finalFields = mutableSetOf<String>()
        finalFields.addAll(fields)
        if (finalFields.size == 0) {
            finalFields.addAll(propertyNames)
        }

        finalFields.removeAll(excludeFields)

        val columns = mutableListOf<String>()
        if (finalFields.contains(m2mData.keyFieldName) || finalFields.contains(m2mData.keyColumnAlias) ) {
            columns.add(m2mData.keyColumn)
        }
        if (finalFields.contains(m2mData.joinFieldName) || finalFields.contains(m2mData.relatedColumnNameAlias) ) {
            columns.add(m2mData.relatedColumnName)
        }

        return columns
    }

    /**
     * Calculates the actual list of fields to be included in the premission JSON based on the allowed {@code fields}
     * and the {@code excludeFields}
     */
    private fun calcFinalFields(fields: List<String>, excludeFields: List<String>, entity: EntityType<*>): Pair<List<String>, List<String>>
    {
        val computedFieldNames = entity.javaType.declaredFields
                                    .filter { it.isAnnotationPresent(HasuraComputedField::class.java) }
                                    .map { it.name }
        // If no include/exlude given, then we will allow all fields
        if (fields.size == 0 && excludeFields.size == 0) {
            // If nothing is specified, we have to explicitly include all the computed fields. We don't have a generic
            // "*" option in this case.
            val computedList = mutableListOf<String>()
            if (computedFieldNames != null && computedFieldNames.isNotEmpty()) {
                computedList.addAll(computedFieldNames)
            }
            return Pair(listOf(), computedList)
        }

        val classMetadata = metaModel.entityPersister(entity.javaType.typeName) as AbstractEntityPersister
        val propertyNames = mutableListOf<String>()
        propertyNames.addAll(classMetadata.propertyNames)
        propertyNames.add(classMetadata.identifierPropertyName)
        propertyNames.addAll(computedFieldNames)

        val finalFields = mutableSetOf<String>()
        finalFields.addAll(fields)
        if (finalFields.size == 0) {
            finalFields.addAll(propertyNames)
        }

        finalFields.removeAll(excludeFields)

        val columns = mutableListOf<String>()
        val computedFields = mutableListOf<String>()
        for (propertyName in finalFields) {
            // Handle @HasuraIgnoreRelationship annotation on field
            val f = Utils.findDeclaredFieldUsingReflection(entity.javaType, propertyName)
            if (f == null) {
                throw HasuraConfiguratorException("Inexistent field '$propertyName' in the permission definitions of $entity")
            }
            if (f!!.isAnnotationPresent(HasuraIgnoreRelationship::class.java)) {
                continue;
            }
            // HasuraComputedFields are added as-is
            if (f!!.isAnnotationPresent(HasuraComputedField::class.java)) {
                computedFields.add(f.name)
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
        return Pair(columns, computedFields)
    }

    private val atIncludeRegex = "[\"']\\s*@include\\((.*?)\\)\\s*[\"']".toRegex()


    private fun resolveIncludes(json: String, entity: EntityType<*>?, m2mData: HasuraConfigurator.M2MData?): String
    {
        // Does actual @include resolving recursively processing @includes in included files
        fun doResolveIncludes(json: String, jsonFile: String?, entity: EntityType<*>?, m2mData: HasuraConfigurator.M2MData?): String
        {
            var resolvedJson = json+"" // make a copy

            atIncludeRegex.findAll(json).forEach {
                val inc = it.groupValues[0]
                val file = it.groupValues[1]
                try {
                    var includedJson = HasuraConfigurator::class.java.getResource(file).readText()
                    // Recursively process includes in the included json
                    includedJson = doResolveIncludes(includedJson, file, null, null)
                    resolvedJson = resolvedJson.replace(inc, includedJson)
                }
                catch (ex: Throwable) {
                    if (entity != null) {
                        throw HasuraConfiguratorException("Unable to load include ${inc} for entity ${entity}", ex)
                    }
                    else if (m2mData != null) {
                        throw HasuraConfiguratorException("Unable to load include ${inc} for field ${m2mData.field}", ex)
                    }
                    else {
                        throw HasuraConfiguratorException("Unable to load include ${inc} in file ${jsonFile!!}", ex)
                    }
                }
            }
            return resolvedJson
        }

        return doResolveIncludes(json, null, entity, m2mData)
    }

    /**
     * Calculates final JSON. The JSON is generated based on {@code json} or if empty, based on the contents of
     * {@code jsonFile}. We support a relaxed JSON syntax (JSON5/HJSON). The input json may contain reference to
     * other files via {@code @include(/path/to/file)} this way the permission JSON can be defined modularly where
     * JSON fragments can be reused.
     */
    private fun calcJson(jsonString: String, jsonFile: String, entity: EntityType<*>?, m2mData: HasuraConfigurator.M2MData?): String
    {
        var json = jsonString.trim()
        if (json.length == 0 && jsonFile.length != 0) {
            try {
                json = HasuraConfigurator::class.java.getResource(jsonFile).readText()
            }
            catch(ex: Throwable) {
                if (entity != null) {
                    throw HasuraConfiguratorException(
                        "Unable to load from jsonFile ${jsonFile} for entity ${entity}",
                        ex
                    )
                }
                else if (m2mData != null) {
                    throw HasuraConfiguratorException(
                        "Unable to load from jsonFile ${jsonFile} for field ${m2mData.field}",
                        ex
                    )
                }
            }
        }

        if (json.trim().length == 0) {
            if (entity != null) {
                LOG.warn("Neither json nor jsonFile defined for hasura permission annotation on ${entity.javaType}")
            }
            else if (m2mData != null) {
                LOG.warn("Neither json nor jsonFile defined for hasura permission annotation on ${m2mData.field}")
            }
            return json
            //throw HasuraConfiguratorException("Neither json nor jsonFile defined for hasura permission annotation on ${entity}");
        }
        try {
            json = resolveIncludes(json, entity, m2mData)
            val configObject = Jankson
                    .builder()
                    .build()
                    .load(json)

            //This will strip comments and regularize the file, but emit newlines and indents for readability
            val processed = configObject.toJson(false, true)
            return processed
        } catch (error: SyntaxError) {
            if (entity != null) {
                throw HasuraConfiguratorException("Syntax error in hasura permission annotation on ${entity.javaType}: ${error.completeMessage}")
            }
            else if (m2mData != null) {
                throw HasuraConfiguratorException("Syntax error in hasura permission annotation on ${m2mData.field}: ${error.completeMessage}")
            }
            throw HasuraConfiguratorException("Syntax error in hasura permission: ${error.completeMessage}")
        }
    }

    private fun collectDefaultPermissionValues(annotations: Array<Annotation>) : Map<String, HasuraPermissionValues>
    {
        // Key is "operation-" or "operation-role"
        val defaultPermissions = mutableMapOf<String, HasuraPermissionValues>()

        processAnnotations(annotations) {annot ->
            if (annot.default) {
                val ops = calcEffectiveOperations(annot)
                val roles = calcEffectiveRoles(annot)
                ops.forEach { op ->
                    roles.forEach { role ->
                        defaultPermissions.put("${op}-${role}", HasuraPermissionValues.from(annot))
                    }
                }
            }
            // don't return anything, we just use all this for filtering, collectng default values
            null
        }

        return defaultPermissions
    }

    private fun calcEffectiveOperations(annot: HasuraPermission) : List<HasuraOperation>
    {
        val ops = mutableListOf<HasuraOperation>()
        if (!annot.operations.contains(HasuraOperation.NOTSET) && annot.operations.isNotEmpty()) {
            ops.addAll(annot.operations)
        }
        else {
            when (annot.operation) {
                // Just one
                HasuraOperation.SELECT,
                HasuraOperation.INSERT,
                HasuraOperation.UPDATE,
                HasuraOperation.DELETE -> ops.add(annot.operation)
                // Applicable to all of them
                HasuraOperation.ALL, HasuraOperation.NOTSET -> {
                    ops.add(HasuraOperation.SELECT)
                    ops.add(HasuraOperation.INSERT)
                    ops.add(HasuraOperation.UPDATE)
                    ops.add(HasuraOperation.DELETE)
                }
            }
        }
        return ops
    }

    private fun calcEffectiveRoles(annot: HasuraPermission) : List<String>
    {
        val roles = mutableListOf<String>()
        if (!annot.roles.contains(NOTSET)) {
            roles.addAll(annot.roles)
        }
        else if (annot.role == NOTSET){
            roles.add("") // any role
        }
        else {
            roles.add(annot.role)
        }
        return roles
    }

    fun process(entity: EntityType<*>): List<PermissionData> {
        //val permissions = mutableListOf<PermissionData>()
        val classMetadata = metaModel.entityPersister(entity.javaType.typeName) as AbstractEntityPersister
        val tableName = classMetadata.tableName

        val defaultPermissions = collectDefaultPermissionValues(entity.javaType.annotations)
        val permissions = processAnnotations(entity.javaType.annotations) { annot ->
            if (!annot.default) {
                // A single annotations my cause generation of multiple permission definitions per operation and
                // per role, so first calc all the ops and roles and permutate permission defnitions based on this
                val ops = calcEffectiveOperations(annot)
                val roles = calcEffectiveRoles(annot)
                val res = mutableListOf<PermissionData>()
                ops.forEach { op ->
                    roles.forEach { role ->
                        val permVal = HasuraPermissionValues.from(annot)
                        permVal.operation = op
                        permVal.role = role
                        res.add(createPermissionData(
                            entity,
                            tableName,
                            classMetadata,
                            permVal,
                            defaultPermissions
                        ))
                    }
                }
                res
            }
            else {
                null
            }
        }

        return permissions
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
        permissionValues: HasuraPermissionValues,
        defaults: Map<String,HasuraPermissionValues>? = null,
    ): PermissionData
    {
        // Calc the actual permissionValues from default HasuraPermission values and the actual HasuraPermission
        var defOp : HasuraPermissionValues? = null
        var defOpRole : HasuraPermissionValues? = null
        if (defaults != null) {
            defOp = defaults!!.get("${permissionValues.operation}-")
            defOpRole = defaults!!.get("${permissionValues.operation}-${permissionValues.role}")
        }
        // Inherit values from the generic operation based default, then the operation+role based, then from the
        // actual annotation
        val finalValues = mergePermissionValues(
            defOp,
            defOpRole,
            permissionValues)

        val finalFields = calcFinalFields(
            finalValues.fields.filter { it != NOTSET },
            finalValues.excludeFields.filter { it != NOTSET },
            entity)

        return PermissionData(
            finalValues,
            tableName,
            finalValues.role,
            finalValues.operation,
            calcJson(
                if (finalValues.json == NOTSET) "" else finalValues.json,
                if (finalValues.jsonFile == NOTSET) "" else finalValues.jsonFile,
                entity, null),
            finalFields.first,
            finalFields.second,
            createPresetFieldsMap(entity, classMetadata, finalValues.fieldPresets.filter {
                //Only those where not both of column and field is set to NOTSET
                !(it.column == NOTSET && it.field == NOTSET)
            }),
            !(finalValues.allowAggregations == AllowAggregationsEnum.NOTSET || finalValues.allowAggregations == AllowAggregationsEnum.FALSE)
        )
    }

    /**
     * Process permissions on a many-to-many field. Such fields have no mapped java classes, so we have to work on the
     * HasuraConfigurator.M2MData objects, which represent the many-to-many join attributes
     */
    fun process(m2mData: HasuraConfigurator.M2MData): List<PermissionData> {
        val defaultPermissions = collectDefaultPermissionValues(m2mData.field.annotations)
        val permissions = processAnnotations(m2mData.field.annotations) { annot ->
            if (!annot.default) {
                // A single annotations my cause generation of multiple permission definitions per operation and
                // per role, so first calc all the ops and roles and permutate permission defnitions based on this
                val ops = calcEffectiveOperations(annot)
                val roles = calcEffectiveRoles(annot)
                val res = mutableListOf<PermissionData>()
                ops.forEach { op ->
                    roles.forEach { role ->
                        val permVal = HasuraPermissionValues.from(annot)
                        permVal.operation = op
                        permVal.role = role
                        res.add(createPermissionData(m2mData, permVal, defaultPermissions))
                    }
                }
                res
            }
            else {
                null
            }
        }

        return permissions
    }

    private fun createPermissionData(
        m2mData: HasuraConfigurator.M2MData,
        permissionValues: HasuraPermissionValues,
        defaults: Map<String,HasuraPermissionValues>? = null,
    ): PermissionData
    {
        // Calc the actual permissionValues from default HasuraPermission values and the actual HasuraPermission
        var defOp : HasuraPermissionValues? = null
        var defOpRole : HasuraPermissionValues? = null
        if (defaults != null) {
            defOp = defaults!!.get("${permissionValues.operation}-")
            defOpRole = defaults!!.get("${permissionValues.operation}-${permissionValues.role}")
        }
        // Inherit values from the generic operation based default, then the operation+role based, then from the
        // actual annotation
        val finalValues = mergePermissionValues(
            defOp,
            defOpRole,
            permissionValues)

        val finalFields = calcFinalFields(
            finalValues.fields.filter { it != NOTSET },
            finalValues.excludeFields.filter { it != NOTSET },
            m2mData)

        return PermissionData(
            finalValues,
            m2mData.tableName,
            finalValues.role,
            finalValues.operation,
            calcJson(
                if (finalValues.json == NOTSET) "" else finalValues.json,
                if (finalValues.jsonFile == NOTSET) "" else finalValues.jsonFile,
                m2mData = m2mData,
                entity = null),
            finalFields,
            listOf(), // no calculated fields here. Should we support it via some @HasuraPermission parameter?
            createPresetFieldsMap(m2mData, finalValues.fieldPresets.filter {
                //Only those where not both of column and field is set to NOTSET
                !(it.column == NOTSET && it.field == NOTSET)
            }),
            !(finalValues.allowAggregations == AllowAggregationsEnum.NOTSET || finalValues.allowAggregations == AllowAggregationsEnum.FALSE)
        )
    }

    private fun mergePermissionValues(defOp: HasuraPermissionValues?, defOpRole: HasuraPermissionValues?, actual: HasuraPermissionValues) : HasuraPermissionValues
    {
        var result : HasuraPermissionValues? = null
        if (defOp != null) {
            result = defOp
        }
        if (defOpRole != null) {
            if (result != null) {
                result = result.merge(defOpRole)
            }
            else {
                result = defOpRole
            }
        }
        if (result != null) {
            result = result.merge(actual)
        }
        else {
            result = actual
        }
        return result
    }


    /**
     * Collects HasuraPermission annotations with `default=true` settings and orders them so that first come
     * defaults without a role specification, then default with both operation and role. Defaults will be applied to
     * actual PermissionData in this same order.
     * @param entity
     */
//    fun collectDefaultPermissions(entity: EntityType<*>): List<PermissionData> {
//        val permissions = processAnnotations(entity.javaType.annotations) {
//
//        }
//        return permissions
//    }
//
//    fun collectDefaultPermissions(m2mData: HasuraConfigurator.M2MData): List<PermissionData> {
//    }


        /**
     * Find @HasuraPermission annotations either in a @HasuraPermission annotation or on an external annotation, and
     * process these annotations using a processor function generating PermissionData.
     */
    fun processAnnotations(annotations: Array<Annotation>, processor: (HasuraPermission) ->  List<PermissionData>?) : List<PermissionData> {
        val permissions = mutableListOf<PermissionData>()
        annotations.forEach {
            // Direct @HasuraPermission
            // Note: we use isSubclassOf because the annotation maybe a proxy in which case direct
            // equality check would not work.
            if (it::class.isSubclassOf(HasuraPermission::class)) {
                val d = processor(it as HasuraPermission)
                if (d != null) {
                    permissions.addAll(d)
                }
            }
            else if (it::class.isSubclassOf(HasuraPermissions::class)) {
                (it as HasuraPermissions).value.forEach {
                    val d = processor(it)
                    if (d != null) {
                        permissions.addAll(d)
                    }
                }
            }
            else {
                // Meta @HasuraPermission
                it.annotationClass.annotations.forEach {
                    if (it::class.isSubclassOf(HasuraPermission::class)) {
                        val d = processor(it as HasuraPermission)
                        if (d != null) {
                            permissions.addAll(d)
                        }
                    }
                    else if (it::class.isSubclassOf(HasuraPermissions::class)) {
                        (it as HasuraPermissions).value.forEach {
                            val d = processor(it)
                            if (d != null) {
                                permissions.addAll(d)
                            }
                        }
                    }
                }
            }
        }
        return permissions
    }
}

data class PermissionData (
    val permissionValues: HasuraPermissionValues,
    val table: String,
    val role: String,
    val operation: HasuraOperation,
    val json: String,
    val columns: List<String>,
    val computedFields: List<String>,
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
                    if (computedFields.isNotEmpty()) {
                        putJsonArray("computed_fields") {
                            computedFields.distinct().forEach { add(it) }
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

//    fun merge(other: PermissionData) : PermissionData {
//        return PermissionData(
//            other.table,
//            other.role,
//            other.operation,
//            if (other.json != NOTSET) other.json else this.json,
//            if (other.columns.isNotEmpty()) other.columns else this.columns,
//            if (other.computedFields.isNotEmpty()) other.computedFields else this.computedFields,
//            if (other.fieldPresets.isNotEmpty()) other.fieldPresets else this.fieldPresets,
//            if (other.allowAggregations) other.allowAggregations else this.allowAggregations
//        )
//    }
}

