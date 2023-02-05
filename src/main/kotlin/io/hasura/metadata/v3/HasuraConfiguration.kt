package io.hasura.metadata.v3

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

/**
 * All configuration data, which is needed to configure Hasura metadata and associated SQL data. For some reason
 * "is_enum" is not art of the TableEntry config, so we need a separate list for this as well
 */
data class HasuraConfiguration(

    /**
     * The metadata, which ca be directly imported into Hasura
     */
    var metadata: HasuraMetadataV3,

    /**
     * SQL config for enum tables
     */
    var enumTableConfigs: List<EnumTableConfig>? = null,

    /**
     * SQL definitions for computed fields
     */
    var computedFieldConfigs: List<ComputedFieldConfig>? = null,

    /**
     * SQL definitions for cascaded delete field definitions
     */
    var cascadeDeleteFieldConfigs: List<CascadeDeleteFieldConfig>? = null,

    /**
     * Postgresql CHECK constraints generated based on JSR validation annotations
     */
    var checkConstraintRunSqls: List<JsonObject>? = null,


    /**
     * Optional JsonSchema of the tables we generate metadata for with hasura specific extensions
     */
    var jsonSchema: String? = null
)

data class ComputedFieldConfig (
    val computedField: ComputedField,
    val runSql: JsonObject? = null
)

data class EnumTableConfig (
    val tableName: String,
    val schema: String,
    val runSqls: List<JsonObject>
)

data class CascadeDeleteFieldConfig (
    var table: String,
    val tableSchema: String,
    var field: String,
    var joinedTable: String,
    var joinedTableSchema: String,
    var runSql: JsonObject
)

/**
 * Metadata as JSON string
 */
val HasuraConfiguration.metadataJson: String
    get() = Json.encodeToString(this.metadata)

/**
 * All SQL configurations as a list of "run_sql" operation JsonObject
 */
val HasuraConfiguration.runSqls: List<JsonObject>?
    get() {
        val list = mutableListOf<JsonObject>()
        if (enumTableConfigs != null) {
            list.addAll(enumTableConfigs!!.flatMap { it.runSqls })
        }
        if (computedFieldConfigs != null) {
            list.addAll(computedFieldConfigs!!.filter { it.runSql != null }.map { it.runSql!! })
        }
        if (cascadeDeleteFieldConfigs != null) {
            list.addAll(cascadeDeleteFieldConfigs!!.map { it.runSql })
        }
        if (checkConstraintRunSqls != null) {
            list.addAll(checkConstraintRunSqls!!)
        }
        return if(list.isNotEmpty()) list else null
    }

fun HasuraConfiguration.toBulkRunSql(): JsonObject {
    return buildJsonObject {
        put("type", "bulk")
        put("args", when {
            runSqls != null -> runSqls!!.toJsonArray()
            else -> buildJsonArray {}
        })
    }
}

fun HasuraConfiguration.toCascadeDeleteJson(): JsonObject {
    return buildJsonObject {
        put("type", "bulk")
        put("args", when {
            cascadeDeleteFieldConfigs != null -> cascadeDeleteFieldConfigs!!.map { it.runSql }.toJsonArray()
            else -> buildJsonArray {}
        })
    }
}


val HasuraConfiguration.replaceMetadataJson: String
    get() = buildJsonObject {
                put("type", "replace_metadata")
                // allow_inconsistent_metadata??
                put("args", Json.encodeToString(metadata))
            }.toString()

val HasuraMetadataV3.replaceMetadataJson: String
    get() = buildJsonObject {
        put("type", "replace_metadata")
        // allow_inconsistent_metadata??
        put("args", Json.encodeToJsonElement(this@replaceMetadataJson))
    }.toString()

fun List<JsonObject>.toJsonArray() = buildJsonArray { forEach { add(it) } }

/**
 * Merge `other` metadata into
 */
fun HasuraMetadataV3.merge(other: HasuraMetadataV3) : HasuraMetadataV3 {
    TODO("Not tested")

    // Work on a clone of the receiver
    val orig = Json.decodeFromString<HasuraMetadataV3>(Json.encodeToString(this))

    other.actions?.let {
        if (orig.actions == null) {
            orig.actions = other.actions
        }
        else {
            orig.actions = buildList {
                addAll(orig.actions!!)
                addAll(other.actions!!)
            }
        }
    }

    other.allowlist?.let {
        if (orig.allowlist == null) {
            orig.allowlist = other.allowlist
        }
        else {
            orig.allowlist = buildList {
                addAll(orig.allowlist!!)
                addAll(other.allowlist!!)
            }
        }
    }

    other.cronTriggers?.let {
        if (orig.cronTriggers == null) {
            orig.cronTriggers = other.cronTriggers
        }
        else {
            orig.cronTriggers = buildList {
                addAll(orig.cronTriggers!!)
                addAll(other.cronTriggers!!)
            }
        }
    }


    other.customTypes?.let {
        if (orig.customTypes == null) {
            other.customTypes = orig.customTypes
        }
        else {
            other.customTypes!!.scalars?.let {
                if (orig.customTypes!!.scalars == null) {
                    orig.customTypes!!.scalars = other.customTypes!!.scalars
                }
                else {
                    orig.customTypes!!.scalars = buildList {
                        addAll(orig.customTypes!!.scalars!!)
                        addAll(other.customTypes!!.scalars!!)
                    }
                }
            }
            other.customTypes!!.enums?.let {
                if (orig.customTypes!!.enums == null) {
                    orig.customTypes!!.enums = other.customTypes!!.enums
                }
                else {
                    orig.customTypes!!.enums = buildList {
                        addAll(orig.customTypes!!.enums!!)
                        addAll(other.customTypes!!.enums!!)
                    }
                }
            }
            other.customTypes!!.objects?.let {
                if (orig.customTypes!!.objects == null) {
                    orig.customTypes!!.objects = other.customTypes!!.objects
                }
                else {
                    orig.customTypes!!.objects = buildList {
                        addAll(orig.customTypes!!.objects!!)
                        addAll(other.customTypes!!.objects!!)
                    }
                }
            }
            other.customTypes!!.inputObjects?.let {
                if (orig.customTypes!!.inputObjects == null) {
                    orig.customTypes!!.inputObjects = other.customTypes!!.inputObjects
                }
                else {
                    orig.customTypes!!.inputObjects = buildList {
                        addAll(orig.customTypes!!.inputObjects!!)
                        addAll(other.customTypes!!.inputObjects!!)
                    }
                }
            }
        }
    }

    other.inheritedRoles?.let {
        if (orig.inheritedRoles == null) {
            orig.inheritedRoles = other.inheritedRoles
        }
        else {
            orig.inheritedRoles = buildList {
                addAll(orig.inheritedRoles!!)
                addAll(other.inheritedRoles!!)
            }
        }
    }

    other.queryCollections?.let {
        if (orig.queryCollections == null) {
            orig.queryCollections = other.queryCollections
        }
        else {
            orig.queryCollections = buildList {
                addAll(orig.queryCollections!!)
                addAll(other.queryCollections!!)
            }
        }
    }

    other.remoteSchemas?.let {
        if (orig.remoteSchemas == null) {
            orig.remoteSchemas = other.remoteSchemas
        }
        else {
            orig.remoteSchemas = buildList {
                addAll(orig.remoteSchemas!!)
                addAll(other.remoteSchemas!!)
            }
        }
    }

    other.restEndpoints?.let {
        if (orig.restEndpoints == null) {
            orig.restEndpoints = other.restEndpoints
        }
        else {
            orig.restEndpoints = buildList {
                addAll(orig.restEndpoints!!)
                addAll(other.restEndpoints!!)
            }
        }
    }

    val origSourceNames = orig.sources.map { it.name }.toSet()
    val otherSourceNames = other.sources.map { it.name }.toSet()

    val sourceList = mutableListOf<Source>()

    // 1. add all sources that are in orig but not in other
    val origOnlyNames = origSourceNames.minus(otherSourceNames)
    orig.sources
        .filter { origOnlyNames.contains(it.name) }
        .forEach { sourceList.add(it) }

    // 2. add all sources that are in other but not in orig
    val otherOnlyNames = otherSourceNames.minus(origSourceNames)
    other.sources
        .filter { otherOnlyNames.contains(it.name) }
        .forEach { sourceList.add(it) }

    // Merge source, which appear in both
    val commonNames = origSourceNames.intersect(otherOnlyNames)
    val commonSources = orig.sources.filter { commonNames.contains(it.name) }
    commonSources.forEach {origSource ->
        val otherSource = other.sources.filter { it.name == origSource.name }.first()

        otherSource.tables?.let {
            if (origSource.tables == null) {
                origSource.tables = otherSource.tables
            }
            else {
                origSource.tables = buildList {
                    addAll(origSource.tables!!)
                    addAll(otherSource.tables!!)
                }
            }
        }

        otherSource.functions?.let {
            if (origSource.functions == null) {
                origSource.functions = otherSource.functions
            }
            else {
                origSource.functions = buildList {
                    addAll(origSource.functions!!)
                    addAll(otherSource.functions!!)
                }
            }
        }

        // Now everything is merged in origSource
        sourceList.add(origSource)
    }

    // Update sources with new sources from other and the merged sources
    orig.sources = sourceList

    return this
}

fun HasuraConfiguration.merge(other: HasuraConfiguration): HasuraConfiguration {
    TODO("Not tested")

    // Work on a clone of the receiver
    val orig = Json.decodeFromString<HasuraConfiguration>(Json.encodeToString(this))

    orig.metadata.merge(other.metadata)

    other.enumTableConfigs?.let {
        if (orig.enumTableConfigs == null) {
            orig.enumTableConfigs = other.enumTableConfigs
        }
        else {
            orig.enumTableConfigs = buildList {
                addAll(orig.enumTableConfigs!!)
                addAll(other.enumTableConfigs!!)
            }
        }
    }

    other.computedFieldConfigs?.let {
        if (orig.computedFieldConfigs == null) {
            orig.computedFieldConfigs = other.computedFieldConfigs
        }
        else {
            orig.computedFieldConfigs = buildList {
                addAll(orig.computedFieldConfigs!!)
                addAll(other.computedFieldConfigs!!)
            }
        }
    }

    other.cascadeDeleteFieldConfigs?.let {
        if (orig.cascadeDeleteFieldConfigs == null) {
            orig.cascadeDeleteFieldConfigs = other.cascadeDeleteFieldConfigs
        }
        else {
            orig.cascadeDeleteFieldConfigs = buildList {
                addAll(orig.cascadeDeleteFieldConfigs!!)
                addAll(other.cascadeDeleteFieldConfigs!!)
            }
        }
    }

    // TODD: merge jsonSchema

    return this
}


data class TablesAndFunctions(
    var functions: List<CustomFunction> = listOf<CustomFunction>(),
    var tables: List<TableEntry> = listOf<TableEntry>()
)
/**
 * Returns from teh given source the tables and functions matching the given schemaName
 */
fun HasuraMetadataV3.tablesAndFunctionsWithSchema(sourceName: String, schemaName: String) : TablesAndFunctions {

    val res = TablesAndFunctions()

    sources?.find{ it.name == sourceName }?.let {source ->
        val tablesInSchema = source.tables.filter {
            it.table.schema == schemaName
        }
        val functionsInSchema = source.functions?.filter {
            it.function is FunctionName.QualifiedFunctionValue &&
                    (it.function as FunctionName.QualifiedFunctionValue).value.schema == schemaName
        }
        if (tablesInSchema.isNotEmpty()) {
            res.tables = tablesInSchema
        }
        if (!functionsInSchema.isNullOrEmpty()) {
            res.functions = functionsInSchema
        }
    }

    return res
}
