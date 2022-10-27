/**
 * Converts HasuraMetadataV3 objects to Metadata API operations
 */
package io.hasura.metadata.v3

import kotlinx.serialization.json.*

const val DEFAULT_SOURCE_NAME = "default"

// https://hasura.io/docs/latest/api-reference/metadata-api/manage-metadata/#metadata-replace-metadata
fun HasuraMetadataV3.toReplaceMetadata(allowInconsistent: Boolean = false) : JsonObject {
    return buildJsonObject {
        put("type", "replace_metadata")
        put("version", 2)
        putJsonObject("args") {
            put("allow_inconsistent_metadata", allowInconsistent)
            put("metadata", Json.encodeToJsonElement(this@toReplaceMetadata))
        }
    }
}

fun HasuraConfiguration.toReplaceMetadata() : JsonObject {
    return metadata.toReplaceMetadata()
}

// https://hasura.io/docs/latest/api-reference/schema-api/run-sql/#schema-run-sql
fun HasuraConfiguration.toRunSqls() : JsonObject {
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
fun TableEntry.toPgTrackTable(sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    // https://hasura.io/docs/latest/api-reference/metadata-api/table-view/#metadata-pg-track-table
    return buildJsonObject {
        put("type", "pg_track_table")

        val tableEntry = Json.encodeToJsonElement(this@toPgTrackTable).jsonObject
        // When TableEntry is used in pg_track_table is_enum needs to be removed and source must be added
        // is_enum must be set via pg_set_table_is_enum
        val args = JsonObject(tableEntry.toMutableMap().apply {
            put("source", sourceName)
            remove("is_enum")
        })
        put("args", args)
    }
}

fun TableEntry.toPgTrackTable(source: Source) = toPgTrackTable(source.name)


fun TableEntry.toPgUntrackTable(sourceName: String = DEFAULT_SOURCE_NAME, cascade: Boolean? = true): JsonObject {
    // https://hasura.io/docs/latest/api-reference/metadata-api/table-view/#metadata-pg-untrack-table
    return buildJsonObject {
        put("type", "pg_untrack_table")
        putJsonObject("args") {
            put("source", sourceName)
            put("cascade", cascade)
            put("table", Json.encodeToJsonElement(table))
        }
    }
}

fun TableEntry.toPgUntrackTable(source: Source, cascade: Boolean? = true) = toPgUntrackTable(source.name, cascade)



// If table is an enum then we also need a
// https://hasura.io/docs/latest/api-reference/metadata-api/table-view/#metadata-pg-set-table-is-enum
fun TableEntry.toPgSetTableIsEnum(sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_set_table_is_enum")
        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table))
            if (this@toPgSetTableIsEnum.isEnum != null && this@toPgSetTableIsEnum.isEnum == true) {
                put("is_enum", true)
            }
            else {
                put("is_enum", false)
            }
        }
    }
}

fun TableEntry.toPgSetTableIsEnum(source: Source) = toPgSetTableIsEnum(source.name)


// https://hasura.io/docs/latest/api-reference/metadata-api/table-view/#metadata-pg-untrack-table
fun TableEntry.toPgSetTableCustomization(sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_set_table_customization")
        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table))
            if (configuration != null) {
                put("configuration", Json.encodeToJsonElement(configuration))
            }
        }
    }
}

fun TableEntry.toPgSetTableCustomization(source: Source) = toPgSetTableCustomization(source.name)


// https://hasura.io/docs/latest/api-reference/metadata-api/table-view/#metadata-pg-set-apollo-federation-config
fun TableEntry.toPgSetApolloFederationConfig(sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_set_apollo_federation_config")
        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table))
            if (this@toPgSetApolloFederationConfig.apolloFederationConfig != null) {
                put("apollo_federation_config", Json.encodeToJsonElement(apolloFederationConfig))
            }
        }
    }
}

fun TableEntry.toPgSetApolloFederationConfig(source: Source) = toPgSetApolloFederationConfig(source.name)



// https://hasura.io/docs/latest/api-reference/metadata-api/relationship/#metadata-pg-create-object-relationship
fun ObjectRelationship.toPgCreateObjectRelationship(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_create_object_relationship")
        val rel = Json.encodeToJsonElement(this@toPgCreateObjectRelationship).jsonObject
        val args = JsonObject(rel.toMutableMap().apply {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
        })
        put("args", args)
    }
}

fun ObjectRelationship.toPgCreateObjectRelationship(table: TableEntry, source: Source) =
    toPgCreateObjectRelationship(table, source.name)

//https://hasura.io/docs/latest/api-reference/metadata-api/relationship/#metadata-pg-drop-relationship
fun ObjectRelationship.toPgDropRelationship(
    table: TableEntry,
    sourceName: String = DEFAULT_SOURCE_NAME,
    cascade: Boolean = false): JsonObject
{
    return buildJsonObject {
        put("type", "pg_drop_relationship")
        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
            put("relationship", this@toPgDropRelationship.name)
            put("cascade", cascade)
        }
    }
}

fun ObjectRelationship.toPgDropRelationship(table: TableEntry, source: Source, cascade: Boolean = false) =
    toPgDropRelationship(table, source.name, cascade)

//https://hasura.io/docs/latest/api-reference/metadata-api/relationship/#metadata-pg-set-relationship-comment
fun ObjectRelationship.toPgSetRelationshipComment(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject
{
    return buildJsonObject {
        put("type", "pg_set_relationship_comment")
        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
            this@toPgSetRelationshipComment.comment?.let {
                put("comment", this@toPgSetRelationshipComment.comment)
            }
        }
    }
}

fun ObjectRelationship.toPgSetRelationshipComment(table: TableEntry, source: Source) =
    toPgSetRelationshipComment(table, source.name)


// https://hasura.io/docs/latest/api-reference/metadata-api/relationship/#metadata-pg-rename-relationship
fun ObjectRelationship.toPgRenameRelationship(table: TableEntry, newName: String, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject
{
    return buildJsonObject {
        put("type", "pg_rename_relationship")
        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
            put("name", this@toPgRenameRelationship.name)
            put("new_name", newName)
        }
    }
}

fun ObjectRelationship.toPgRenameRelationship(table: TableEntry, newName: String, source: Source) =
    toPgRenameRelationship(table, newName, source.name)

// https://hasura.io/docs/latest/api-reference/metadata-api/relationship/#metadata-pg-create-array-relationship
fun ArrayRelationship.toPgCreateArrayRelationship(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_create_array_relationship")
        val rel = Json.encodeToJsonElement(this@toPgCreateArrayRelationship).jsonObject
        val args = JsonObject(rel.toMutableMap().apply {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
        })
        put("args", args)
    }
}

fun ArrayRelationship.toPgCreateArrayRelationship(table: TableEntry, source: Source) =
    toPgCreateArrayRelationship(table, source.name)

//https://hasura.io/docs/latest/api-reference/metadata-api/relationship/#metadata-pg-drop-relationship
fun ArrayRelationship.toPgDropRelationship(
    table: TableEntry,
    sourceName: String = DEFAULT_SOURCE_NAME,
    cascade: Boolean = false): JsonObject
{
    return buildJsonObject {
        put("type", "pg_drop_relationship")
        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
            put("relationship", this@toPgDropRelationship.name)
            put("cascade", cascade)
        }
    }
}

fun ArrayRelationship.toPgDropRelationship(table: TableEntry, source: Source, cascade: Boolean = false) =
    toPgDropRelationship(table, source.name, cascade)

//https://hasura.io/docs/latest/api-reference/metadata-api/relationship/#metadata-pg-set-relationship-comment
fun ArrayRelationship.toPgSetRelationshipComment(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject
{
    return buildJsonObject {
        put("type", "pg_set_relationship_comment")
        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
            this@toPgSetRelationshipComment.comment?.let {
                put("comment", this@toPgSetRelationshipComment.comment)
            }
        }
    }
}

fun ArrayRelationship.toPgSetRelationshipComment(table: TableEntry, source: Source) =
    toPgSetRelationshipComment(table, source.name)

// https://hasura.io/docs/latest/api-reference/metadata-api/relationship/#metadata-pg-rename-relationship
fun ArrayRelationship.toPgRenameRelationship(table: TableEntry, newName: String, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject
{
    return buildJsonObject {
        put("type", "pg_rename_relationship")
        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
            put("name", this@toPgRenameRelationship.name)
            put("new_name", newName)
        }
    }
}

fun ArrayRelationship.toPgRenameRelationship(table: TableEntry, newName: String, source: Source) =
    toPgRenameRelationship(table, newName, source.name)

// https://hasura.io/docs/latest/api-reference/metadata-api/permission/#metadata-pg-create-insert-permission
fun InsertPermissionEntry.toPgCreateInsertPermission(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_create_insert_permission")
        val perm = Json.encodeToJsonElement(this@toPgCreateInsertPermission).jsonObject
        val args = JsonObject(perm.toMutableMap().apply {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
        })
        put("args", args)
    }
}

fun InsertPermissionEntry.toPgCreateInsertPermission(table: TableEntry, source: Source) =
    toPgCreateInsertPermission(table, source.name)

// https://hasura.io/docs/latest/api-reference/metadata-api/permission/#metadata-pg-drop-insert-permission
fun InsertPermissionEntry.toPgDropInsertPermission(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_drop_insert_permission")
        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
            put("role", this@toPgDropInsertPermission.role)
        }
    }
}

fun InsertPermissionEntry.toPgDropInsertPermission(table: TableEntry, source: Source) =
    toPgCreateInsertPermission(table, source.name)

// https://hasura.io/docs/latest/api-reference/metadata-api/permission/#metadata-pg-set-permission-comment
fun InsertPermissionEntry.toPgSetPermissionComment(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_set_permission_comment")
        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
            put("comment", this@toPgSetPermissionComment.comment)
        }
    }
}

fun InsertPermissionEntry.toPgSetPermissionComment(table: TableEntry, source: Source) =
    toPgSetPermissionComment(table, source.name)

// https://hasura.io/docs/latest/api-reference/metadata-api/permission/#metadata-pg-create-select-permission
fun SelectPermissionEntry.toPgCreateSelectPermission(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_create_select_permission")
        val perm = Json.encodeToJsonElement(this@toPgCreateSelectPermission).jsonObject
        val args = JsonObject(perm.toMutableMap().apply {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
        })
        put("args", args)
    }
}

fun SelectPermissionEntry.toPgCreateSelectPermission(table: TableEntry, source: Source) =
    toPgCreateSelectPermission(table, source.name)

// https://hasura.io/docs/latest/api-reference/metadata-api/permission/#metadata-pg-drop-select-permission
fun SelectPermissionEntry.toPgDropSelectPermission(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_drop_select_permission")
        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
            put("role", this@toPgDropSelectPermission.role)
        }
    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/permission/#metadata-pg-set-permission-comment
fun SelectPermissionEntry.toPgSetPermissionComment(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_set_permission_comment")
        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
            put("comment", this@toPgSetPermissionComment.comment)
        }
    }
}

fun SelectPermissionEntry.toPgSetPermissionComment(table: TableEntry, source: Source) =
    toPgSetPermissionComment(table, source.name)


// https://hasura.io/docs/latest/api-reference/metadata-api/permission/#metadata-pg-create-update-permission
fun UpdatePermissionEntry.toPgCreateUpdatePermission(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_create_update_permission")
        val perm = Json.encodeToJsonElement(this@toPgCreateUpdatePermission).jsonObject
        val args = JsonObject(perm.toMutableMap().apply {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
        })
        put("args", args)
    }
}

fun UpdatePermissionEntry.toPgCreateUpdatePermission(table: TableEntry, source: Source) =
    toPgCreateUpdatePermission(table, source.name)

// https://hasura.io/docs/latest/api-reference/metadata-api/permission/#metadata-pg-drop-update-permission
fun UpdatePermissionEntry.toPgDropUpdatePermission(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_drop_update_permission")
        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
            put("role", this@toPgDropUpdatePermission.role)
        }
    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/permission/#metadata-pg-set-permission-comment
fun UpdatePermissionEntry.toPgSetPermissionComment(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_set_permission_comment")
        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
            put("comment", this@toPgSetPermissionComment.comment)
        }
    }
}

fun UpdatePermissionEntry.toPgSetPermissionComment(table: TableEntry, source: Source) =
    toPgSetPermissionComment(table, source.name)

// https://hasura.io/docs/latest/api-reference/metadata-api/permission/#metadata-pg-create-delete-permission
fun DeletePermissionEntry.toPgCreateDeletePermission(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_create_delete_permission")
        val perm = Json.encodeToJsonElement(this@toPgCreateDeletePermission).jsonObject
        val args = JsonObject(perm.toMutableMap().apply {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
        })
        put("args", args)
    }
}

fun DeletePermissionEntry.toPgCreateDeletePermission(table: TableEntry, source: Source) =
    toPgCreateDeletePermission(table, source.name)

// https://hasura.io/docs/latest/api-reference/metadata-api/permission/#metadata-pg-drop-delete-permission
fun DeletePermissionEntry.toPgDropDeletePermission(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_drop_delete_permission")
        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
            put("role", this@toPgDropDeletePermission.role)
        }
    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/permission/#metadata-pg-set-permission-comment
fun DeletePermissionEntry.toPgSetPermissionComment(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_set_permission_comment")
        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
            put("comment", this@toPgSetPermissionComment.comment)
        }
    }
}

fun DeletePermissionEntry.toPgSetPermissionComment(table: TableEntry, source: Source) =
    toPgSetPermissionComment(table, source.name)



// https://hasura.io/docs/latest/api-reference/metadata-api/event-triggers/#metadata-pg-create-event-trigger
fun EventTrigger.toPgCreateEventTrigger(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_create_event_trigger")

        val t = Json.encodeToJsonElement(this@toPgCreateEventTrigger).jsonObject
        val defMap = t.toMutableMap().get("definition")!!.jsonObject.toMutableMap()
        // Move all fields from "definition" up to the event trigger data + source and table
        val args = JsonObject(t.toMutableMap().apply {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
            remove("definition")
            putAll(defMap)
        })
        put("args", args)
    }
}

fun EventTrigger.toPgCreateEventTrigger(table: TableEntry, source: Source) =
    toPgCreateEventTrigger(table, source.name)


// https://hasura.io/docs/latest/api-reference/metadata-api/event-triggers/#metadata-pg-delete-event-trigger
fun EventTrigger.toPgDeleteEventTrigger(name: String, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_delete_event_trigger")
        putJsonObject("args") {
            put("source", sourceName)
            put("name", name)
        }
    }
}

fun EventTrigger.toPgDeleteEventTrigger(name: String, source: Source) =
    toPgDeleteEventTrigger(name, source.name)


// https://hasura.io/docs/latest/api-reference/metadata-api/computed-field/#metadata-pg-add-computed-field
fun ComputedField.toPgAddComputedField(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_add_computed_field")

        val computedField = Json.encodeToJsonElement(this@toPgAddComputedField).jsonObject
        val args = JsonObject(computedField.toMutableMap().apply {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
        })
        put("args", args)
    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/computed-field/#metadata-pg-drop-computed-field
fun ComputedField.toPgDropComputedField(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME, cascade: Boolean = false): JsonObject {
    return buildJsonObject {
        put("type", "pg_drop_computed_field")
        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
            put("name", this@toPgDropComputedField.name)
        }
    }
}

fun ComputedField.toPgDropComputedField(table: TableEntry, source: Source, cascade: Boolean = false) =
    toPgDropComputedField(table, source.name, cascade)


// https://hasura.io/docs/latest/api-reference/metadata-api/remote-relationships/#metadata-pg-create-remote-relationship
private fun RemoteRelationship.toPgCreateOrUpdateRemoteRelationship(type: String, table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", type)
        val rel = Json.encodeToJsonElement(this@toPgCreateOrUpdateRemoteRelationship).jsonObject
        val args = JsonObject(rel.toMutableMap().apply {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
        })
        put("args", args)
    }
}

fun RemoteRelationship.toPgCreateRemoteRelationship(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return toPgCreateOrUpdateRemoteRelationship("pg_create_remote_relationship", table, sourceName)
}

fun RemoteRelationship.toPgCreateRemoteRelationship(table: TableEntry, source: Source) =
    toPgCreateRemoteRelationship(table, source.name)

fun RemoteRelationship.toPgUpdateRemoteRelationship(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return toPgCreateOrUpdateRemoteRelationship("pg_update_remote_relationship", table, sourceName)
}

fun RemoteRelationship.toPgUpdateRemoteRelationship(table: TableEntry, source: Source) =
    toPgCreateRemoteRelationship(table, source.name)

private fun RemoteRelationship.toPgDeleteRemoteRelationship(table: TableEntry, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "pg_delete_remote_relationship")
        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
            put("name", this@toPgDeleteRemoteRelationship.name)
        }
    }
}

fun RemoteRelationship.toPgDeleteRemoteRelationship(table: TableEntry, source: Source) =
    toPgDeleteRemoteRelationship(table, source.name)


// https://hasura.io/docs/latest/api-reference/metadata-api/actions/#metadata-create-action
fun Action.toCreateAction(sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "create_action")
        val action = Json.encodeToJsonElement(this@toCreateAction).jsonObject
        val args = JsonObject(action.toMutableMap().apply {
            remove("permissions") // permission handled via create_action_permission
            put("source", sourceName)
        })
        put("args", args)

    }
}

fun Action.toCreateAction(source: Source) =
    toCreateAction(source.name)

// https://hasura.io/docs/latest/api-reference/metadata-api/actions/#metadata-drop-action
fun Action.toDropAction(sourceName: String = DEFAULT_SOURCE_NAME, clearData: Boolean = true): JsonObject {
    return buildJsonObject {
        put("type", "drop_action")
        putJsonObject("args") {
            put("name", this@toDropAction.name)
            put("clear_data", clearData)
        }
    }
}

fun Action.toDropAction(source: Source, clearData: Boolean = true) =
    toDropAction(source.name, clearData)

// https://hasura.io/docs/latest/api-reference/metadata-api/actions/#metadata-update-action
fun Action.toUpdateAction(sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "update_action")
        val action = Json.encodeToJsonElement(this@toUpdateAction).jsonObject
        val args = JsonObject(action.toMutableMap().apply {
            remove("permissions") // permission handled via create_action_permission
            remove("comment") // TODO: comment cannot be updated according to doc
            put("source", sourceName)
        })
        put("args", args)
    }
}

fun Action.toUpdateAction(source: Source) =
    toUpdateAction(source.name)

// https://hasura.io/docs/latest/api-reference/metadata-api/actions/#metadata-create-action-permission
fun Action.toCreateActionPermissions(sourceName: String = DEFAULT_SOURCE_NAME): List<JsonObject>? {
    return permissions?.map {
        buildJsonObject {
            put("type", "create_action_permission")
            putJsonObject("args") {
                put("name", this@toCreateActionPermissions.name)
                put("role", it.role)
            }
        }
    }
}

fun Action.toCreateActionPermissions(source: Source) =
    toCreateActionPermissions(source.name)

fun Action.toDropActionPermissions(sourceName: String = DEFAULT_SOURCE_NAME): List<JsonObject>? {
    return permissions?.map {
        buildJsonObject {
            put("type", "drop_action_permission")
            putJsonObject("args") {
                put("name", this@toDropActionPermissions.name)
                put("role", it.role)
            }
        }
    }
}

fun Action.toDropActionPermissions(source: Source) =
    toDropActionPermissions(source.name)

fun Action.toDropActionPermission(role: String, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "drop_action_permission")
        putJsonObject("args") {
            put("name", this@toDropActionPermission.name)
            put("role", role)
        }
    }
}

fun Action.toDropActionPermission(role: String, source: Source) =
    toDropActionPermission(role, source.name)

// https://hasura.io/docs/latest/api-reference/metadata-api/query-collections/#metadata-add-collection-to-allowlist
fun AllowList.toAddCollectionToAllowList(sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    TODO()
}

//https://hasura.io/docs/latest/api-reference/metadata-api/scheduled-triggers/#metadata-create-cron-trigger
fun CronTrigger.toCreateCronTrigger(sourceName: String = DEFAULT_SOURCE_NAME, replace: Boolean = false): JsonObject {
    return buildJsonObject {
        put("type", "create_cron_trigger")
        val cron = Json.encodeToJsonElement(this@toCreateCronTrigger).jsonObject
        val args = JsonObject(cron.toMutableMap().apply {
            put("replace", replace)
        })
        put("args", args)
    }
}

fun CronTrigger.toCreateCronTrigger(source: Source, replace: Boolean = false) =
    toCreateCronTrigger(source.name, replace)

// https://hasura.io/docs/latest/api-reference/metadata-api/scheduled-triggers/#metadata-delete-cron-trigger
fun CronTrigger.toDeleteCronTrigger(sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "delete_cron_trigger")
        putJsonObject("args") {
            put(name, this@toDeleteCronTrigger.name)
        }
    }
}

fun CronTrigger.toDeleteCronTrigger(source: Source) =
    toCreateCronTrigger(source.name)



// https://hasura.io/docs/latest/api-reference/metadata-api/custom-types/#metadata-set-custom-types
fun CustomTypes.toSetCustomTypes(sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "set_custom_types")
        put("args", Json.encodeToJsonElement(this@toSetCustomTypes))
    }
}

fun CustomTypes.toSetCustomTypes(source: Source) =
    toSetCustomTypes(source.name)


// https://hasura.io/docs/latest/api-reference/metadata-api/custom-functions/#metadata-pg-track-function
fun CustomFunction.toPgTrackFunction(sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    TODO("CustomFunction type need fields to be added from https://hasura.io/docs/latest/api-reference/syntax-defs/#function-configuration")
}

// https://hasura.io/docs/latest/api-reference/metadata-api/query-collections/#metadata-create-query-collection
fun QueryCollectionEntry.toCreateQueryCollection(sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    TODO()
}

// https://hasura.io/docs/latest/api-reference/metadata-api/remote-schemas/#metadata-add-remote-schema
private fun RemoteSchema.toAddOrUpdateRemoteSchema(type: String, sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", type)
        put("args", Json.encodeToJsonElement(this@toAddOrUpdateRemoteSchema))
    }
}



fun RemoteSchema.toAddRemoteSchema(sourceName: String = DEFAULT_SOURCE_NAME) =
    toAddOrUpdateRemoteSchema("add_remote_schema", sourceName)

fun RemoteSchema.toAddRemoteSchema(source: Source) =
    toAddRemoteSchema(source.name)

fun RemoteSchema.toUpdateRemoteSchema(sourceName: String = DEFAULT_SOURCE_NAME) =
    toAddOrUpdateRemoteSchema("update_remote_schema", sourceName)


fun RemoteSchema.toUpdateRemoteSchema(source: Source) =
    toAddRemoteSchema(source.name)

// https://hasura.io/docs/latest/api-reference/metadata-api/remote-schemas/#metadata-remove-remote-schema
fun RemoteSchema.toRemoveRemoteSchema(sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "remove_remote_schema")
        putJsonObject("args") {
            put("name", name)
        }
    }
}

fun RemoteSchema.toRemoveRemoteSchema(source: Source) =
    toRemoveRemoteSchema(source.name)

// https://hasura.io/docs/latest/api-reference/metadata-api/remote-schemas/#metadata-reload-remote-schema
fun RemoteSchema.toReloadRemoteSchema(sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    return buildJsonObject {
        put("type", "reload_remote_schema")
        putJsonObject("args") {
            put("name", name)
        }
    }
}

fun RemoteSchema.toReloadRemoteSchema(source: Source) =
    toReloadRemoteSchema(source.name)

// TODO: https://hasura.io/docs/latest/api-reference/metadata-api/remote-schema-permissions/
// TODO: https://hasura.io/docs/latest/api-reference/metadata-api/custom-functions/
// TODO: https://hasura.io/docs/latest/api-reference/metadata-api/inherited-roles/
// TODO: https://hasura.io/docs/latest/api-reference/metadata-api/event-triggers/#metadata-pg-redeliver-event
// TODO: https://hasura.io/docs/latest/api-reference/metadata-api/event-triggers/#metadata-pg-invoke-event-trigger
// TODO: https://hasura.io/docs/latest/api-reference/metadata-api/remote-relationships/#metadata-create-remote-schema-remote-relationship
// TODO: https://hasura.io/docs/latest/api-reference/metadata-api/remote-relationships/#metadata-update-remote-schema-remote-relationship
// TODO: https://hasura.io/docs/latest/api-reference/metadata-api/remote-relationships/#metadata-delete-remote-schema-remote-relationship
// TODO; https://hasura.io/docs/latest/api-reference/metadata-api/scheduled-triggers/#metadata-create-scheduled-event
// TODO: https://hasura.io/docs/latest/api-reference/metadata-api/scheduled-triggers/#metadata-delete-scheduled-event
// TODO: https://hasura.io/docs/latest/api-reference/metadata-api/scheduled-triggers/#metadata-get-cron-triggers
// TODO: https://hasura.io/docs/latest/api-reference/metadata-api/restified-endpoints/
// TODO: https://hasura.io/docs/latest/api-reference/metadata-api/query-collections/
