package com.beepsoft.hasuraconf
import com.beepsoft.hasuraconf.annotation.*
import kotlinx.serialization.json.*
import net.pearx.kasechange.toCamelCase
import org.hibernate.dialect.PostgreSQL9Dialect
import org.hibernate.internal.SessionFactoryImpl
import org.hibernate.metamodel.spi.MetamodelImplementor
import org.hibernate.persister.entity.AbstractEntityPersister
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.scanners.Scanners.*
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.math.BigDecimal
import java.math.BigInteger
import java.sql.Types
import java.util.*
import javax.persistence.Entity
import javax.persistence.EntityManager


/**
 * Based on [HasuraAction] annotations generates
 */
class HasuraActionGenerator(
    val metaModel: MetamodelImplementor? = null
) {

    enum class TypeDefinitionKind {
        INPUT,
        OUTPUT
    }

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
        java.sql.Timestamp::class.java      to Types.TIMESTAMP
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
            "Bool",
            "Date",
            "Float",
            "String" -> true
            else -> false
        }

    val actions = mutableMapOf<String, JsonObject>()
    val inputTypes = mutableMapOf<String, JsonObject>()
    val outputTypes = mutableMapOf<String, JsonObject>()
    val scalars = mutableMapOf<String, JsonObject>()
    val enums = mutableMapOf<String, JsonObject>()

    // https://hasura.io/docs/latest/graphql/core/api-reference/syntax-defs.html#actiondefinition
    fun generateActionMetadata(roots: List<String>): JsonObject {
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
        return buildJsonObject {
            put("actions", buildJsonArray { actions.values.forEach { add(it) } })
            putJsonObject("custom_types") {
                putJsonArray("input_objects") {
                    inputTypes.values.forEach { add(it)  }
                }
                putJsonArray("objects") {
                    outputTypes.values.forEach { add(it) }
                }
                putJsonArray("scalars") {
                    scalars.values.forEach { add(it) }
                }
                putJsonArray("enums") {
                    enums.values.forEach { add(it) }
                }
            }
        }
    }

    /**
     * Generate action definition for the given method.
     */
    private fun generateAction(method: Method, annot: HasuraAction) : JsonObject {
        return buildJsonObject {

            put("name", generateActionName(method, annot))

            if (annot.comment.isNotEmpty()) {
                put("comment", annot.comment)
            }

            if (annot.permissions.isNotEmpty()) {
                putJsonArray("permissions") {
                    annot.permissions.forEach { role ->
                        addJsonObject {
                            put("role", role)
                        }
                    }
                }
            }

            putJsonObject("definition") {
                if (annot.handler.isEmpty()) {
                    throw HasuraConfiguratorException("handler is not set for @HasuraAction on method $method")
                }
                put("handler", annot.handler)
                put("type", annot.type.name.toLowerCase())
                put("kind", annot.kind.name.toLowerCase())
                if (annot.forwardClientHeaders) {
                    put("forward_client_headers", annot.forwardClientHeaders)
                }
                if (annot.headers.isNotEmpty()) {
                    putJsonArray("headers") {
                        annot.headers.forEach { h ->
                            addJsonObject {
                                put("name", h.name)
                                put("value", h.value)
                            }
                        }
                    }
                }

                // Generate and set output type
                // Use the class's name as the default
                var typeForTypeName = method.returnType
                if (method.returnType.isArray) {
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
                put("output_type", generateReturnTypeDefinition(method.returnType, returnTypeName))

                // Generate input args and their types
                if (annot.wrapArgsInType) {
                    TODO()
                }
                else {
                    putJsonArray("arguments") {
                        method.parameters.forEach { p ->
                            addJsonObject {
                                val fieldAnnot = p.getDeclaredAnnotation(HasuraField::class.java)
                                var fieldName = p.name
                                // May override default name with annotation value
                                if (fieldAnnot != null) {
                                    if (fieldAnnot.value.isNotEmpty()) {
                                        fieldName = fieldAnnot.value
                                    }
                                    else if (fieldAnnot.name.isNotEmpty()) {
                                        fieldName = fieldAnnot.name
                                    }
                                }
                                put("name", fieldName)
                                var typeForAnnot = p.type
                                if (typeForAnnot.isArray) {
                                    typeForAnnot = typeForAnnot.componentType
                                }

                                val typeName = calcExplicitName(typeForAnnot.getAnnotation(HasuraType::class.java), fieldAnnot)
                                put("type", generateParameterTypeDefinition(p.type, typeName))
                            }
                        }
                    }
                }
            }
        }
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

    private fun generateTypeDefinition(type: Class<*>, explicitName: String?, kind: TypeDefinitionKind, failForOutputTypeRecursion: Boolean? = false) : String {
        if (type.isPrimitive || type == String::class.java) {
            // TODO add nullability
            return explicitName ?: getHasuraTypeOf(type)!!
        }
        var actualTypeName = explicitName ?: type.simpleName
        if (type.isEnum) {
            // TODO add nullability
            return generateEnum(type, actualTypeName, kind)
        }

        var actualType = type
        if (type.isArray) {
            val compoType = type.componentType
            actualTypeName = explicitName ?: compoType.simpleName
            if (compoType.isPrimitive || compoType == String::class.java) {
                // TODO add nullability
                return "[" + (actualTypeName ?: getHasuraTypeOf(compoType)!!) + "]"
            }
            else if (compoType.isEnum) {
                // TODO add nullability
                return "[" + generateEnum(compoType, actualTypeName, kind) + "]"
            }
            else if (failForOutputTypeRecursion != null && failForOutputTypeRecursion && kind == TypeDefinitionKind.OUTPUT) {
                throw HasuraConfiguratorException("Invalid return type $compoType. Return types should be primitive/enum type or a class made up of primitive/enum type fields")
            }
            actualType = compoType
        }
        if (Collection::class.java.isAssignableFrom(actualType)) {
                throw HasuraConfiguratorException("Invalid type $actualType. Collection classes cannot be used in Hasura action input and output types")
        }
        generateTypeDefinitionForClass(actualType, actualTypeName, kind)
        if (type.isArray) {
            // TODO add nullability
            return "[$actualTypeName!]"
        }

        // TODO add nullability
        return "$actualTypeName"
    }


    /**
     * kind is either "input" or "output"
     */
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
                t.declaredFields.forEach {field ->
                    addJsonObject {

                        val hasuraFieldAnnot = field.getAnnotation(HasuraField::class.java)
                        var name = field.name
                        if (hasuraFieldAnnot != null) {
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
                        // or calc from Hibernate mappings: TODO
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
                        // If graphqlType has not been set as the result of @HasuraRelationship, then generate it now
                        if (graphqlType == null) {
                            graphqlType = generateTypeDefinition(fieldType, explicitFieldTypeName, kind, true)
                        }
                        put("type", graphqlType)
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
                                // In case of hasurra managed entity we must have a calcualted remoteTable
                                if (remoteTable != null) {
                                    put("name", remoteTable)
                                }
                                else {
                                    put("name", annot.remoteTable)
                                }
                                put("schema", if(annot.remoteSchema.isNotEmpty()) annot.remoteSchema else "public")
                            }
                            putJsonArray("field_mappings") {
                                if (hasuraManagedEntityRelationship) {
                                    addJsonObject {
                                        put(fromId!!, toId!!)
                                    }
                                }
                                else {
                                    annot.fieldMappings.forEach {mapping ->
                                        addJsonObject {
                                            put(mapping.fromField, mapping.toField)
                                        }
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
                        put("name", enumConst.toString())
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
