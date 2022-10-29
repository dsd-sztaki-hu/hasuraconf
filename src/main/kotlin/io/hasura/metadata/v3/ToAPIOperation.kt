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

/**
 * Creates a /v1/metadata compatible "bulk" operation with various other metadata operations to effectively
 * replace the current metadata with the new definitions.
 * This implements kind of "replace_metadata" but using individual API operations bundled in a "bulk"
 * operation.
 */
fun HasuraMetadataV3.toBulkMetadataAPIOperationJson() : String {
    return toBulkMetadataAPIOperation().toString()
}

fun HasuraMetadataV3.toBulkMetadataAPIOperation() : JsonObject {
    val meta = this
    return buildJsonObject {
        put("type", "bulk")
        putJsonArray("args") {
            meta.sources.forEach {source ->
                add(source.toPgAddSource(true))
                // First just track all tables.
                source.tables.forEach { table ->
                    add(table.toPgTrackTable(source))
                }
                // No configure tables
                source.tables.forEach {table ->
                    // Here the order of configuring things is important. Eg. computed fields
                    // must  be added first so that permissions can be conffigured for them
                    table.isEnum?.let {
                        add(table.toPgSetTableIsEnum(source))
                    }
                    table.computedFields?.forEach { field ->
                        add(field.toPgAddComputedField(table, source))
                    }
//                    table.configuration?.let {
//                        add(table.toPgSetTableCustomization(source))
//                    }
                    table.objectRelationships?.forEach { rel->
                        add(rel.toPgCreateObjectRelationship(table, source))
                    }
                    table.arrayRelationships?.forEach { rel->
                        add(rel.toPgCreateArrayRelationship(table, source))
                    }
                    table.remoteRelationships?.forEach { rel->
                        add(rel.toPgCreateRemoteRelationship(table, source))
                    }
                    table.insertPermissions?.forEach { perm ->
                        add(perm.toPgCreateInsertPermission(table, source))
                    }
                    table.selectPermissions?.forEach { perm ->
                        add(perm.toPgCreateSelectPermission(table, source))
                    }
                    table.updatePermissions?.forEach { perm ->
                        add(perm.toPgCreateUpdatePermission(table, source))
                    }
                    table.deletePermissions?.forEach { perm ->
                        add(perm.toPgCreateDeletePermission(table, source))
                    }
                    table.eventTriggers?.forEach { field ->
                        add(field.toPgCreateEventTrigger(table, source))
                    }
                }
                source.functions?.forEach { func ->
                    add(func.toPgTrackFunction(source))
                }
            }
            meta.customTypes?.let {
                add(customTypes!!.toSetCustomTypes())
            }
            meta.actions?.forEach { action ->
                add(action.toCreateAction())
                action.toCreateActionPermissions()?.forEach { perm ->
                    add(perm)
                }
            }
        }
    }
}

fun Source.toPgAddOrUpdateSource(type: String, replaceConfiguration: Boolean = false): JsonObject {
    return buildJsonObject {
        put("type", type)

        putJsonObject("args") {
            put("configuration", Json.encodeToJsonElement(this@toPgAddOrUpdateSource.configuration))
            this@toPgAddOrUpdateSource.customization?.let {
                put("customization", Json.encodeToJsonElement(this@toPgAddOrUpdateSource.customization))
            }
            put("name", this@toPgAddOrUpdateSource.name)
            put("replace_configuration", replaceConfiguration)
        }
    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/source/#metadata-pg-add-source
fun Source.toPgAddSource(replaceConfiguration: Boolean = false) =
    toPgAddOrUpdateSource("pg_add_source", replaceConfiguration)

// https://hasura.io/docs/latest/api-reference/metadata-api/source/#metadata-pg-drop-source
fun Source.toPgDropSource(cascade: Boolean = false): JsonObject {
    return buildJsonObject {
        put("type", "pg_drop_source")

        putJsonObject("args") {
            put("name", this@toPgDropSource.name)
            put("cascade", cascade)
        }
    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/source/#metadata-rename-source
fun Source.toPgRenameSource(newName: String): JsonObject {
    return buildJsonObject {
        put("type", "pg_rename_source")

        putJsonObject("args") {
            put("name", this@toPgRenameSource.name)
            put("newName", newName)
        }
    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/source/#metadata-pg-add-source
fun Source.toPgUpdateSource(replaceConfiguration: Boolean = false) =
    toPgAddOrUpdateSource("pg_update_source", replaceConfiguration)


// https://hasura.io/docs/latest/api-reference/metadata-api/table-view/#metadata-pg-track-table
fun TableEntry.toPgTrackTable(sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    // https://hasura.io/docs/latest/api-reference/metadata-api/table-view/#metadata-pg-track-table
    val table = this
    return buildJsonObject {
        put("type", "pg_track_table")

        putJsonObject("args") {
            put("source", sourceName)
            put("table", Json.encodeToJsonElement(table.table))
            table.configuration?.let {
                put("configuration", Json.encodeToJsonElement(table.configuration))
            }
            table.apolloFederationConfig?.let {
                put("apollo_federation_config", Json.encodeToJsonElement(table.apolloFederationConfig))
            }
        }
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
            this.put("source", Json.encodeToJsonElement(sourceName))
            this.put("table", Json.encodeToJsonElement(table.table))
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
            this.put("source", Json.encodeToJsonElement(sourceName))
            this.put("table", Json.encodeToJsonElement(table.table))
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
            this.put("source", Json.encodeToJsonElement(sourceName))
            this.put("table", Json.encodeToJsonElement(table.table))
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
            this.put("source", Json.encodeToJsonElement(sourceName))
            this.put("table", Json.encodeToJsonElement(table.table))
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
            this.put("source", Json.encodeToJsonElement(sourceName))
            this.put("table", Json.encodeToJsonElement(table.table))
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
            this.put("source", Json.encodeToJsonElement(sourceName))
            this.put("table", Json.encodeToJsonElement(table.table))
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
            this.put("source", Json.encodeToJsonElement(sourceName))
            this.put("table", Json.encodeToJsonElement(table.table))
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
            this.put("source", Json.encodeToJsonElement(sourceName))
            this.put("table", Json.encodeToJsonElement(table.table))
        })
        put("args", args)
    }
}

fun ComputedField.toPgAddComputedField(table: TableEntry, source: Source) =
    toPgAddComputedField(table, source.name)

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
            this.put("source", Json.encodeToJsonElement(sourceName))
            this.put("table", Json.encodeToJsonElement(table.table))
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
fun Action.toCreateAction(): JsonObject {
    return buildJsonObject {
        put("type", "create_action")
        val action = Json.encodeToJsonElement(this@toCreateAction).jsonObject
        val args = JsonObject(action.toMutableMap().apply {
            remove("permissions") // permission handled via create_action_permission
        })
        put("args", args)

    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/actions/#metadata-drop-action
fun Action.toDropAction(clearData: Boolean = true): JsonObject {
    return buildJsonObject {
        put("type", "drop_action")
        putJsonObject("args") {
            put("name", this@toDropAction.name)
            put("clear_data", clearData)
        }
    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/actions/#metadata-update-action
fun Action.toUpdateAction(): JsonObject {
    return buildJsonObject {
        put("type", "update_action")
        val action = Json.encodeToJsonElement(this@toUpdateAction).jsonObject
        val args = JsonObject(action.toMutableMap().apply {
            remove("permissions") // permission handled via create_action_permission
            remove("comment") // TODO: comment cannot be updated according to doc
        })
        put("args", args)
    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/actions/#metadata-create-action-permission
fun Action.toCreateActionPermissions(): List<JsonObject>? {
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

fun Action.toDropActionPermissions(): List<JsonObject>? {
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

fun Action.toDropActionPermission(role: String): JsonObject {
    return buildJsonObject {
        put("type", "drop_action_permission")
        putJsonObject("args") {
            put("name", this@toDropActionPermission.name)
            put("role", role)
        }
    }
}

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
            this.put("replace", Json.encodeToJsonElement(replace))
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
fun CustomTypes.toSetCustomTypes(): JsonObject {
    return buildJsonObject {
        put("type", "set_custom_types")
        put("args", Json.encodeToJsonElement(this@toSetCustomTypes))
    }
}

// https://hasura.io/docs/latest/api-reference/metadata-api/custom-functions/#metadata-pg-track-function
fun CustomFunction.toPgTrackFunction(sourceName: String = DEFAULT_SOURCE_NAME): JsonObject {
    TODO("CustomFunction type need fields to be added from https://hasura.io/docs/latest/api-reference/syntax-defs/#function-configuration")
}

fun CustomFunction.toPgTrackFunction(source: Source) =
    toPgTrackFunction(source.name)

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
