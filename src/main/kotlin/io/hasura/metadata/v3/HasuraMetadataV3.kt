/**
 * All val-s changed to var-s in data classes so that it is easier to mutate them
 */


// To parse the JSON, install kotlin's serialization plugin and do:
//
// val json                         = Json(JsonConfiguration.Stable)
// val pGColumn                     = json.parse(PGColumn.serializer(), jsonString)
// val computedFieldName            = json.parse(ComputedFieldName.serializer(), jsonString)
// val roleName                     = json.parse(RoleName.serializer(), jsonString)
// val triggerName                  = json.parse(TriggerName.serializer(), jsonString)
// val remoteRelationshipName       = json.parse(RemoteRelationshipName.serializer(), jsonString)
// val remoteSchemaName             = json.parse(RemoteSchemaName.serializer(), jsonString)
// val collectionName               = json.parse(CollectionName.serializer(), jsonString)
// val graphQLName                  = json.parse(GraphQLName.serializer(), jsonString)
// val graphQLType                  = json.parse(GraphQLType.serializer(), jsonString)
// val relationshipName             = json.parse(RelationshipName.serializer(), jsonString)
// val actionName                   = json.parse(ActionName.serializer(), jsonString)
// val webhookURL                   = json.parse(WebhookURL.serializer(), jsonString)
// val tableName                    = json.parse(TableName.serializer(), jsonString)
// val qualifiedTable               = json.parse(QualifiedTable.serializer(), jsonString)
// val tableConfig                  = json.parse(TableConfig.serializer(), jsonString)
// val tableEntry                   = json.parse(TableEntry.serializer(), jsonString)
// val customRootFields             = json.parse(CustomRootFields.serializer(), jsonString)
// val customColumnNames            = json.parse(CustomColumnNames.serializer(), jsonString)
// val functionName                 = json.parse(FunctionName.serializer(), jsonString)
// val qualifiedFunction            = json.parse(QualifiedFunction.serializer(), jsonString)
// val customFunction               = json.parse(CustomFunction.serializer(), jsonString)
// val functionConfiguration        = json.parse(FunctionConfiguration.serializer(), jsonString)
// val objectRelationship           = json.parse(ObjectRelationship.serializer(), jsonString)
// val objRelUsing                  = json.parse(ObjRelUsing.serializer(), jsonString)
// val objRelUsingManualMapping     = json.parse(ObjRelUsingManualMapping.serializer(), jsonString)
// val arrayRelationship            = json.parse(ArrayRelationship.serializer(), jsonString)
// val arrRelUsing                  = json.parse(ArrRelUsing.serializer(), jsonString)
// val arrRelUsingFKeyOn            = json.parse(ArrRelUsingFKeyOn.serializer(), jsonString)
// val arrRelUsingManualMapping     = json.parse(ArrRelUsingManualMapping.serializer(), jsonString)
// val columnPresetsExpression      = json.parse(ColumnPresetsExpression.serializer(), jsonString)
// val insertPermissionEntry        = json.parse(InsertPermissionEntry.serializer(), jsonString)
// val insertPermission             = json.parse(InsertPermission.serializer(), jsonString)
// val selectPermissionEntry        = json.parse(SelectPermissionEntry.serializer(), jsonString)
// val selectPermission             = json.parse(SelectPermission.serializer(), jsonString)
// val updatePermissionEntry        = json.parse(UpdatePermissionEntry.serializer(), jsonString)
// val updatePermission             = json.parse(UpdatePermission.serializer(), jsonString)
// val deletePermissionEntry        = json.parse(DeletePermissionEntry.serializer(), jsonString)
// val deletePermission             = json.parse(DeletePermission.serializer(), jsonString)
// val computedField                = json.parse(ComputedField.serializer(), jsonString)
// val computedFieldDefinition      = json.parse(ComputedFieldDefinition.serializer(), jsonString)
// val eventTrigger                 = json.parse(EventTrigger.serializer(), jsonString)
// val eventTriggerDefinition       = json.parse(EventTriggerDefinition.serializer(), jsonString)
// val eventTriggerColumns          = json.parse(EventTriggerColumns.serializer(), jsonString)
// val operationSpec                = json.parse(OperationSpec.serializer(), jsonString)
// val headerFromValue              = json.parse(HeaderFromValue.serializer(), jsonString)
// val headerFromEnv                = json.parse(HeaderFromEnv.serializer(), jsonString)
// val retryConf                    = json.parse(RetryConf.serializer(), jsonString)
// val cronTrigger                  = json.parse(CronTrigger.serializer(), jsonString)
// val retryConfST                  = json.parse(RetryConfST.serializer(), jsonString)
// val remoteSchema                 = json.parse(RemoteSchema.serializer(), jsonString)
// val remoteSchemaDef              = json.parse(RemoteSchemaDef.serializer(), jsonString)
// val remoteRelationship           = json.parse(RemoteRelationship.serializer(), jsonString)
// val remoteRelationshipDef        = json.parse(RemoteRelationshipDef.serializer(), jsonString)
// val remoteField                  = json.parse(RemoteField.serializer(), jsonString)
// val inputArguments               = json.parse(InputArguments.serializer(), jsonString)
// val queryCollectionEntry         = json.parse(QueryCollectionEntry.serializer(), jsonString)
// val queryCollection              = json.parse(QueryCollection.serializer(), jsonString)
// val allowList                    = json.parse(AllowList.serializer(), jsonString)
// val customTypes                  = json.parse(CustomTypes.serializer(), jsonString)
// val inputObjectType              = json.parse(InputObjectType.serializer(), jsonString)
// val inputObjectField             = json.parse(InputObjectField.serializer(), jsonString)
// val objectType                   = json.parse(ObjectType.serializer(), jsonString)
// val objectField                  = json.parse(ObjectField.serializer(), jsonString)
// val customTypeObjectRelationship = json.parse(CustomTypeObjectRelationship.serializer(), jsonString)
// val scalarType                   = json.parse(ScalarType.serializer(), jsonString)
// val enumType                     = json.parse(EnumType.serializer(), jsonString)
// val enumValue                    = json.parse(EnumValue.serializer(), jsonString)
// val action                       = json.parse(Action.serializer(), jsonString)
// val actionDefinition             = json.parse(ActionDefinition.serializer(), jsonString)
// val inputArgument                = json.parse(InputArgument.serializer(), jsonString)
// val hasuraMetadataV2             = json.parse(HasuraMetadataV2.serializer(), jsonString)
// val fromEnv                      = json.parse(FromEnv.serializer(), jsonString)
// val pGConfiguration              = json.parse(PGConfiguration.serializer(), jsonString)
// val mSSQLConfiguration           = json.parse(MSSQLConfiguration.serializer(), jsonString)
// val bigQueryConfiguration        = json.parse(BigQueryConfiguration.serializer(), jsonString)
// val pGSourceConnectionInfo       = json.parse(PGSourceConnectionInfo.serializer(), jsonString)
// val mSSQLSourceConnectionInfo    = json.parse(MSSQLSourceConnectionInfo.serializer(), jsonString)
// val pGConnectionParameters       = json.parse(PGConnectionParameters.serializer(), jsonString)
// val pGPoolSettings               = json.parse(PGPoolSettings.serializer(), jsonString)
// val pGCERTSettings               = json.parse(PGCERTSettings.serializer(), jsonString)
// val mSSQLPoolSettings            = json.parse(MSSQLPoolSettings.serializer(), jsonString)
// val backendKind                  = json.parse(BackendKind.serializer(), jsonString)
// val baseSource                   = json.parse(BaseSource.serializer(), jsonString)
// val pGSource                     = json.parse(PGSource.serializer(), jsonString)
// val mSSQLSource                  = json.parse(MSSQLSource.serializer(), jsonString)
// val bigQuerySource               = json.parse(BigQuerySource.serializer(), jsonString)
// val source                       = json.parse(Source.serializer(), jsonString)
// val aPILimits                    = json.parse(APILimits.serializer(), jsonString)
// val depthLimit                   = json.parse(DepthLimit.serializer(), jsonString)
// val rateLimit                    = json.parse(RateLimit.serializer(), jsonString)
// val rateLimitRule                = json.parse(RateLimitRule.serializer(), jsonString)
// val nodeLimit                    = json.parse(NodeLimit.serializer(), jsonString)
// val rESTEndpoint                 = json.parse(RESTEndpoint.serializer(), jsonString)
// val rESTEndpointDefinition       = json.parse(RESTEndpointDefinition.serializer(), jsonString)
// val inheritedRole                = json.parse(InheritedRole.serializer(), jsonString)
// val hasuraMetadataV3             = json.parse(HasuraMetadataV3.serializer(), jsonString)
// val recordStringAny              = json.parse(RecordStringAny.serializer(), jsonString)

package io.hasura.metadata.v3

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

typealias CustomColumnNames = HashMap<String, String>
typealias ColumnPresetsExpression = HashMap<String, String>
typealias RemoteField = HashMap<String, RemoteFieldValue>
typealias InputArguments = HashMap<String, String>
typealias RecordStringAny = HashMap<String, JsonObject?>

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/syntax-defs.html#headerfromvalue
 */
@Serializable
data class HeaderFromValue (
    /**
     * Name of the header
     */
    var name: String,

    /**
     * Value of the header
     */
    var value: String
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/syntax-defs.html#headerfromenv
 */
@Serializable
data class HeaderFromEnv (
    /**
     * Name of the header
     */
    var name: String,

    /**
     * Name of the environment variable which holds the value of the header
     */
    @SerialName("value_from_env")
    var valueFromEnv: String
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/custom-types.html#objectfield
 */
@Serializable
data class ObjectField (
    /**
     * Description of the Input object type
     */
    var description: String? = null,

    /**
     * Name of the Input object type
     */
    var name: String,

    /**
     * GraphQL type of the Input object type
     */
    var type: String
)

/**
 * Type used in exported 'metadata.json' and replace metadata endpoint
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/manage-metadata.html#replace-metadata
 */
@Serializable
data class HasuraMetadataV2 (
    var actions: List<Action>? = null,
    var allowlist: List<AllowList>? = null,

    @SerialName("cron_triggers")
    var cronTriggers: List<CronTrigger>? = null,

    @SerialName("custom_types")
    var customTypes: CustomTypes? = null,

    var functions: List<CustomFunction>? = null,

    @SerialName("query_collections")
    var queryCollections: List<QueryCollectionEntry>? = null,

    @SerialName("remote_schemas")
    var remoteSchemas: List<RemoteSchema>? = null,

    var tables: List<TableEntry>,
    var version: Double
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/actions.html#args-syntax
 */
@Serializable
data class Action (
    /**
     * Comment
     */
    var comment: String? = null,

    /**
     * Definition of the action
     */
    var definition: ActionDefinition,

    /**
     * Name of the action
     */
    var name: String,

    /**
     * Permissions of the action
     */
    var permissions: List<Permission>? = null
)

/**
 * Definition of the action
 *
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/actions.html#actiondefinition
 */
@Serializable
data class ActionDefinition (
    var arguments: List<InputArgument>? = null,

    @SerialName("forward_client_headers")
    var forwardClientHeaders: Boolean? = null,

    /**
     * A String value which supports templating environment variables enclosed in {{ and }}.
     * Template example: https://{{ACTION_API_DOMAIN}}/create-user
     */
    var handler: String,

    var headers: List<Header>? = null,
    var kind: String? = null,

    @SerialName("output_type")
    var outputType: String? = null,

    var type: ActionDefinitionType? = null,

    // FIXME: this was missing from generated type
    var timeout: Long? = null,

    @SerialName("request_transform")
    var requestTransform: RequestTransformation? = null,

    @SerialName("response_transform")
    var responseTransform: ResponseTransformation? = null,



    )

// https://hasura.io/docs/latest/api-reference/syntax-defs/#requesttransformation
@Serializable
data class RequestTransformation(
    var version: String? = null,
    var method: String? = null,
    var url: String? = null,
    var body: BodyTransform? = null,
    @SerialName("content_type")
    var contentType: String? = null,
    @SerialName("query_params")
    var queryParams: Map<String, String>? = null,
    @SerialName("request_headers")
    var requestHeaders: TransformHeaders? = null,
    @SerialName("template_engine")
    var templateEngne: String?, // "kriti",

)

// https://hasura.io/docs/latest/api-reference/syntax-defs/#bodytransform
@Serializable(with = BodyTransformSerializer::class)
sealed class BodyTransform {
    class BodyTransformDefinitionValue( val value: BodyTransformDefinition) : BodyTransform()
    class StringValue(val value: String) : BodyTransform()
}


class BodyTransformSerializer : KSerializer<BodyTransform> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("BodyTransformSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BodyTransform {
        val input = decoder as? JsonDecoder
            ?: throw SerializationException("Expected JsonInput for ${decoder::class}")
        val jsonElem = input.decodeJsonElement()
        if (jsonElem is JsonPrimitive) {
            return BodyTransform.StringValue(jsonElem.jsonPrimitive.content)
        }
        return BodyTransform.BodyTransformDefinitionValue(Json.decodeFromJsonElement<BodyTransformDefinition>(jsonElem))
    }

    override fun serialize(encoder: Encoder, value: BodyTransform) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("Expected JsonEncoder for ${encoder::class}")
        when (value) {
            is BodyTransform.BodyTransformDefinitionValue -> {
                BodyTransformDefinition.serializer().serialize(encoder, value.value)
            }
            is BodyTransform.StringValue -> encoder.encodeString(value.value)
        }
    }
}

@Serializable
data class BodyTransformDefinition(
    var action: String, // remove | transform | x_www_form_urlencoded
    var template: String,
    @SerialName("form_template")
    var formTemplate: Map<String, String>
)

// https://hasura.io/docs/latest/api-reference/syntax-defs/#transformheaders
@Serializable
data class TransformHeaders(
    @SerialName("add_headers")
    var addHeaders: Map<String, String>? = null,
    @SerialName("remove_headers")
    var removeHeaders: List<String>? = null,
)

// https://hasura.io/docs/latest/api-reference/syntax-defs/#responsetransformation
@Serializable
data class ResponseTransformation(
    var version: String? = null,
    var body: BodyTransform? = null,
    @SerialName("template_engine")
    var templateEngne: String?, // "kriti",
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/actions.html#inputargument
 */
@Serializable
data class InputArgument (
    var name: String,
    var type: String
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/syntax-defs.html#headerfromvalue
 *
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/syntax-defs.html#headerfromenv
 */
@Serializable
data class Header (
    /**
     * Name of the header
     */
    var name: String,

    /**
     * Value of the header
     */
    var value: String? = null,

    /**
     * Name of the environment variable which holds the value of the header
     */
    @SerialName("value_from_env")
    var valueFromEnv: String? = null
)

@Serializable
enum class ActionDefinitionType(val value: String) {
    MUTATION("mutation"),
    QUERY("query");

    @Serializer(forClass = ActionDefinitionType::class)
    companion object : KSerializer<ActionDefinitionType> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("io.hasura.metadata.v3.ActionDefinitionType", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): ActionDefinitionType = when (val value = decoder.decodeString()) {
            "mutation" -> MUTATION
            "query"    -> QUERY
            else       -> throw IllegalArgumentException("ActionDefinitionType could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: ActionDefinitionType) {
            return encoder.encodeString(value.value)
        }
    }
}

@Serializable
data class Permission (
    var role: String
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/query-collections.html#add-collection-to-allowlist-syntax
 */
@Serializable
data class AllowList (
    /**
     * Name of a query collection to be added to the allow-list
     */
    var collection: String
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/scheduled-triggers.html#create-cron-trigger
 */
@Serializable
data class CronTrigger (
    /**
     * Custom comment.
     */
    var comment: String? = null,

    /**
     * List of headers to be sent with the webhook
     */
    var headers: List<Header>,

    /**
     * Flag to indicate whether a trigger should be included in the metadata. When a cron
     * trigger is included in the metadata, the user will be able to export it when the metadata
     * of the graphql-engine is exported.
     */
    @SerialName("include_in_metadata")
    var includeInMetadata: Boolean,

    /**
     * Name of the cron trigger
     */
    var name: String,

    /**
     * Any JSON payload which will be sent when the webhook is invoked.
     */
    var payload: JsonObject? = null,

    /**
     * Retry configuration if scheduled invocation delivery fails
     */
    @SerialName("retry_conf")
    var retryConf: RetryConfST? = null,

    /**
     * Cron expression at which the trigger should be invoked.
     */
    var schedule: String,

    /**
     * URL of the webhook
     */
    var webhook: String,

    @SerialName("request_transform")
    var requestTransform: RequestTransformation? = null,

    @SerialName("response_transform")
    var responseTransform: ResponseTransformation? = null,
)

/**
 * Retry configuration if scheduled invocation delivery fails
 *
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/scheduled-triggers.html#retryconfst
 */
@Serializable
data class RetryConfST (
    /**
     * Number of times to retry delivery.
     * Default: 0
     */
    @SerialName("num_retries")
    var numRetries: Long? = null,

    /**
     * Number of seconds to wait between each retry.
     * Default: 10
     */
    @SerialName("retry_interval_seconds")
    var retryIntervalSeconds: Long? = null,

    /**
     * Number of seconds to wait for response before timing out.
     * Default: 60
     */
    @SerialName("timeout_seconds")
    var timeoutSeconds: Long? = null,

    /**
     * Number of seconds between scheduled time and actual delivery time that is acceptable. If
     * the time difference is more than this, then the event is dropped.
     * Default: 21600 (6 hours)
     */
    @SerialName("tolerance_seconds")
    var toleranceSeconds: Long? = null
)

@Serializable
data class CustomTypes (
    var enums: List<EnumType>? = null,

    @SerialName("input_objects")
    var inputObjects: List<InputObjectType>? = null,

    var objects: List<ObjectType>? = null,
    var scalars: List<ScalarType>? = null
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/custom-types.html#enumtype
 */
@Serializable
data class EnumType (
    /**
     * Description of the Enum type
     */
    var description: String? = null,

    /**
     * Name of the Enum type
     */
    var name: String,

    /**
     * Values of the Enum type
     */
    var values: List<EnumValue>
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/custom-types.html#enumvalue
 */
@Serializable
data class EnumValue (
    /**
     * Description of the Enum value
     */
    var description: String? = null,

    /**
     * If set to true, the enum value is marked as deprecated
     */
    @SerialName("is_deprecated")
    var isDeprecated: Boolean? = null,

    /**
     * Value of the Enum type
     */
    var value: String
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/custom-types.html#inputobjecttype
 */
@Serializable
data class InputObjectType (
    /**
     * Description of the Input object type
     */
    var description: String? = null,

    /**
     * Fields of the Input object type
     */
    var fields: List<InputObjectField>,

    /**
     * Name of the Input object type
     */
    var name: String
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/custom-types.html#inputobjectfield
 */
@Serializable
data class InputObjectField (
    /**
     * Description of the Input object type
     */
    var description: String? = null,

    /**
     * Name of the Input object type
     */
    var name: String,

    /**
     * GraphQL type of the Input object type
     */
    var type: String
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/custom-types.html#objecttype
 */
@Serializable
data class ObjectType (
    /**
     * Description of the Input object type
     */
    var description: String? = null,

    /**
     * Fields of the Input object type
     */
    var fields: List<InputObjectField>,

    /**
     * Name of the Input object type
     */
    var name: String,

    /**
     * Relationships of the Object type to tables
     */
    var relationships: List<CustomTypeObjectRelationship>? = null
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/custom-types.html#objectrelationship
 */
@Serializable
data class CustomTypeObjectRelationship (
    /**
     * Mapping of fields of object type to columns of remote table
     */
    @SerialName("field_mapping")
    var fieldMapping: Map<String, String>,

    /**
     * Name of the relationship, shouldn’t conflict with existing field names
     */
    var name: String,

    /**
     * The table to which relationship is defined
     */
    @SerialName("remote_table")
    var remoteTable: TableName,

    /**
     * Type of the relationship
     */
    var type: CustomTypeObjectRelationshipType
)

// https://www.appsloveworld.com/kotlin/100/5/kotlinx-serialization-polymorphic-serializer-was-not-found-for-missing-class-dis
@Serializable(with = TableNameSerializer::class)
sealed class TableName {
    class QualifiedTableValue(val value: QualifiedTable) : TableName()
    class StringValue(val name: String)                 : TableName()
}

class TableNameSerializer : KSerializer<TableName> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("TableNameSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): TableName {
        val input = decoder as? JsonDecoder
            ?: throw SerializationException("Expected JsonInput for ${decoder::class}")
        val jsonElem = input.decodeJsonElement()
        if (jsonElem is JsonPrimitive) {
            return TableName.StringValue(jsonElem.jsonPrimitive.content)
        }
        return TableName.QualifiedTableValue(Json.decodeFromJsonElement<QualifiedTable>(jsonElem))
    }

    override fun serialize(encoder: Encoder, value: TableName) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("Expected JsonEncoder for ${encoder::class}")
        when (value) {
            is TableName.QualifiedTableValue -> {
                QualifiedTable.serializer().serialize(encoder, value.value)
            }
            is TableName.StringValue -> encoder.encodeString(value.name)
        }
    }
}
@Serializable
data class QualifiedTable (
    var name: String,
    var schema: String
)

/**
 * Type of the relationship
 */
@Serializable
enum class CustomTypeObjectRelationshipType(val value: String) {
    Object("object"),
    TypeArray("array");

    @Serializer(forClass = CustomTypeObjectRelationshipType::class)
    companion object : KSerializer<CustomTypeObjectRelationshipType> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("io.hasura.metadata.v3.CustomTypeObjectRelationshipType", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): CustomTypeObjectRelationshipType = when (val value = decoder.decodeString()) {
            "object" -> Object
            "array"  -> TypeArray
            else     -> throw IllegalArgumentException("CustomTypeObjectRelationshipType could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: CustomTypeObjectRelationshipType) {
            return encoder.encodeString(value.value)
        }
    }
}

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/custom-types.html#scalartype
 */
@Serializable
data class ScalarType (
    /**
     * Description of the Scalar type
     */
    var description: String? = null,

    /**
     * Name of the Scalar type
     */
    var name: String
)

/**
 * A custom SQL function to add to the GraphQL schema with configuration.
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/custom-functions.html#args-syntax
 */
@Serializable
data class CustomFunction (
    /**
     * Configuration for the SQL function
     */
    var configuration: FunctionConfiguration? = null,

    /**
     * Name of the SQL function
     */
    var function: FunctionName
)

/**
 * Configuration for the SQL function
 *
 * Configuration for a CustomFunction
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/custom-functions.html#function-configuration
 */
@Serializable
data class FunctionConfiguration (
    /**
     * Function argument which accepts session info JSON
     * Currently, only functions which satisfy the following constraints can be exposed over the
     * GraphQL API (terminology from Postgres docs):
     * - Function behaviour: ONLY `STABLE` or `IMMUTABLE`
     * - Return type: MUST be `SETOF <table-name>`
     * - Argument modes: ONLY `IN`
     */
    @SerialName("session_argument")
    var sessionArgument: String? = null

    // TODO: mmissing fields from https://hasura.io/docs/latest/api-reference/syntax-defs/#function-configuration
)

@Serializable(with = FunctionNameSerializer::class)
sealed class FunctionName {
    class QualifiedFunctionValue(val value: QualifiedFunction) : FunctionName()
    class StringValue(val value: String)                       : FunctionName()
}


class FunctionNameSerializer : KSerializer<FunctionName> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("FunctionNameSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): FunctionName {
        val input = decoder as? JsonDecoder
            ?: throw SerializationException("Expected JsonInput for ${decoder::class}")
        val jsonElem = input.decodeJsonElement()
        if (jsonElem is JsonPrimitive) {
            return FunctionName.StringValue(jsonElem.jsonPrimitive.content)
        }
        return FunctionName.QualifiedFunctionValue(Json.decodeFromJsonElement<QualifiedFunction>(jsonElem))
    }

    override fun serialize(encoder: Encoder, value: FunctionName) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("Expected JsonEncoder for ${encoder::class}")
        when (value) {
            is FunctionName.QualifiedFunctionValue -> {
                QualifiedFunction.serializer().serialize(encoder, value.value)
            }
            is FunctionName.StringValue -> encoder.encodeString(value.value)
        }
    }
}


@Serializable
data class QualifiedFunction (
    var name: String,
    var schema: String
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/query-collections.html#args-syntax
 */
@Serializable
data class QueryCollectionEntry (
    /**
     * Comment
     */
    var comment: String? = null,

    /**
     * List of queries
     */
    var definition: Definition,

    /**
     * Name of the query collection
     */
    var name: String
)

/**
 * List of queries
 */
@Serializable
data class Definition (
    var queries: List<QueryCollection>
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/syntax-defs.html#collectionquery
 */
@Serializable
data class QueryCollection (
    var name: String,
    var query: String
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/remote-schemas.html#add-remote-schema
 */
@Serializable
data class RemoteSchema (
    /**
     * Comment
     */
    var comment: String? = null,

    /**
     * Name of the remote schema
     */
    var definition: RemoteSchemaDef,

    /**
     * Name of the remote schema
     */
    var name: String
)

/**
 * Name of the remote schema
 *
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/syntax-defs.html#remoteschemadef
 */
@Serializable
data class RemoteSchemaDef (
    @SerialName("forward_client_headers")
    var forwardClientHeaders: Boolean? = null,

    var headers: List<Header>? = null,

    @SerialName("timeout_seconds")
    var timeoutSeconds: Double? = null,

    var url: String? = null,

    @SerialName("url_from_env")
    var urlFromEnv: String? = null
)

/**
 * Representation of a table in metadata, 'tables.yaml' and 'metadata.json'
 */
@Serializable
data class TableEntry (
    var table: QualifiedTable,

    @SerialName("array_relationships")
    var arrayRelationships: List<ArrayRelationship>? = null,

    @SerialName("computed_fields")
    var computedFields: List<ComputedField>? = null,

    /**
     * Configuration for the table/view
     *
     * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/table-view.html#table-config
     */
    var configuration: TableConfig? = null,

    @SerialName("delete_permissions")
    var deletePermissions: List<DeletePermissionEntry>? = null,

    @SerialName("event_triggers")
    var eventTriggers: List<EventTrigger>? = null,

    @SerialName("insert_permissions")
    var insertPermissions: List<InsertPermissionEntry>? = null,

    @SerialName("is_enum")
    var isEnum: Boolean? = null,

    @SerialName("object_relationships")
    var objectRelationships: List<ObjectRelationship>? = null,

    @SerialName("remote_relationships")
    var remoteRelationships: List<RemoteRelationship>? = null,

    @SerialName("select_permissions")
    var selectPermissions: List<SelectPermissionEntry>? = null,

    @SerialName("update_permissions")
    var updatePermissions: List<UpdatePermissionEntry>? = null,

    @SerialName("apollo_federation_config")
    var apolloFederationConfig: ApolloFederationConfig? = null
)

@Serializable
data class ApolloFederationConfig(
    var enable: String
)
/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/relationship.html#create-array-relationship-syntax
 */
@Serializable
data class ArrayRelationship (
    /**
     * Comment
     */
    var comment: String? = null,

    /**
     * Name of the new relationship
     */
    var name: String,

    /**
     * Use one of the available ways to define an array relationship
     */
    var using: ArrRelUsing
)

/**
 * Use one of the available ways to define an array relationship
 *
 * Use one of the available ways to define an object relationship
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/relationship.html#arrrelusing
 */
@Serializable
data class ArrRelUsing (
    /**
     * The column with foreign key constraint
     */
    @SerialName("foreign_key_constraint_on")
    var foreignKeyConstraintOn: ArrRelUsingFKeyOn? = null,

    /**
     * Manual mapping of table and columns
     */
    @SerialName("manual_configuration")
    var manualConfiguration: ArrRelUsingManualMapping? = null
)

/**
 * The column with foreign key constraint
 *
 * The column with foreign key constraint
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/relationship.html#arrrelusingfkeyon
 */
@Serializable
data class ArrRelUsingFKeyOn (
    var column: String,
    var table: TableName
)

/**
 * Manual mapping of table and columns
 *
 * Manual mapping of table and columns
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/relationship.html#arrrelusingmanualmapping
 */
@Serializable
data class ArrRelUsingManualMapping (
    /**
     * Mapping of columns from current table to remote table
     */
    @SerialName("column_mapping")
    var columnMapping: Map<String, String>,

    /**
     * The table to which the relationship has to be established
     */
    @SerialName("remote_table")
    var remoteTable: TableName
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/computed-field.html#args-syntax
 */
@Serializable
data class ComputedField (
    /**
     * Comment
     */
    var comment: String? = null,

    /**
     * The computed field definition
     */
    var definition: ComputedFieldDefinition,

    /**
     * Name of the new computed field
     */
    var name: String
)

/**
 * The computed field definition
 *
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/computed-field.html#computedfielddefinition
 */
@Serializable
data class ComputedFieldDefinition (
    /**
     * The SQL function
     */
    var function: FunctionName,

    /**
     * Name of the argument which accepts the Hasura session object as a JSON/JSONB value. If
     * omitted, the Hasura session object is not passed to the function
     */
    @SerialName("session_argument")
    var sessionArgument: String? = null,

    /**
     * Name of the argument which accepts a table row type. If omitted, the first argument is
     * considered a table argument
     */
    @SerialName("table_argument")
    var tableArgument: String? = null
)

@Serializable
data class ColumnConfigValue (
    var customName: String? = null,
    var comment: String? = null
)

/**
 * Configuration for the table/view
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/table-view.html#table-config
 */
@Serializable
data class TableConfig (
    /**
     * Customise the column names
     */
    @SerialName("custom_column_names")
    @Deprecated("Deprecated, use columnConfig instead")
    var customColumnNames: Map<String, String>? = null,

    @SerialName("column_config")
    var columnConfig: Map<String, ColumnConfigValue>? = null,

    /**
     * Customise the table name
     */
    @SerialName("custom_name")
    var customName: String? = null,

    /**
     * Customise the root fields
     */
    @SerialName("custom_root_fields")
    var customRootFields: CustomRootFields? = null
)

/**
 * Customise the root fields
 *
 * Customise the root fields
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/table-view.html#custom-root-fields
 */
@Serializable
data class CustomRootFields (
    /**
     * Customise the `delete_<table-name>` root field
     */
    var delete: String? = null,

    /**
     * Customise the `delete_<table-name>_by_pk` root field
     */
    @SerialName("delete_by_pk")
    var deleteByPk: String? = null,

    /**
     * Customise the `insert_<table-name>` root field
     */
    var insert: String? = null,

    /**
     * Customise the `insert_<table-name>_one` root field
     */
    @SerialName("insert_one")
    var insertOne: String? = null,

    /**
     * Customise the `<table-name>` root field
     */
    var select: String? = null,

    /**
     * Customise the `<table-name>_aggregate` root field
     */
    @SerialName("select_aggregate")
    var selectAggregate: String? = null,

    /**
     * Customise the `<table-name>_by_pk` root field
     */
    @SerialName("select_by_pk")
    var selectByPk: String? = null,

    /**
     * Customise the `update_<table-name>` root field
     */
    var update: String? = null,

    /**
     * Customise the `update_<table-name>_by_pk` root field
     */
    @SerialName("update_by_pk")
    var updateByPk: String? = null
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/permission.html#create-delete-permission-syntax
 */
@Serializable
data class DeletePermissionEntry (
    /**
     * Comment
     */
    var comment: String? = null,

    /**
     * The permission definition
     */
    var permission: DeletePermission,

    /**
     * Role
     */
    var role: String
)

/**
 * The permission definition
 *
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/permission.html#deletepermission
 */
@Serializable
data class DeletePermission (
    /**
     * Only the rows where this precondition holds true are updatable
     */
    var filter: Filter? = null
)

@Serializable(with = FilterSerializer::class)
sealed class Filter {
    class AnythingMapValue(val value: JsonObject) : Filter()
    class StringValue(val value: String)          : Filter()
}

class FilterSerializer : KSerializer<Filter> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("FilterSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Filter {
        val input = decoder as? JsonDecoder
            ?: throw SerializationException("Expected JsonInput for ${decoder::class}")
        val jsonElem = input.decodeJsonElement()
        if (jsonElem is JsonPrimitive) {
            return Filter.StringValue(jsonElem.jsonPrimitive.content)
        }
        return Filter.AnythingMapValue(jsonElem.jsonObject)
    }

    override fun serialize(encoder: Encoder, value: Filter) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("Expected JsonEncoder for ${encoder::class}")
        when (value) {
            is Filter.AnythingMapValue -> encoder.encodeJsonElement(value.value)
            is Filter.StringValue -> encoder.encodeString(value.value)
        }
    }
}

/**
 * NOTE: The metadata type doesn't QUITE match the 'create' arguments here
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/event-triggers.html#create-event-trigger
 */
@Serializable
data class EventTrigger (
    /**
     * The SQL function
     */
    var definition: EventTriggerDefinition,

    /**
     * The SQL function
     */
    var headers: List<Header>? = null,

    /**
     * Name of the event trigger
     */
    var name: String,

    /**
     * The SQL function
     */
    @SerialName("retry_conf")
    var retryConf: RetryConf,

    /**
     * The SQL function
     */
    var webhook: String? = null,

    @SerialName("webhook_from_env")
    var webhookFromEnv: String? = null
)

/**
 * The SQL function
 */
@Serializable
data class EventTriggerDefinition (
    /**
     *
     * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/event-triggers.html#operationspec
     */
    var delete: OperationSpec? = null,

    @SerialName("enable_manual")
    var enableManual: Boolean,

    /**
     *
     * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/event-triggers.html#operationspec
     */
    var insert: OperationSpec? = null,

    /**
     *
     * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/event-triggers.html#operationspec
     */
    var update: OperationSpec? = null
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/event-triggers.html#operationspec
 */
@Serializable
data class OperationSpec (
    /**
     *
     * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/event-triggers.html#eventtriggercolumns
     */
    var columns: EventTriggerColumns,

    /**
     *
     * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/event-triggers.html#eventtriggercolumns
     */
    var payload: EventTriggerColumns? = null
)

@Serializable(with = EventTriggerColumnsSerializer::class)
sealed class EventTriggerColumns {
    class EnumValue(val value: Columns)             : EventTriggerColumns()
    class StringArrayValue(val value: List<String>) : EventTriggerColumns()
}

class EventTriggerColumnsSerializer : KSerializer<EventTriggerColumns> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("EventTriggerColumnsSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): EventTriggerColumns {
        val input = decoder as? JsonDecoder
            ?: throw SerializationException("Expected JsonInput for ${decoder::class}")
        val jsonElem = input.decodeJsonElement()
        if (jsonElem is JsonPrimitive) {
            return EventTriggerColumns.EnumValue(Columns.Empty)
        }
        return EventTriggerColumns.StringArrayValue(Json.decodeFromJsonElement<List<String>>(jsonElem))
    }

    override fun serialize(encoder: Encoder, value: EventTriggerColumns) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("Expected JsonEncoder for ${encoder::class}")
        when (value) {
            is EventTriggerColumns.EnumValue -> {
                jsonEncoder.encodeJsonElement(JsonPrimitive("*"))
            }
            is EventTriggerColumns.StringArrayValue -> jsonEncoder.encodeJsonElement(buildJsonArray {
                value.value.forEach {
                    add(JsonPrimitive(it))
                }
            })
        }
    }
}

@Serializable
enum class Columns(val value: String) {
    Empty("*");

    @Serializer(forClass = Columns::class)
    companion object : KSerializer<Columns> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("io.hasura.metadata.v3.Columns", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): Columns = when (val value = decoder.decodeString()) {
            "*"  -> Empty
            else -> throw IllegalArgumentException("Columns could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: Columns) {
            return encoder.encodeString(value.value)
        }
    }
}

/**
 * The SQL function
 *
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/event-triggers.html#retryconf
 */
@Serializable
data class RetryConf (
    /**
     * Number of seconds to wait between each retry.
     * Default: 10
     */
    @SerialName("interval_sec")
    var intervalSEC: Long? = null,

    /**
     * Number of times to retry delivery.
     * Default: 0
     */
    @SerialName("num_retries")
    var numRetries: Long? = null,

    /**
     * Number of seconds to wait for response before timing out.
     * Default: 60
     */
    @SerialName("timeout_sec")
    var timeoutSEC: Long? = null
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/permission.html#args-syntax
 */
@Serializable
data class InsertPermissionEntry (
    /**
     * Comment
     */
    var comment: String? = null,

    /**
     * The permission definition
     */
    var permission: InsertPermission,

    /**
     * Role
     */
    var role: String
)

/**
 * The permission definition
 *
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/permission.html#insertpermission
 */
@Serializable
data class InsertPermission (
    /**
     * When set to true the mutation is accessible only if x-hasura-use-backend-only-permissions
     * session variable exists
     * and is set to true and request is made with x-hasura-admin-secret set if any auth is
     * configured
     */
    @SerialName("backend_only")
    var backendOnly: Boolean? = null,

    /**
     * This expression has to hold true for every new row that is inserted
     */
    var check: Map<String, Filter>? = null,

    /**
     * Can insert into only these columns (or all when '*' is specified)
     */
    var columns: EventTriggerColumns,

    /**
     * Preset values for columns that can be sourced from session variables or static values
     */
    var set: Map<String, String>? = null
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/relationship.html#args-syntax
 */
@Serializable
data class ObjectRelationship (
    /**
     * Comment
     */
    var comment: String? = null,

    /**
     * Name of the new relationship
     */
    var name: String,

    /**
     * Use one of the available ways to define an object relationship
     */
    var using: ObjRelUsing
)

/**
 * Use one of the available ways to define an object relationship
 *
 * Use one of the available ways to define an object relationship
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/relationship.html#objrelusing
 */
@Serializable
data class ObjRelUsing (
    /**
     * The column with foreign key constraint
     */
    @SerialName("foreign_key_constraint_on")
    var foreignKeyConstraintOn: String? = null,

    /**
     * Manual mapping of table and columns
     */
    @SerialName("manual_configuration")
    var manualConfiguration: ObjRelUsingManualMapping? = null
)

/**
 * Manual mapping of table and columns
 *
 * Manual mapping of table and columns
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/relationship.html#objrelusingmanualmapping
 */
@Serializable
data class ObjRelUsingManualMapping (
    /**
     * Mapping of columns from current table to remote table
     */
    @SerialName("column_mapping")
    var columnMapping: Map<String, String>,

    /**
     * The table to which the relationship has to be established
     */
    @SerialName("remote_table")
    var remoteTable: TableName
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/remote-relationships.html#args-syntax
 */
@Serializable
data class RemoteRelationship (
    /**
     * Definition object
     */
    var definition: RemoteRelationshipDef,

    /**
     * Name of the remote relationship
     */
    var name: String
)

/**
 * Definition object
 */
@Serializable
data class RemoteRelationshipDef (
    /**
     * Column(s) in the table that is used for joining with remote schema field.
     * All join keys in remote_field must appear here.
     */
    @SerialName("hasura_fields")
    var hasuraFields: List<String>,

    /**
     * The schema tree ending at the field in remote schema which needs to be joined with.
     */
    @SerialName("remote_field")
    var remoteField: Map<String, RemoteFieldValue>,

    /**
     * Name of the remote schema to join with
     */
    @SerialName("remote_schema")
    var remoteSchema: String
)

@Serializable
data class RemoteFieldValue (
    var arguments: Map<String, String>,

    /**
     * A recursive tree structure that points to the field in the remote schema that needs to be
     * joined with.
     * It is recursive because the remote field maybe nested deeply in the remote schema.
     *
     * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/remote-relationships.html#remotefield
     */
    var field: Map<String, RemoteFieldValue>? = null
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/permission.html#create-select-permission-syntax
 */
@Serializable
data class SelectPermissionEntry (
    /**
     * Comment
     */
    var comment: String? = null,

    /**
     * The permission definition
     */
    var permission: SelectPermission,

    /**
     * Role
     */
    var role: String
)

/**
 * The permission definition
 *
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/permission.html#selectpermission
 */
@Serializable
data class SelectPermission (
    /**
     * Toggle allowing aggregate queries
     */
    @SerialName("allow_aggregations")
    var allowAggregations: Boolean? = null,

    /**
     * Only these columns are selectable (or all when '*' is specified)
     */
    var columns: EventTriggerColumns,

    /**
     * Only these computed fields are selectable
     */
    @SerialName("computed_fields")
    var computedFields: List<String>? = null,

    /**
     * Only the rows where this precondition holds true are selectable
     */
    var filter: Filter? = null,

    /**
     * The maximum number of rows that can be returned
     */
    var limit: Long? = null
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/permission.html#create-update-permission-syntax
 */
@Serializable
data class UpdatePermissionEntry (
    /**
     * Comment
     */
    var comment: String? = null,

    /**
     * The permission definition
     */
    var permission: UpdatePermission,

    /**
     * Role
     */
    var role: String
)

/**
 * The permission definition
 *
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/schema-metadata-api/permission.html#updatepermission
 */
@Serializable
data class UpdatePermission (
    /**
     * Postcondition which must be satisfied by rows which have been updated
     */
    var check: Filter? = null,

    /**
     * Only these columns are selectable (or all when '*' is specified)
     */
    var columns: EventTriggerColumns,

    /**
     * Only the rows where this precondition holds true are updatable
     */
    var filter: Filter? = null,

    /**
     * Preset values for columns that can be sourced from session variables or static values
     */
    var set: Map<String, String>? = null
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#pgconnectionparameters
 */
@Serializable
data class PGConnectionParameters (
    /**
     * The database name
     */
    var database: String,

    /**
     * The name of the host to connect to
     */
    var host: String,

    /**
     * The Postgres user’s password
     */
    var password: String? = null,

    /**
     * The port number to connect with, at the server host
     */
    var port: Double,

    /**
     * The Postgres user to be connected
     */
    var username: String
)

@Serializable
data class BaseSource (
    var functions: List<CustomFunction>? = null,
    var name: String,
    var tables: List<TableEntry>
)

@Serializable
data class PGSource (
    var configuration: PGConfiguration,
    var functions: List<CustomFunction>? = null,
    var kind: PGSourceKind,
    var name: String,
    var tables: List<TableEntry>
)

/**
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#pgconfiguration
 */
@Serializable
data class PGConfiguration (
    /**
     * Connection parameters for the source
     */
    @SerialName("connection_info")
    var connectionInfo: PGSourceConnectionInfo,

    /**
     * Optional list of read replica configuration (supported only in cloud/enterprise versions)
     */
    @SerialName("read_replicas")
    var readReplicas: List<PGSourceConnectionInfo>? = null
)

/**
 * Connection parameters for the source
 *
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#pgsourceconnectioninfo
 */
@Serializable
data class PGSourceConnectionInfo (
    /**
     * The database connection URL as a string, as an environment variable, or as connection
     * parameters.
     */
    @SerialName("database_url")
    var databaseURL: DatabaseURL,

    /**
     * The transaction isolation level in which the queries made to the source will be run with
     * (default: read-committed).
     */
    @SerialName("isolation_level")
    var isolationLevel: IsolationLevel? = null,

    /**
     * Connection pool settings
     */
    @SerialName("pool_settings")
    var poolSettings: PGPoolSettings? = null,

    /**
     * The client SSL certificate settings for the database (Only available in Cloud).
     */
    @SerialName("ssl_configuration")
    var sslConfiguration: PGCERTSettings? = null,

    /**
     * If set to true the server prepares statement before executing on the source database
     * (default: false). For more details, refer to the Postgres docs
     */
    @SerialName("use_prepared_statements")
    var usePreparedStatements: Boolean? = null
)

/**
 * The database connection URL as a string, as an environment variable, or as connection
 * parameters.
 */
@Serializable(with = DatabaseURLSerializer::class)
sealed class DatabaseURL {
    class PGConnectionParametersClassValue(val value: PGConnectionParametersClass) : DatabaseURL()
    class StringValue(val value: String)                                           : DatabaseURL()
}

class DatabaseURLSerializer : KSerializer<DatabaseURL> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("DatabaseURLSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): DatabaseURL {
        val input = decoder as? JsonDecoder
            ?: throw SerializationException("Expected JsonInput for ${decoder::class}")
        val jsonElem = input.decodeJsonElement()
        if (jsonElem is JsonPrimitive) {
            return DatabaseURL.StringValue(jsonElem.jsonPrimitive.content)
        }
        return DatabaseURL.PGConnectionParametersClassValue(Json.decodeFromJsonElement<PGConnectionParametersClass>(jsonElem))
    }

    override fun serialize(encoder: Encoder, value: DatabaseURL) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("Expected JsonEncoder for ${encoder::class}")
        when (value) {
            is DatabaseURL.PGConnectionParametersClassValue -> {
                PGConnectionParametersClass.serializer().serialize(encoder, value.value)
            }
            is DatabaseURL.StringValue -> encoder.encodeString(value.value)
        }
    }
}

/**
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#fromenv
 *
 * Environment variable which stores the client certificate.
 *
 * Environment variable which stores the client private key.
 *
 * Environment variable which stores trusted certificate authorities.
 *
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#pgconnectionparameters
 */
@Serializable
data class PGConnectionParametersClass (
    /**
     * Name of the environment variable
     */
    @SerialName("from_env")
    var fromEnv: String? = null,

    /**
     * The database name
     */
    var database: String? = null,

    /**
     * The name of the host to connect to
     */
    var host: String? = null,

    /**
     * The Postgres user’s password
     */
    var password: String? = null,

    /**
     * The port number to connect with, at the server host
     */
    var port: Double? = null,

    /**
     * The Postgres user to be connected
     */
    var username: String? = null
)

/**
 * The transaction isolation level in which the queries made to the source will be run with
 * (default: read-committed).
 */
@Serializable
enum class IsolationLevel(val value: String) {
    ReadCommitted("read-committed"),
    RepeatableRead("repeatable-read"),
    Serializable("serializable");

    @Serializer(forClass = IsolationLevel::class)
    companion object : KSerializer<IsolationLevel> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("io.hasura.metadata.v3.IsolationLevel", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): IsolationLevel = when (val value = decoder.decodeString()) {
            "read-committed"  -> ReadCommitted
            "repeatable-read" -> RepeatableRead
            "serializable"    -> Serializable
            else              -> throw IllegalArgumentException("IsolationLevel could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: IsolationLevel) {
            return encoder.encodeString(value.value)
        }
    }
}

/**
 * Connection pool settings
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#pgpoolsettings
 */
@Serializable
data class PGPoolSettings (
    /**
     * Time from connection creation after which the connection should be destroyed and a new
     * one created. A value of 0 indicates we should never destroy an active connection. If 0 is
     * passed, memory from large query results may not be reclaimed. (default: 600 sec)
     */
    @SerialName("connection_lifetime")
    var connectionLifetime: Double? = null,

    /**
     * The idle timeout (in seconds) per connection (default: 180)
     */
    @SerialName("idle_timeout")
    var idleTimeout: Double? = null,

    /**
     * Maximum number of connections to be kept in the pool (default: 50)
     */
    @SerialName("max_connections")
    var maxConnections: Double? = null,

    /**
     * Maximum time to wait while acquiring a Postgres connection from the pool, in seconds
     * (default: forever)
     */
    @SerialName("pool_timeout")
    var poolTimeout: Double? = null,

    /**
     * Number of retries to perform (default: 1)
     */
    var retries: Double? = null
)

/**
 * The client SSL certificate settings for the database (Only available in Cloud).
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#pgcertsettings
 */
@Serializable
data class PGCERTSettings (
    /**
     * Environment variable which stores the client certificate.
     */
    var sslcert: FromEnv,

    /**
     * Environment variable which stores the client private key.
     */
    var sslkey: FromEnv,

    /**
     * The SSL connection mode. See the libpq ssl support docs
     * <https://www.postgresql.org/docs/9.1/libpq-ssl.html> for more details.
     */
    var sslmode: String,

    /**
     * Password in the case where the sslkey is encrypted.
     */
    var sslpassword: Sslpassword? = null,

    /**
     * Environment variable which stores trusted certificate authorities.
     */
    var sslrootcert: FromEnv
)

/**
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#fromenv
 *
 * Environment variable which stores the client certificate.
 *
 * Environment variable which stores the client private key.
 *
 * Environment variable which stores trusted certificate authorities.
 */
@Serializable
data class FromEnv (
    /**
     * Name of the environment variable
     */
    @SerialName("from_env")
    var fromEnv: String
)

@Serializable
sealed class Sslpassword {
    class FromEnvValue(val value: FromEnv) : Sslpassword()
    class StringValue(val value: String)   : Sslpassword()
}

@Serializable
enum class PGSourceKind(val value: String) {
    Citus("citus"),
    Postgres("postgres");

    @Serializer(forClass = PGSourceKind::class)
    companion object : KSerializer<PGSourceKind> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("io.hasura.metadata.v3.PGSourceKind", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): PGSourceKind = when (val value = decoder.decodeString()) {
            "citus"    -> Citus
            "postgres" -> Postgres
            else       -> throw IllegalArgumentException("PGSourceKind could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: PGSourceKind) {
            return encoder.encodeString(value.value)
        }
    }
}

@Serializable
data class MSSQLSource (
    var configuration: MSSQLConfiguration,
    var functions: List<CustomFunction>? = null,
    var kind: MSSQLSourceKind,
    var name: String,
    var tables: List<TableEntry>
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#mssqlsourceconnectioninfo
 */
@Serializable
data class MSSQLConfiguration (
    /**
     * Connection parameters for the source
     */
    @SerialName("connection_info")
    var connectionInfo: MSSQLSourceConnectionInfo
)

/**
 * Connection parameters for the source
 *
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#mssqlsourceconnectioninfo
 */
@Serializable
data class MSSQLSourceConnectionInfo (
    /**
     * The database connection string, or as an environment variable
     */
    @SerialName("connection_string")
    var connectionString: Sslpassword,

    /**
     * Connection pool settings
     */
    @SerialName("pool_settings")
    var poolSettings: MSSQLPoolSettings? = null
)

/**
 * Connection pool settings
 *
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#mssqlpoolsettings
 */
@Serializable
data class MSSQLPoolSettings (
    /**
     * The idle timeout (in seconds) per connection (default: 180)
     */
    @SerialName("idle_timeout")
    var idleTimeout: Double? = null,

    /**
     * Maximum number of connections to be kept in the pool (default: 50)
     */
    @SerialName("max_connections")
    var maxConnections: Double? = null
)

@Serializable
enum class MSSQLSourceKind(val value: String) {
    Mssql("mssql");

    @Serializer(forClass = MSSQLSourceKind::class)
    companion object : KSerializer<MSSQLSourceKind> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("io.hasura.metadata.v3.MSSQLSourceKind", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): MSSQLSourceKind = when (val value = decoder.decodeString()) {
            "mssql" -> Mssql
            else    -> throw IllegalArgumentException("MSSQLSourceKind could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: MSSQLSourceKind) {
            return encoder.encodeString(value.value)
        }
    }
}

@Serializable
data class BigQuerySource (
    var configuration: BigQueryConfiguration,
    var functions: List<CustomFunction>? = null,
    var kind: BigQuerySourceKind,
    var name: String,
    var tables: List<TableEntry>
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#bigqueryconfiguration
 */
@Serializable
data class BigQueryConfiguration (
    /**
     * List of BigQuery datasets
     */
    var datasets: Datasets,

    /**
     * Project Id for BigQuery database
     */
    @SerialName("project_id")
    var projectID: Sslpassword,

    /**
     * Service account for BigQuery database
     */
    @SerialName("service_account")
    var serviceAccount: ServiceAccount
)

/**
 * List of BigQuery datasets
 */
@Serializable
sealed class Datasets {
    class FromEnvValue(val value: FromEnv)          : Datasets()
    class StringArrayValue(val value: List<String>) : Datasets()
}

/**
 * Service account for BigQuery database
 */
@Serializable
sealed class ServiceAccount {
    class RecordStringAnyClassValue(val value: RecordStringAnyClass) : ServiceAccount()
    class StringValue(val value: String)                             : ServiceAccount()
}

/**
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#fromenv
 *
 * Environment variable which stores the client certificate.
 *
 * Environment variable which stores the client private key.
 *
 * Environment variable which stores trusted certificate authorities.
 */
@Serializable
data class RecordStringAnyClass (
    /**
     * Name of the environment variable
     */
    @SerialName("from_env")
    var fromEnv: String? = null
)

@Serializable
enum class BigQuerySourceKind(val value: String) {
    Bigquery("bigquery");

    @Serializer(forClass = BigQuerySourceKind::class)
    companion object : KSerializer<BigQuerySourceKind> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("io.hasura.metadata.v3.BigQuerySourceKind", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): BigQuerySourceKind = when (val value = decoder.decodeString()) {
            "bigquery" -> Bigquery
            else       -> throw IllegalArgumentException("BigQuerySourceKind could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: BigQuerySourceKind) {
            return encoder.encodeString(value.value)
        }
    }
}

@Serializable
data class HasuraMetadataV3 (
    var actions: List<Action>? = null,
    var allowlist: List<AllowList>? = null,

    @SerialName("api_limits")
    var apiLimits: APILimits? = null,

    @SerialName("cron_triggers")
    var cronTriggers: List<CronTrigger>? = null,

    @SerialName("custom_types")
    var customTypes: CustomTypes? = null,

    @SerialName("inherited_roles")
    var inheritedRoles: List<InheritedRole>? = null,

    @SerialName("query_collections")
    var queryCollections: List<QueryCollectionEntry>? = null,

    @SerialName("remote_schemas")
    var remoteSchemas: List<RemoteSchema>? = null,

    // This changed from generated code. restEndpoints could actually be null
    @SerialName("rest_endpoints")
    var restEndpoints: List<RESTEndpoint>? = null,

    var sources: List<Source>,
    var version: Int
)

@Serializable
data class APILimits (
    @SerialName("depth_limit")
    var depthLimit: DepthLimit? = null,

    var disabled: Boolean,

    @SerialName("node_limit")
    var nodeLimit: NodeLimit? = null,

    @SerialName("rate_limit")
    var rateLimit: RateLimit? = null
)

@Serializable
data class DepthLimit (
    var global: Double,

    @SerialName("per_role")
    var perRole: Map<String, Double>
)

@Serializable
data class NodeLimit (
    var global: Double,

    @SerialName("per_role")
    var perRole: Map<String, Double>
)

@Serializable
data class RateLimit (
    var global: RateLimitRule,

    @SerialName("per_role")
    var perRole: Map<String, RateLimitRule>
)

@Serializable
data class RateLimitRule (
    @SerialName("max_reqs_per_min")
    var maxReqsPerMin: Double,

    @SerialName("unique_params")
    var uniqueParams: UniqueParams
)

@Serializable
sealed class UniqueParams {
    class EnumValue(val value: UniqueParamsEnum)    : UniqueParams()
    class StringArrayValue(val value: List<String>) : UniqueParams()
    class NullValue()                               : UniqueParams()
}

@Serializable
enum class UniqueParamsEnum(val value: String) {
    IP("IP");

    @Serializer(forClass = UniqueParamsEnum::class)
    companion object : KSerializer<UniqueParamsEnum> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("io.hasura.metadata.v3.UniqueParamsEnum", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): UniqueParamsEnum = when (val value = decoder.decodeString()) {
            "IP" -> IP
            else -> throw IllegalArgumentException("UniqueParamsEnum could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: UniqueParamsEnum) {
            return encoder.encodeString(value.value)
        }
    }
}

@Serializable
data class InheritedRole (
    @SerialName("role_name")
    var roleName: String,

    @SerialName("role_set")
    var roleSet: List<String>
)

@Serializable
data class RESTEndpoint (
    var comment: String? = null,
    var definition: RESTEndpointDefinition,
    var methods: List<Method>,
    var name: String,
    var url: String
)

@Serializable
data class RESTEndpointDefinition (
    var query: Query
)

@Serializable
data class Query (
    @SerialName("collection_name")
    var collectionName: String,

    @SerialName("query_name")
    var queryName: String
)

@Serializable
enum class Method(val value: String) {
    Patch("PATCH"),
    Post("POST"),
    Put("PUT");

    @Serializer(forClass = Method::class)
    companion object : KSerializer<Method> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("io.hasura.metadata.v3.Method", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): Method = when (val value = decoder.decodeString()) {
            "PATCH" -> Patch
            "POST"  -> Post
            "PUT"   -> Put
            else    -> throw IllegalArgumentException("Method could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: Method) {
            return encoder.encodeString(value.value)
        }
    }
}

@Serializable
data class Source (
    var configuration: Configuration,
    var functions: List<CustomFunction>? = null,
    var kind: BackendKind,
    var name: String,
    var tables: List<TableEntry>
)

/**
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#pgconfiguration
 *
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#mssqlsourceconnectioninfo
 *
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#bigqueryconfiguration
 */
@Serializable
data class  Configuration (
    /**
     * Connection parameters for the source
     */
    @SerialName("connection_info")
    var connectionInfo: SourceConnectionInfo? = null,

    /**
     * Optional list of read replica configuration (supported only in cloud/enterprise versions)
     */
    @SerialName("read_replicas")
    var readReplicas: List<PGSourceConnectionInfo>? = null,

    /**
     * List of BigQuery datasets
     */
    var datasets: Datasets? = null,

    /**
     * Project Id for BigQuery database
     */
    @SerialName("project_id")
    var projectID: Sslpassword? = null,

    /**
     * Service account for BigQuery database
     */
    @SerialName("service_account")
    var serviceAccount: ServiceAccount? = null
)

/**
 * Connection parameters for the source
 *
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#pgsourceconnectioninfo
 *
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#mssqlsourceconnectioninfo
 */
@Serializable
data class SourceConnectionInfo (
    /**
     * The database connection URL as a string, as an environment variable, or as connection
     * parameters.
     */
    @SerialName("database_url")
    var databaseURL: DatabaseURL? = null,

    /**
     * The transaction isolation level in which the queries made to the source will be run with
     * (default: read-committed).
     */
    @SerialName("isolation_level")
    var isolationLevel: IsolationLevel? = null,

    /**
     * Connection pool settings
     */
    @SerialName("pool_settings")
    var poolSettings: PoolSettings? = null,

    /**
     * The client SSL certificate settings for the database (Only available in Cloud).
     */
    @SerialName("ssl_configuration")
    var sslConfiguration: PGCERTSettings? = null,

    /**
     * If set to true the server prepares statement before executing on the source database
     * (default: false). For more details, refer to the Postgres docs
     */
    @SerialName("use_prepared_statements")
    var usePreparedStatements: Boolean? = null,

    /**
     * The database connection string, or as an environment variable
     */
    @SerialName("connection_string")
    var connectionString: Sslpassword? = null
)

/**
 * Connection pool settings
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#pgpoolsettings
 *
 *
 * https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#mssqlpoolsettings
 */
@Serializable
data class PoolSettings (
    /**
     * Time from connection creation after which the connection should be destroyed and a new
     * one created. A value of 0 indicates we should never destroy an active connection. If 0 is
     * passed, memory from large query results may not be reclaimed. (default: 600 sec)
     */
    @SerialName("connection_lifetime")
    var connectionLifetime: Double? = null,

    /**
     * The idle timeout (in seconds) per connection (default: 180)
     */
    @SerialName("idle_timeout")
    var idleTimeout: Double? = null,

    /**
     * Maximum number of connections to be kept in the pool (default: 50)
     */
    @SerialName("max_connections")
    var maxConnections: Double? = null,

    /**
     * Maximum time to wait while acquiring a Postgres connection from the pool, in seconds
     * (default: forever)
     */
    @SerialName("pool_timeout")
    var poolTimeout: Double? = null,

    /**
     * Number of retries to perform (default: 1)
     */
    var retries: Double? = null
)

@Serializable
enum class BackendKind(val value: String) {
    Bigquery("bigquery"),
    Citus("citus"),
    Mssql("mssql"),
    Postgres("postgres");

    @Serializer(forClass = BackendKind::class)
    companion object : KSerializer<BackendKind> {
        override val descriptor: SerialDescriptor get() {
            return PrimitiveSerialDescriptor("io.hasura.metadata.v3.BackendKind", PrimitiveKind.STRING)
        }
        override fun deserialize(decoder: Decoder): BackendKind = when (val value = decoder.decodeString()) {
            "bigquery" -> Bigquery
            "citus"    -> Citus
            "mssql"    -> Mssql
            "postgres" -> Postgres
            else       -> throw IllegalArgumentException("BackendKind could not parse: $value")
        }
        override fun serialize(encoder: Encoder, value: BackendKind) {
            return encoder.encodeString(value.value)
        }
    }
}
