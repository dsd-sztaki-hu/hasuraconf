package io.hasura.metadata.v3

import kotlinx.serialization.json.*

const val DEFAULT_SOURCE_NAME = "default"

// https://hasura.io/docs/latest/api-reference/metadata-api/manage-metadata/#metadata-replace-metadata
fun HasuraMetadataV3.toReplaceMetadata(sourceName: String? = DEFAULT_SOURCE_NAME) : JsonObject {
    return buildJsonObject {
        put("type", "replace_metadata")
        put("version", 2)
        putJsonObject("args") {
            put("allow_inconsistent_metadata", false)
            put("metadata", Json.encodeToJsonElement(this@toReplaceMetadata))
        }
    }
}

fun HasuraConfiguration.toReplaceMetadata(sourceName: String? = DEFAULT_SOURCE_NAME) : JsonObject {
    return metadata.toReplaceMetadata(sourceName)
}

// https://hasura.io/docs/latest/api-reference/schema-api/run-sql/#schema-run-sql
fun HasuraConfiguration.toRunSqls(sourceName: String? = DEFAULT_SOURCE_NAME) : JsonObject {
    return buildJsonObject {
        put("type", "run_sql")
        putJsonArray("args") {
            addJsonObject {
                put("type", "run_sql")
                putJsonObject("args") {

                }
            }
        }
    }
}


/**
 * Creates a /v1/metadata compatible "bulk" operation with various other metadata operations to effectively
 * replace the current metadata with the new definitions.
 * This implements kind of "replace_metadata" but using individual API operations bundled in a "bulk"
 * operation.
 */
fun HasuraMetadataV3.toBulkMetadataAPIOperation() : String {
    return toBulkMetadataAPIOperationJson().toString()
}

fun HasuraMetadataV3.toBulkMetadataAPIOperationJson() : JsonObject {
    return buildJsonObject {
        put("type", "bulk")
        putJsonArray("args") {
        }
    }
}

// https://hasura.io/docs/latest/api-reference/metadata- api/table-view/#metadata-pg-track-table
@OptIn(ExperimentalStdlibApi::class)
fun TableEntry.toPgTrackTable(sourceName: String? = DEFAULT_SOURCE_NAME): JsonObject {
    // https://hasura.io/docs/latest/api-reference/metadata-api/table-view/#metadata-pg-track-table
    return buildJsonObject {

    }
}

// If table is an enum then we also need a
// https://hasura.io/docs/latest/api-reference/metadata-api/table-view/#metadata-pg-set-table-is-enum
fun TableEntry.toPgSetTableIsEnum(sourceName: String? = DEFAULT_SOURCE_NAME): JsonObject? {
    return buildJsonObject {

    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/relationship/#metadata-pg-create-object-relationship
fun ObjectRelationship.toPgCreateObjectRelationship(sourceName: String? = DEFAULT_SOURCE_NAME): JsonObject? {
    return buildJsonObject {

    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/relationship/#metadata-pg-create-array-relationship
fun ArrayRelationship.toPgCreateArrayRelationship(sourceName: String? = DEFAULT_SOURCE_NAME): JsonObject? {
    return buildJsonObject {

    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/permission/#metadata-pg-create-insert-permission
fun InsertPermissionEntry.toPgCreateInsertPermission(sourceName: String? = DEFAULT_SOURCE_NAME): JsonObject? {
    return buildJsonObject {

    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/permission/#metadata-pg-create-select-permission
fun SelectPermissionEntry.toPgCreateSelectPermission(sourceName: String? = DEFAULT_SOURCE_NAME): JsonObject? {
    return buildJsonObject {

    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/permission/#metadata-pg-create-update-permission
fun UpdatePermissionEntry.toPgCreateUpdatePermission(sourceName: String? = DEFAULT_SOURCE_NAME): JsonObject? {
    return buildJsonObject {

    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/permission/#metadata-pg-create-delete-permission
fun DeletePermissionEntry.toPgCreateDeletePermission(sourceName: String? = DEFAULT_SOURCE_NAME): JsonObject? {
    return buildJsonObject {

    }
}


// https://hasura.io/docs/latest/api-reference/metadata-api/event-triggers/#metadata-pg-create-event-trigger
fun EventTrigger.toPgCreateEventTrigger(sourceName: String? = DEFAULT_SOURCE_NAME): JsonObject? {
    return buildJsonObject {

    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/computed-field/#metadata-pg-add-computed-field
fun ComputedField.toPgAddComputedField(sourceName: String? = DEFAULT_SOURCE_NAME): JsonObject? {
    return buildJsonObject {

    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/remote-relationships/#metadata-pg-create-remote-relationship
fun RemoteRelationship.toPgCreateRemoteRelationship(sourceName: String? = DEFAULT_SOURCE_NAME): JsonObject? {
    return buildJsonObject {

    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/actions/#metadata-create-action
fun Action.toCreateAction(sourceName: String? = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {  }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/actions/#metadata-create-action-permission
fun Action.toCreateActionPermission(sourceName: String? = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {  }
}


// https://hasura.io/docs/latest/api-reference/metadata-api/query-collections/#metadata-add-collection-to-allowlist
fun AllowList.toAddCollectionToAllowList(sourceName: String? = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {  }
}

//https://hasura.io/docs/latest/api-reference/metadata-api/scheduled-triggers/#metadata-create-cron-trigger
fun CronTrigger.toCreateCronTrigger(sourceName: String? = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {  }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/custom-types/#metadata-set-custom-types
fun CustomTypes.toSetCustomTypes(sourceName: String? = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {  }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/custom-functions/#metadata-pg-track-function
fun CustomFunction.toPgTrackFunction(sourceName: String? = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {  }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/query-collections/#metadata-create-query-collection
fun QueryCollectionEntry.toCreateQueryCollection(sourceName: String? = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {  }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/remote-schemas/#metadata-add-remote-schema
fun RemoteSchema.toAddRemoteSchema(sourceName: String? = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {  }
}

