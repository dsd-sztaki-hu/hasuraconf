package com.beepsoft.hasuraconf
import com.beepsoft.hasuraconf.annotation.*
import io.hasura.metadata.v3.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import net.pearx.kasechange.toCamelCase
import org.hibernate.dialect.PostgreSQL9Dialect
import org.hibernate.metamodel.spi.MetamodelImplementor
import org.hibernate.persister.entity.AbstractEntityPersister
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.scanners.Scanners.*
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.math.BigDecimal
import java.math.BigInteger
import java.sql.Timestamp
import java.sql.Types
import java.util.*
import javax.persistence.Entity
import kotlin.reflect.jvm.kotlinFunction
import kotlin.reflect.jvm.kotlinProperty


/**
 * Based on [HasuraAction] annotations generates
 */
class HasuraActionGenerator(
    val metaModel: MetamodelImplementor? = null
) {

    companion object {
        public val LOG = getLogger(this::class.java.enclosingClass)
    }

    enum class TypeDefinitionKind {
        INPUT,
        OUTPUT
    }

    data class ActionsAndCustomTypes(
        val actions: List<Action>,
        val customTypes: CustomTypes
    )

    // https://docs.oracle.com/cd/E19830-01/819-4721/beajw/index.html
    // 	Java Type            JDBC Type          Nullability
    //	boolean              BIT                No
    //	java.lang.Boolean    BIT                Yes
    //	byte                 TINYINT            No
    //	java.lang.Byte       TINYINT            Yes
    //	double               DOUBLE             No
    //	java.lang.Double     DOUBLE             Yes
    //	float                REAL               No
    //	java.lang.Float      REAL               Yes
    //	int                  INTEGER            No
    //	java.lang.Integer    INTEGER            Yes
    //	long                 BIGINT             No
    //	java.lang.Long       BIGINT             Yes
    //	short                SMALLINT           No
    //	java.lang.Short      SMALLINT           Yes
    //	java.math.BigDecimalDECIMAL             Yes
    //	java.math.BigInteger DECIMAL            Yes
    //	char                 CHAR               No
    //	java.lang.Character CHAR                Yes
    //	java.lang.String     VARCHAR or CLOBYes
    //	Serializable         BLOB               Yes
    //	byte[]               BLOB               Yes
    //	                     DATE (Oracle only)
    //	                     TIMESTAMP (all
    //	java.util.Date       other databases)   Yes
    //	java.sql.Date        DATE               Yes
    //	java.sql.Time        TIME               Yes
    //	java.sql.Timestamp TIMESTAMP            Yes

    // Defines how java classes are mapped to JDBC types
    val javaTypeToJDBCype = mapOf(
        Boolean::class.java                 to Types.BIT,
        java.lang.Boolean::class.java       to Types.BIT,
        Byte::class.java                    to Types.TINYINT,
        java.lang.Byte::class.java          to Types.TINYINT,
        Double::class.java                  to Types.DOUBLE,
        java.lang.Double::class.java        to Types.DOUBLE,
        Float::class.java                   to Types.REAL,
        java.lang.Float::class.java         to Types.REAL,
        Int::class.java                     to Types.INTEGER,
        java.lang.Integer::class.java       to Types.INTEGER,
        Long::class.java                    to Types.BIGINT,
        java.lang.Long::class.java          to Types.BIGINT,
        Short::class.java                   to Types.SMALLINT,
        java.lang.Short::class.java         to Types.SMALLINT,
        BigDecimal::class.java              to Types.DECIMAL,
        BigInteger::class.java              to Types.DECIMAL,
        Char::class.java                    to Types.CHAR,
        java.lang.Character::class.java     to Types.CHAR,
        String::class.java                  to Types.VARCHAR,
        Date::class.java                    to Types.DATE,
        java.sql.Date::class.java           to Types.DATE,
        java.sql.Time::class.java           to Types.TIME,
        java.sql.Timestamp::class.java      to Types.TIMESTAMP,
        // Not sure how exact this is, but dialect.getTypeName(1111) returns 'uuid'
        java.util.UUID::class.java          to Types.OTHER
    )

    // dialect.getTypeName() defines how JDBC types are mapped to actual DB types
    val dialect = PostgreSQL9Dialect()
    // 		registerColumnType( Types.BIT, "bool" );
    //		registerColumnType( Types.BIGINT, "int8" );
    //		registerColumnType( Types.SMALLINT, "int2" );
    //		registerColumnType( Types.TINYINT, "int2" );
    //		registerColumnType( Types.INTEGER, "int4" );
    //		registerColumnType( Types.CHAR, "char(1)" );
    //		registerColumnType( Types.VARCHAR, "varchar($l)" );
    //		registerColumnType( Types.FLOAT, "float4" );
    //		registerColumnType( Types.DOUBLE, "float8" );
    //		registerColumnType( Types.DATE, "date" );
    //		registerColumnType( Types.TIME, "time" );
    //		registerColumnType( Types.TIMESTAMP, "timestamp" );
    //		registerColumnType( Types.VARBINARY, "bytea" );
    //		registerColumnType( Types.BINARY, "bytea" );
    //		registerColumnType( Types.LONGVARCHAR, "text" );
    //		registerColumnType( Types.LONGVARBINARY, "bytea" );
    //		registerColumnType( Types.CLOB, "text" );
    //		registerColumnType( Types.BLOB, "oid" );
    //		registerColumnType( Types.NUMERIC, "numeric($p, $s)" );
    //		registerColumnType( Types.OTHER, "uuid" );

    // Uses HasuraConfigurator.postgresqlNames to map from Java type to JDBC type to SQL type to postgresql name
    // for the type
    inline fun getHasuraTypeOf(clazz: Class<*>) =
        HasuraConfigurator.postgresqlNames[dialect.getTypeName(javaTypeToJDBCype[clazz] as Int)]

    inline fun isBuiltinGraphqlType(type: String) =
        when(type) {
            "Int",
            "Boolean",
            "Date",
            "Float",
            "String" -> true
            else -> false
        }

    val actions = mutableMapOf<String, Action>()
    val inputTypes = mutableMapOf<String, JsonObject>()
    val outputTypes = mutableMapOf<String, JsonObject>()
    val scalars = mutableMapOf<String, JsonObject>()
    val enums = mutableMapOf<String, JsonObject>()

    fun configureActions(roots: List<String>): ActionsAndCustomTypes {
        roots.forEach { root ->
            val reflections = Reflections(root, Scanners.values())

            val actionMethods: Set<Method> = reflections.get(
                MethodsAnnotated.with(HasuraAction::class.java).`as`(
                    Method::class.java
                )
            )

            actionMethods.forEach { method ->
                val annot = method.getDeclaredAnnotation(HasuraAction::class.java)

                actions.put(generateActionName(method, annot), generateAction(method, annot))
            }
        }

        return ActionsAndCustomTypes(
            actions = actions.values.toList(),
            // For now we keep the custom types in JsonObject-s and we create metadata objects from
            // these
            customTypes = CustomTypes(
                inputObjects = inputTypes.values.map{ Json.decodeFromJsonElement(it) },
                objects = outputTypes.values.map{ Json {ignoreUnknownKeys=true }.decodeFromJsonElement(it) },
                enums = enums.values.map{ Json.decodeFromJsonElement(it) },
                scalars = scalars.values.map{ Json.decodeFromJsonElement(it) }
            )
        )
    }

    /**
     * Generate action definition for the given method.
     */
    private fun generateAction(method: Method, annot: HasuraAction) : Action {

        if (annot.handler.isEmpty()) {
            throw HasuraConfiguratorException("handler is not set for @HasuraAction on method $method")
        }

        val action = Action(
            name = generateActionName(method, annot),
            comment = if (annot.comment.isNotEmpty()) annot.comment else null,
            definition = ActionDefinition(
                handler = annot.handler,
                type = ActionDefinitionType.valueOf(annot.type.name),
                kind = annot.kind.name.toLowerCase(),
                forwardClientHeaders = annot.forwardClientHeaders,
                timeout = if (annot.timeout != 0L) annot.timeout else null,
                headers = generateActionHeaders(annot),
                requestTransform = generateRequestTransform(annot.requestTransform),
                responseTransform = generateResponseTransform(annot.responseTransform),
                outputType = generateOutputType(method, annot),
                arguments = generateArguments(method, annot),
            ),
            permissions = when {
                annot.permissions.size > 0 -> annot.permissions.map { role -> Permission(role) }
                else -> null
            }
        )

        return action
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun generateArguments(method: Method, annot: HasuraAction): List<InputArgument> {
        // Generate input args and their types
        if (annot.wrapArgsInType) {
            TODO()
        }
        else {
            return method.parameters.filter { !it.isAnnotationPresent(HasuraIgnoreParameter::class.java)}.mapIndexed { ix, p ->
                var nullable: Boolean? = null
                method.kotlinFunction?.let {
                    nullable = it.parameters[ix].type.isMarkedNullable
                }

                val fieldAnnot = p.getDeclaredAnnotation(HasuraField::class.java)
                var fieldName = p.name
                // May override default name with annotation value
                if (fieldAnnot != null) {
                    nullable = if (fieldAnnot.nullable != Nullable.UNSET) fieldAnnot.nullable.name.toLowerCase().toBoolean() else nullable
                    if (fieldAnnot.value.isNotEmpty()) {
                        fieldName = fieldAnnot.value
                    }
                    else if (fieldAnnot.name.isNotEmpty()) {
                        fieldName = fieldAnnot.name
                    }
                }

                var typeForAnnot = p.type
                if (typeForAnnot.isArray) {
                    typeForAnnot = typeForAnnot.componentType
                }

                val typeName = calcExplicitName(typeForAnnot.getAnnotation(HasuraType::class.java), fieldAnnot)
                if (nullable == null) {
                    nullable = true
                }

                InputArgument(
                    name = fieldName,
                    type = generateParameterTypeDefinition(p.type, typeName) + if(typeName == null && !nullable!!) "!" else ""
                )
            }
        }

    }

    private fun generateOutputType(method: Method, annot: HasuraAction): String {
        // Generate and set output type
        // Use the class's name as the default
        var outputType = method.returnType
        if (annot.outputType != Void::class) {
            // output tye may be overriden by annotation
            outputType = annot.outputType.java
        }
        var typeForTypeName = outputType
        if (outputType.isArray) {
            typeForTypeName = typeForTypeName.componentType
        }
        var returnTypeName = typeForTypeName.simpleName
        // If class has a @HasuraType annotation, use the value/name from there
        val hasuraTypeAnnot = typeForTypeName.getAnnotation(HasuraType::class.java)
        if (hasuraTypeAnnot != null) {
            if (hasuraTypeAnnot.value.isNotEmpty()) {
                returnTypeName = hasuraTypeAnnot.value
            }
            else if (hasuraTypeAnnot.name.isNotEmpty()) {
                returnTypeName = hasuraTypeAnnot.name
            }
        }
        // @HasuraAction(outputTypeName=...) may override @HasuraType
        if (annot.outputTypeName.isNotEmpty()) {
            returnTypeName = annot.outputTypeName
        }
        // Now we have the final returnTypeName
        return generateReturnTypeDefinition(outputType, returnTypeName)
    }

    private fun generateActionHeaders(annot: HasuraAction)  =
            if (annot.headers.isNotEmpty()) {
                annot.headers.map { h ->
                    Header(
                        name = h.name,
                        value = if (h.value.isNotEmpty()) h.value else null,
                        valueFromEnv = if (h.valueFromEnv.isNotEmpty()) h.value else null
                    )
                }
            }
            else null

    @OptIn(ExperimentalStdlibApi::class)
    private fun generateResponseTransform(annot: HasuraResponseTransform): ResponseTransformation?
    {
        if (annot.body.isEmpty()) {
            return null
        }
        return ResponseTransformation(
            body = BodyTransform.StringValue(annot.body),
            templateEngne = annot.templateEngine
        )
    }

    // TODO: this might be usable for event transforms as well
    @OptIn(ExperimentalStdlibApi::class)
    private fun generateRequestTransform(annot: HasuraRequestTransform): RequestTransformation?
    {
        // If not set, then nothing to do
        if (annot.url == "<<<empty>>>") {
            return null
        }

        return RequestTransformation(
            version = 1,
            body = BodyTransform.StringValue(annot.body.replace("\\s".toRegex(), "")),
            url = annot.url,
            method = annot.method.name,
            queryParams = buildMap {
                annot.queryParams.forEach {
                    put(it.key, it.value)
                }
            },
            templateEngne = annot.templateEngine,
            requestHeaders = let {
                var h: TransformHeaders? = null
                if (annot.requestHeaders.addHeaders.size != 0) {
                    h = TransformHeaders()
                    h.addHeaders = buildMap {
                        annot.requestHeaders.addHeaders.forEach {
                            put(it.name, it.value)
                        }
                    }
                }
                if (annot.requestHeaders.removeHeaders.size != 0) {
                    if (h == null) {
                        h = TransformHeaders()
                    }
                    h.removeHeaders = annot.requestHeaders.removeHeaders.toList()
                }
                h
            }
        )
    }

    private fun calcExplicitName(hasuraType: HasuraType?, hasuraField: HasuraField?): String?
    {
        var explicitName: String? = null
        if (hasuraType != null) {
            if (hasuraType.value.isNotEmpty()) {
                explicitName = hasuraType.value
            }
            else if (hasuraType.name.isNotEmpty()) {
                explicitName = hasuraType.name
            }
        }

        // The @HasuraField annotation may override this
        if (hasuraField != null && hasuraField.type.isNotEmpty()) {
            explicitName = hasuraField.type
        }
        return explicitName
    }

    private inline fun generateActionName(method: Method, annot: HasuraAction) =
        if (annot.name.isNotEmpty()) annot.name else method.name

    /**
     * Generate input type name based on the method args
     */
    private fun generateWrappedInputTypeName(method: Method) : String? {
        if (method.parameterCount == 0) {
            return null
        }

        return method.name.toCamelCase()+"InputType"
    }

    /**
     * Generates type definition for the parameter and stores it in inputTypes. Returns the parameter's type name
     */
    private fun generateParameterTypeDefinition(type: Class<*>, explicitName: String?) : String {
        return generateTypeDefinition(type, explicitName, TypeDefinitionKind.INPUT)
    }

    private fun generateReturnTypeDefinition(type: Class<*>, explicitName: String?) : String {
        return generateTypeDefinition(type, explicitName, TypeDefinitionKind.OUTPUT)
    }

    private inline fun isSimpleType(type: Class<*>): Boolean {
        return (type.isPrimitive
                || type == Boolean::class.javaObjectType
                || type == Boolean::class.java
                || type == String::class.javaObjectType
                || type == String::class.java
                || type == Date::class.java
                || type == Timestamp::class.java
                || type == UUID::class.java
                || Number::class.java.isAssignableFrom(type)
        )
    }

    /**
     * Generates type name for an action argument (input type) or return type (output type) and at the same type
     * generates a definition of the type if necessary into inputTypes, outputTypes, scalars or enums. These are
     * now generated as JsonObjects, which are later decoded to actual metadata objects when stored in an
     * ActionsAndCustomTypes object
     */
    private fun generateTypeDefinition(type: Class<*>, explicitName: String?, kind: TypeDefinitionKind, field: Field? = null, fieldAnnot: HasuraField? = null, failForOutputTypeRecursion: Boolean? = false) : String {
        if (isSimpleType(type)) {
            var typeName = explicitName ?: getHasuraTypeOf(type)!!
            fieldAnnot?.let {
                var cleanTypeName = typeName.replace("!", "")
                addOptionalScalar(cleanTypeName, fieldAnnot)
            }
            if ((type == Date::class.java || type == Timestamp::class.java) &&
                (fieldAnnot == null || fieldAnnot?.type!!.isEmpty())) {
                LOG.warn("""
                    {$field} has a date type, which is mapped to graphql Date by default, however in case of Hasura you
                    may want to user another type like timestamptz or timetz. You can specificy it using 
                    @HasuraField(type="..."). See https://hasura.io/blog/working-with-dates-time-timezones-graphql-postgresql/                    
                """.trimIndent())
            }
            return typeName
        }
        var actualTypeName = explicitName ?: type.simpleName
        if (type.isEnum) {
            actualTypeName = calcEnumTypeName(type, actualTypeName, explicitName)
            return generateEnum(type, actualTypeName, kind)
        }

        var actualType = type
        if (type.isArray) {
            val compoType = type.componentType
            actualTypeName = explicitName ?: compoType.simpleName
            if (isSimpleType(compoType)) {
                // here we work with explicitName and not actualTypeName
                return "[" + (explicitName ?: getHasuraTypeOf(compoType)!!) + "!]"
            }
            else if (compoType.isEnum) {
                actualTypeName = calcEnumTypeName(compoType, actualTypeName, explicitName)
                return "[" + generateEnum(compoType, actualTypeName, kind) + "!]"
            }
            else if (failForOutputTypeRecursion != null && failForOutputTypeRecursion && kind == TypeDefinitionKind.OUTPUT) {
                throw HasuraConfiguratorException("Invalid return type $compoType. Return types should be primitive/enum type or a class made up of primitive/enum type fields")
            }
            actualType = compoType
        }
        if (Collection::class.java.isAssignableFrom(actualType)) {
                throw HasuraConfiguratorException("Invalid type $actualType. Collection classes cannot be used in Hasura action input and output types")
        }

        generateTypeDefinitionForClassOld(actualType, actualTypeName, kind)

        if (type.isArray) {
            return "[$actualTypeName!]"
        }

        return "$actualTypeName"
    }

    private fun calcEnumTypeName(type: Class<*>, actualTypeName: String, explicitName: String?) : String
    {
        var result = actualTypeName

        // For @HasuraEnum annotated classes we cannot use the name derived from the classname, but we either need
        // to have an HasuraEnum or the name must be derived from the table name, since we already have a definition
        // for the enum generated for the ain graphql definitions, and we have to repeat it here
        if (type.isAnnotationPresent(HasuraEnum::class.java)) {
            if (explicitName == null) {
                if (metaModel != null) {
                    val targetEntityClassMetadata = metaModel.entityPersister(type.typeName) as AbstractEntityPersister
                    result = targetEntityClassMetadata.tableName+"_enum"
                }
                else {
                    throw HasuraConfiguratorException("metaModel must be provided when using @HasuraEnum annotated enums without an explicit type")
                }
            }
        }
        return result
    }

    /**
     * kind is either "input" or "output"
     */
    private fun generateTypeDefinitionForClassOld(t: Class<*>, typeName: String, kind: TypeDefinitionKind): JsonObject {
        // Don't generate if already exists
        when(kind) {
            TypeDefinitionKind.INPUT -> if (inputTypes.contains(typeName)) return inputTypes[typeName]!!
            TypeDefinitionKind.OUTPUT -> if (outputTypes.contains(typeName)) return outputTypes[typeName]!!
        }

        val typeDef = buildJsonObject {
            put("name", typeName)
            val hasuraType = t.getAnnotation(HasuraType::class.java)
            if (hasuraType != null && hasuraType.description.isNotEmpty()) {
                put("description", hasuraType.description)
            }

            val relationshipFields = mutableListOf<Field>()
            putJsonArray("fields") {
                t.declaredFields.forEachIndexed { ix, field ->
                    // @HasuraIgnoreField marks a runtime field, which should not be exposed to graphql
                    if (field.isAnnotationPresent(HasuraIgnoreField::class.java)) {
                        return@forEachIndexed
                    }
                    // Some implicitly ignored fields:
                    // - Companion: Kotlin generates this for @Serializable classes, so users cannot put an explicit
                    //  @HasuraIgnoreField annotation on it, so we ignore it here
                    if (listOf("Companion").contains(field.name)) {
                        return@forEachIndexed
                    }
                    addJsonObject {
                        var nullable: Boolean? = null
                        field.kotlinProperty?.let {
                            nullable = it.returnType.isMarkedNullable
                        }

                        val hasuraFieldAnnot = field.getAnnotation(HasuraField::class.java)
                        var name = field.name
                        if (hasuraFieldAnnot != null) {
                            nullable = if (hasuraFieldAnnot.nullable != Nullable.UNSET) hasuraFieldAnnot.nullable.name.toLowerCase().toBoolean() else nullable
                            if (hasuraFieldAnnot.value.isNotEmpty()) {
                                name = hasuraFieldAnnot.value
                            }
                            else if (hasuraFieldAnnot.name.isNotEmpty()) {
                                name = hasuraFieldAnnot.name
                            }

                            if (hasuraFieldAnnot.description.isNotEmpty()) {
                                put("description", hasuraFieldAnnot.description)
                            }
                        }

                        var fieldType = field.type


                        // fieldType is a class, so recurse. Note: this can now only be an input type at this point,
                        // output types cannot recurse

                        // Get type name override from the field's type
                        var explicitFieldTypeName: String? = calcExplicitName(
                            fieldType.getAnnotation(HasuraType::class.java),
                            hasuraFieldAnnot
                        )

                        // Collect fields with re;lationships, these will be generated later
                        val relationship = field.getAnnotation(HasuraRelationship::class.java)
                        if (relationship != null) {
                            relationshipFields.add(field)
                        }

                        // If field references a class and has a @HasuraRelationship then we won't recurse as the
                        // type is only here for relationship reference, no graphql type will be generated for it.
                        // Here we set name and graphqlType either with explicit values from  @HasuraRelationship
                        // or calc from Hibernate mappings
                        val classType = getClassType(fieldType)
                        var graphqlType: String? = null
                        var explicitGraphqlType = false
                        if (classType != null) {
                            if (relationship != null) {
                                if (relationship.graphqlFieldName.isNotEmpty()) {
                                    name = relationship.graphqlFieldName
                                }
                                else {
                                    name = field.name+"Id"
                                }
                                if (relationship.graphqlFieldType.isEmpty()) {
                                    if (metaModel != null) {
                                        val entity = metaModel.entity(fieldType)
                                        val targetEntityClassMetadata = metaModel.entityPersister(entity.javaType.typeName) as AbstractEntityPersister
                                        graphqlType = HasuraConfigurator.graphqlTypeFor(metaModel, targetEntityClassMetadata.identifierType, targetEntityClassMetadata)
                                    }
                                    else {
                                        throw HasuraConfiguratorException("graphqlFieldType must be specified for a @HasuraRelationship referring to an object type type on field $field")
                                    }
                                }
                                else {
                                    graphqlType = relationship.graphqlFieldType
                                    explicitGraphqlType = true
                                }
                            }
                            else {
                                if (classType.isAnnotationPresent(Entity::class.java)) {
                                    throw HasuraConfiguratorException("Hasura managed entity class $classType must have a @HasuraRelationship annotation")
                                }
                            }
                        }

                        put("name", name)
                        // If graphqlType has not been set as the result of @HasuraRelationship, then generate it now
                        if (graphqlType == null) {
                            graphqlType = generateTypeDefinition(fieldType, explicitFieldTypeName, kind, field, hasuraFieldAnnot, true)
                        }
                        else {
                            hasuraFieldAnnot?.let {
                                addOptionalScalar(graphqlType, hasuraFieldAnnot)
                            }
                        }
                        // If not set default to nullable
                        if (nullable == null) {
                            nullable = true
                        }
                        put("type", graphqlType + if (!explicitGraphqlType && explicitFieldTypeName == null && !nullable!!) "!" else "")
                    }
                }
            }

            if (relationshipFields.isNotEmpty()) {
                putJsonArray("relationships") {
                    relationshipFields.forEach { field ->
                        val annot = field.getAnnotation(HasuraRelationship::class.java)

                        //
                        // TODO: Handle relationships to Hasura managed entities.
                        //
                        //  Figure out remote table, refercne ID, etc. based on entityManager. Later these migght be
                        //  overriden by @HasuraRelationship values
                        var fieldType = field.type
                        var relationshipType: HasuraRelationshipType? = null
                        if (fieldType.isArray) {
                            fieldType = fieldType.componentType
                        }
                        var remoteTable: String? = null
                        var graphqlFieldName: String? = null
                        var fromId: String? = null
                        var toId: String? = null
                        var hasuraManagedEntityRelationship = false
                        if (metaModel != null && fieldType.isAnnotationPresent(Entity::class.java)) {
                            hasuraManagedEntityRelationship = true
                            val entity = metaModel.entity(fieldType)
                            val targetEntityClassMetadata = metaModel.entityPersister(entity.javaType.typeName) as AbstractEntityPersister
                            remoteTable = targetEntityClassMetadata.tableName
                            toId = targetEntityClassMetadata.keyColumnNames[0]
                            graphqlFieldName = field.name.toCamelCase()+"Id"
                            fromId = graphqlFieldName
                            relationshipType = if(field.type.isArray) HasuraRelationshipType.ARRAY
                                                else HasuraRelationshipType.OBJECT
                        }
                        else {
                            //
                            // Handle explicitly configured relations ships
                            //

                            // Sanity check
                            if (annot.fieldMappings.isEmpty()) {
                                throw HasuraConfiguratorException("@HasuraRelationship.fieldMappings vannot be empty for field $field")
                            }
                            if (annot.remoteTable.isEmpty()) {
                                throw HasuraConfiguratorException("@HasuraRelationship.tableName is not specified for field $field")
                            }
                        }

                        addJsonObject {
                            put("name", if (annot.name.isNotEmpty()) annot.name else field.name)
                            put("source", annot.source)
                            // If we have a calculated value for a hasuraManagedEntityRelationship then use that
                            if (relationshipType != null) {
                                put("type", relationshipType.name.toLowerCase())
                            }
                            else {
                                put("type", annot.type.name.toLowerCase())
                            }
                            putJsonObject("remote_table") {
                                val schema = if(annot.remoteSchema.isNotEmpty()) annot.remoteSchema else "public"
                                val tableName = if (remoteTable != null)  remoteTable else annot.remoteTable
                                val schemaAndName = actualSchemaAndName(schema, tableName)
                                put("schema", schemaAndName.first)
                                put("name", schemaAndName.second)
                            }
                            putJsonObject("field_mapping") {
                                if (hasuraManagedEntityRelationship) {
                                    put(fromId!!, toId!!)
                                }
                                else {
                                    annot.fieldMappings.forEach {mapping ->
                                        put(mapping.fromField, mapping.toField)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Save in either inputTypes or outputTypes
        when(kind) {
            TypeDefinitionKind.INPUT -> inputTypes[typeName] = typeDef
            TypeDefinitionKind.OUTPUT -> outputTypes[typeName] = typeDef
        }

        return typeDef
    }

    private fun generateTypeDefinitionForClass(t: Class<*>, typeName: String, kind: TypeDefinitionKind): JsonObject {
        // Don't generate if already exists
        when(kind) {
            TypeDefinitionKind.INPUT -> if (inputTypes.contains(typeName)) return inputTypes[typeName]!!
            TypeDefinitionKind.OUTPUT -> if (outputTypes.contains(typeName)) return outputTypes[typeName]!!
        }

        val typeDef = buildJsonObject {
            put("name", typeName)
            val hasuraType = t.getAnnotation(HasuraType::class.java)
            if (hasuraType != null && hasuraType.description.isNotEmpty()) {
                put("description", hasuraType.description)
            }

            val relationshipFields = mutableListOf<Field>()
            putJsonArray("fields") {
                t.declaredFields.forEachIndexed { ix, field ->
                    // @HasuraIgnoreField marks a runtime field, which should not be exposed to graphql
                    if (field.isAnnotationPresent(HasuraIgnoreField::class.java)) {
                        return@forEachIndexed
                    }
                    // Some implicitly ignored fields:
                    // - Companion: Kotlin generates this for @Serializable classes, so users cannot put an explicit
                    //  @HasuraIgnoreField annotation on it, so we ignore it here
                    if (listOf("Companion").contains(field.name)) {
                        return@forEachIndexed
                    }
                    addJsonObject {
                        var nullable: Boolean? = null
                        field.kotlinProperty?.let {
                            nullable = it.returnType.isMarkedNullable
                        }

                        val hasuraFieldAnnot = field.getAnnotation(HasuraField::class.java)
                        var name = field.name
                        if (hasuraFieldAnnot != null) {
                            nullable = if (hasuraFieldAnnot.nullable != Nullable.UNSET) hasuraFieldAnnot.nullable.name.toLowerCase().toBoolean() else nullable
                            if (hasuraFieldAnnot.value.isNotEmpty()) {
                                name = hasuraFieldAnnot.value
                            }
                            else if (hasuraFieldAnnot.name.isNotEmpty()) {
                                name = hasuraFieldAnnot.name
                            }

                            if (hasuraFieldAnnot.description.isNotEmpty()) {
                                put("description", hasuraFieldAnnot.description)
                            }
                        }

                        var fieldType = field.type


                        // fieldType is a class, so recurse. Note: this can now only be an input type at this point,
                        // output types cannot recurse

                        // Get type name override from the field's type
                        var explicitFieldTypeName: String? = calcExplicitName(
                            fieldType.getAnnotation(HasuraType::class.java),
                            hasuraFieldAnnot
                        )

                        // Collect fields with re;lationships, these will be generated later
                        val relationship = field.getAnnotation(HasuraRelationship::class.java)
                        if (relationship != null) {
                            relationshipFields.add(field)
                        }

                        // If field references a class and has a @HasuraRelationship then we won't recurse as the
                        // type is only here for relationship reference, no graphql type will be generated for it.
                        // Here we set name and graphqlType either with explicit values from  @HasuraRelationship
                        // or calc from Hibernate mappings
                        val classType = getClassType(fieldType)
                        var graphqlType: String? = null
                        if (classType != null) {
                            if (relationship != null) {
                                if (relationship.graphqlFieldName.isNotEmpty()) {
                                    name = relationship.graphqlFieldName
                                }
                                else {
                                    name = field.name+"Id"
                                }
                                if (relationship.graphqlFieldType.isEmpty()) {
                                    if (metaModel != null) {
                                        val entity = metaModel.entity(fieldType)
                                        val targetEntityClassMetadata = metaModel.entityPersister(entity.javaType.typeName) as AbstractEntityPersister
                                        graphqlType = HasuraConfigurator.graphqlTypeFor(metaModel, targetEntityClassMetadata.identifierType, targetEntityClassMetadata)
                                    }
                                    else {
                                        throw HasuraConfiguratorException("graphqlFieldType must be specified for a @HasuraRelationship referring to an object type type on field $field")
                                    }
                                }
                                else {
                                    graphqlType = relationship.graphqlFieldType
                                }
                            }
                            else {
                                if (classType.isAnnotationPresent(Entity::class.java)) {
                                    throw HasuraConfiguratorException("Hasura managed entity class $classType must have a @HasuraRelationship annotation")
                                }
                            }
                        }

                        put("name", name)
                        val explicitType = graphqlType != null
                        // If graphqlType has not been set as the result of @HasuraRelationship, then generate it now
                        if (graphqlType == null) {
                            graphqlType = generateTypeDefinition(fieldType, explicitFieldTypeName, kind, field, hasuraFieldAnnot, true)
                        }
                        else {
                            hasuraFieldAnnot?.let {
                                addOptionalScalar(graphqlType, hasuraFieldAnnot)
                            }
                        }
                        // If not set default to nullable
                        if (nullable == null) {
                            nullable = true
                        }
                        put("type", graphqlType + if (!explicitType && !nullable!!) "!" else "")
                    }
                }
            }

            if (relationshipFields.isNotEmpty()) {
                putJsonArray("relationships") {
                    relationshipFields.forEach { field ->
                        val annot = field.getAnnotation(HasuraRelationship::class.java)

                        //
                        // TODO: Handle relationships to Hasura managed entities.
                        //
                        //  Figure out remote table, refercne ID, etc. based on entityManager. Later these migght be
                        //  overriden by @HasuraRelationship values
                        var fieldType = field.type
                        var relationshipType: HasuraRelationshipType? = null
                        if (fieldType.isArray) {
                            fieldType = fieldType.componentType
                        }
                        var remoteTable: String? = null
                        var graphqlFieldName: String? = null
                        var fromId: String? = null
                        var toId: String? = null
                        var hasuraManagedEntityRelationship = false
                        if (metaModel != null && fieldType.isAnnotationPresent(Entity::class.java)) {
                            hasuraManagedEntityRelationship = true
                            val entity = metaModel.entity(fieldType)
                            val targetEntityClassMetadata = metaModel.entityPersister(entity.javaType.typeName) as AbstractEntityPersister
                            remoteTable = targetEntityClassMetadata.tableName
                            toId = targetEntityClassMetadata.keyColumnNames[0]
                            graphqlFieldName = field.name.toCamelCase()+"Id"
                            fromId = graphqlFieldName
                            relationshipType = if(field.type.isArray) HasuraRelationshipType.ARRAY
                            else HasuraRelationshipType.OBJECT
                        }
                        else {
                            //
                            // Handle explicitly configured relations ships
                            //

                            // Sanity check
                            if (annot.fieldMappings.isEmpty()) {
                                throw HasuraConfiguratorException("@HasuraRelationship.fieldMappings vannot be empty for field $field")
                            }
                            if (annot.remoteTable.isEmpty()) {
                                throw HasuraConfiguratorException("@HasuraRelationship.tableName is not specified for field $field")
                            }
                        }

                        addJsonObject {
                            put("name", if (annot.name.isNotEmpty()) annot.name else field.name)
                            // If we have a calculated value for a hasuraManagedEntityRelationship then use that
                            if (relationshipType != null) {
                                put("type", relationshipType.name.toLowerCase())
                            }
                            else {
                                put("type", annot.type.name.toLowerCase())
                            }
                            putJsonObject("remote_table") {
                                val schema = if(annot.remoteSchema.isNotEmpty()) annot.remoteSchema else "public"
                                val tableName = if (remoteTable != null)  remoteTable else annot.remoteTable
                                val schemaAndName = actualSchemaAndName(schema, tableName)
                                put("schema", schemaAndName.first)
                                put("name", schemaAndName.second)
                            }
                            putJsonObject("field_mapping") {
                                if (hasuraManagedEntityRelationship) {
                                    put(fromId!!, toId!!)
                                }
                                else {
                                    annot.fieldMappings.forEach {mapping ->
                                        put(mapping.fromField, mapping.toField)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Save in either inputTypes or outputTypes
        when(kind) {
            TypeDefinitionKind.INPUT -> inputTypes[typeName] = typeDef
            TypeDefinitionKind.OUTPUT -> outputTypes[typeName] = typeDef
        }

        return typeDef
    }

    private fun addOptionalScalar(graphqlType: String, hasuraFieldAnnot: HasuraField)
    {
        var cleanType = graphqlType.replace("!", "")
        if (!isBuiltinGraphqlType(cleanType)) {
            val existing = scalars[cleanType]
            // If we don't have an existing scalar, or have an existing one but without
            // typeDescription, then add it (even if this one has no description as well)
            if (existing == null || existing["typeDescription"] == null) {
                scalars.put(graphqlType, buildJsonObject {
                    put("name", cleanType)
                    if (hasuraFieldAnnot != null && hasuraFieldAnnot.typeDescription.isNotEmpty()) {
                        put("description", hasuraFieldAnnot.typeDescription)
                    }
                })
            }
        }

    }

    /**
     * If type referes to a class returns type. If type refers to an array of a class, returns the array component
     * type. if type refers to a primitive type or enum, or an array of those, then return null.
     */
    private fun getClassType(type: Class<*>): Class<*>? {
        if (type.isPrimitive || type == String::class.java || type.isEnum) {
            return null
        }
        if (type.isArray) {
            val compoType = type.componentType
            if (compoType.isPrimitive || compoType == String::class.java || compoType.isEnum) {
                return null
            }
            return compoType
        }
        return type
    }
    private fun generateEnum(type: Class<*>, typeName: String, kind: TypeDefinitionKind) : String {

        val actualTypeName = typeName ?: type.simpleName
        if (enums.contains(actualTypeName)) {
            return actualTypeName
        }

        val typeDef = buildJsonObject {
            put("name", actualTypeName)
            putJsonArray("values" ) {
                type.enumConstants.forEach { enumConst ->
                    addJsonObject {
                        put("value", enumConst.toString())
                    }
                }
            }
        }
        enums.put(actualTypeName, typeDef)
        return actualTypeName
    }

    /**
     * Generates a custom output type (ie. returns type of action)
     */
    private fun generateOutputType(method: Method) : JsonObject {
        TODO()
    }

    /**
     * Generates input type for the arguments of the method. The method may have a single argumnet with an object type
     * whose fields are primitive types, or can have many primitive type arguments. In the former case the  name of
     * the class will be used as the type name and this type definition can be reused in multiple actions. In the later
     * case a type name is generated based on the name of the method.
     */
    private fun generateInputType(method: Method) : JsonObject {
        TODO()
    }
}
