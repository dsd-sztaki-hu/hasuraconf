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
     * Any SQL, eg. enum values, custom functions, etc.
     */
    val runSqls: List<JsonObject>? = null,

    /**
     * Optional JsonSchema of the tables we generate metadata for with hasura specific extensions
     */
    val jsonSchema: String?
)

fun HasuraConfiguration.toMetadataJSON(): String {
    return Json.encodeToString(this.metadata)
}

fun HasuraConfiguration.toBulkRunSql(source: String = DEFAULT_SOURCE_NAME): String {
    return buildJsonObject {
        put("type", "bulk")
        put("args", when {
            runSqls != null -> runSqls!!.toJsonArray()
            else -> buildJsonArray{}
        })
    }.toString()
}

fun List<JsonObject>.toJsonArray() = buildJsonArray { forEach { add(it) } }
