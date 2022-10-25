package io.hasura.metadata.v3

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
    val metadata: HasuraMetadataV3,

    /**
     * SQL config for enum tables
     */
    val enumTableConfigs: List<EnumTableConfig>? = null,

    /**
     * SQL definitions for computed fields
     */
    val computedFieldConfigs: List<ComputedFieldConfig>? = null,

    /**
     * SQL definitions for cascaded delete field definitions
     */
    val cascadeDeleteFieldConfigs: List<CascadeDeleteFieldConfig>? = null,

    /**
     * Optional JsonSchema of the tables we generate metadata for with hasura specific extensions
     */
    val jsonSchema: String?
)

data class ComputedFieldConfig (
    val computedField: ComputedField,
    val runSql: JsonObject? = null
)

data class EnumTableConfig (
    val tableName: String,
    val runSqls: List<JsonObject>
)

data class CascadeDeleteFieldConfig (
    var table: String,
    var field: String,
    var joinedTable: String,
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
            list.addAll(enumTableConfigs.flatMap { it.runSqls })
        }
        if (computedFieldConfigs != null) {
            list.addAll(computedFieldConfigs.filter { it.runSql != null }.map { it.runSql!! })
        }
        if (cascadeDeleteFieldConfigs != null) {
            list.addAll(cascadeDeleteFieldConfigs.map { it.runSql })
        }
        return if(list.isNotEmpty()) list else null
    }

val HasuraConfiguration.bulkRunSqlJson: String
    get() = buildJsonObject {
        put("type", "bulk")
        put("args", when {
            runSqls != null -> runSqls!!.toJsonArray()
            else -> buildJsonArray{}
        })
    }.toString()

val HasuraConfiguration.cascadeDeleteJson: String
    get() = buildJsonObject {
        put("type", "bulk")
        put("args", when {
            cascadeDeleteFieldConfigs != null -> cascadeDeleteFieldConfigs.map { it.runSql }.toJsonArray()
            else -> buildJsonArray{}
        })
    }.toString()


fun HasuraConfiguration.toReplaceMetadata(): String {
    return buildJsonObject {
        put("type", "replace_metadata")
        put("args", Json.encodeToJsonElement(metadata))
    }.toString()
}


fun List<JsonObject>.toJsonArray() = buildJsonArray { forEach { add(it) } }
