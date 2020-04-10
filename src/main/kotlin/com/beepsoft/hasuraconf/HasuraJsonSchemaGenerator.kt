package com.beepsoft.hasuraconf

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.victools.jsonschema.generator.*
import java.lang.reflect.Field
import java.util.*
import com.github.victools.jsonschema.module.javax.validation.JavaxValidationModule;


class HasuraSpecPropValues(
        var relation: String,
        var mappedBy: String? = null,
        var type: String? = null,
        var item: String?  = null,
        var reference: String?  = null)

class HasuraSpecTypeValues(
        var tableName: String)

class JoinType(
        var name: String,
        var tableName: String,
        var fromIdName: String,
        var fromIdType: String,
        var fromAccessor: String,
        var fromAccessorType: String,
        var toIdName: String,
        var toIdType: String,
        var toAccessor: String,
        var toAccessorType: String,
        var orderField: String?  = null,
        var orderFieldType: String? = null
)


/**
 * Generates JSON schema adding hasura specific extensions to the schema. Data for the extensions
 * are collected by the [HasuraConfigurator] and set on [HasuraJsonSchemaGenerator] via
 * [addJoinType] and [addSpecValue]
 *
 */
class HasuraJsonSchemaGenerator(
        private val schemaVersion: String = "DRAFT_2019_09",
        private val customPropsFieldName: String
) {
    // Class name + field name to HasuraSpecPropValues map collected while generarting
    // Haura confiuguration
    private val hasuraSpecPropValuesMap = mutableMapOf<String, HasuraSpecPropValues>()
    private val hasuraSpecTypeValuesMap = mutableMapOf<String, HasuraSpecTypeValues>()

    private val joinTypes = mutableMapOf<String, JoinType>()

    private val defsNames = mapOf(
            SchemaVersion.DRAFT_2019_09 to "\$defs",
            SchemaVersion.DRAFT_7 to "definitions"
    )

    private lateinit var defsName: String;

    val ObjectNode.customNode: ObjectNode
        get() {
            var customNode: ObjectNode? = if (this.has(customPropsFieldName)) this[customPropsFieldName] as ObjectNode else null
            if (customNode == null) {
                customNode = this.putObject(customPropsFieldName)
            }
            return customNode!!
        }

    fun generateSchema(vararg forClass: Class<out Any>): String
    {
        var schemaVersionEnum = SchemaVersion.valueOf(schemaVersion)
        if (schemaVersionEnum == null) {
            schemaVersionEnum = SchemaVersion.DRAFT_2019_09;
        }
        defsName = defsNames[schemaVersionEnum]!!

        val configBuilder = SchemaGeneratorConfigBuilder(schemaVersionEnum, OptionPreset.PLAIN_JSON)
                .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
                .with(JavaxValidationModule())

        configBuilder.forTypesInGeneral().withTypeAttributeOverride { jsonSchemaTypeNode:ObjectNode, scope: TypeScope, config: SchemaGeneratorConfig ->
            if (jsonSchemaTypeNode.has("properties")) {
                var custom = jsonSchemaTypeNode.customNode
                hasuraSpecTypeValuesMap[scope.type.typeName]?.let {
                    custom.put("tableName", it.tableName)
                }
            }

            // Handle case that withInstanceAttributeOverride also adds the custom definitions to the property
            // and to the items and there seems no way to distinguish there for which part of the schema we are
            // generating the spec values. So we clean this up  here and remove 'hasura'field from items
            //
            // "calendars": {
            // 	"hasura": {
            // 		"relation": "many-to-many",
            // 		"JoinType": {
            // 			"type": "#/$defs/UserCalendar",
            // 			"reference": "theCalendarId",
            // 			"item": "calendar"
            // 		}
            // 	},
            // 	"type": "array",
            // 	"items": {
            // 		"$ref": "#/$defs/Calendar",
            // 		"hasura": {
            // 			"relation": "many-to-many",
            // 			"JoinType": {
            // 				"type": "#/$defs/UserCalendar",
            // 				"reference": "theCalendarId",
            // 				"item": "calendar"
            // 			}
            // 		}
            // 	}
            // },
            if (jsonSchemaTypeNode.has("items")) {
                // For some reason at this pint it is not the usual "type":array representation but:
                // "items": {
                // 	    "allOf": [
                // 		    {
                // 		    },
                // 		    {
                // 			    "hasura": {
                // 				    "relation": "one-to-many",
                // 				    "mappedBy": "theme"
                // 			    }
                // 		    }
                // 	    ]
                // }

                val items = jsonSchemaTypeNode.get("items") as ObjectNode;
                if (items.has("allOf")) {
                    val allOf = items.get("allOf") as ArrayNode
                    for (jsonNode  in allOf) {
                        (jsonNode as ObjectNode).remove(customPropsFieldName)
                    }
                }

                // Handle the "usual way" too
                items.remove(customPropsFieldName)
            }
        };

        configBuilder.forFields()
                // Add custom hasura node values
                .withInstanceAttributeOverride { jsonSchemaAttributesNode: ObjectNode, member: FieldScope ->
                    val f = member.rawMember
                    val value = hasuraSpecPropValuesMap[f.declaringClass.name + "-" + f.name]
                    if (value != null) {
                        val customNode = jsonSchemaAttributesNode.customNode

                        // Relation is available for all spec properties
                        customNode.put("relation", value.relation)
                        value.mappedBy?.let { customNode.put("mappedBy", value.mappedBy) }

                        // For many-to-many we define the JoinType and  its attributes
                        if (value.relation == "many-to-many") {
                            val joinTypeNode = customNode.putObject("join")
                            value.type?.let { joinTypeNode.put("type", "#/$defsName/${value.type}") }
                            value.reference?.let { joinTypeNode.put("reference", value.reference) }
                            value.item?.let { joinTypeNode.put("item", value.item) }
                        }
                        else {
                            value.reference?.let { customNode.put("reference", value.reference) }
                        }
                    }
                    //println("jsonSchemaAttributesNode$jsonSchemaAttributesNode")
                }
                // Add format. Currently only Date fields are mapped to "date-time" format
                .withStringFormatResolver { target: FieldScope? ->
                    target?.let {
                        val field = it.rawMember
                        if (Date::class.java.isAssignableFrom(field.type)) {
                            "date-time"
                        }
                        else {
                            null
                        }
                    }
                }

        val config = configBuilder.build()
        val generator = SchemaGenerator(config)
        var resultSchema: ObjectNode = ObjectMapper().createObjectNode()
        resultSchema.putObject(defsName);
        for (someClass in forClass) {
            var classSchema = generator.generateSchema(someClass) as ObjectNode
            moveRefs(classSchema, resultSchema, someClass);
        }

        addJoinTypes(resultSchema)

        println(resultSchema.toString())
        return resultSchema.toString()
    }

    private fun moveRefs(from: ObjectNode, to: ObjectNode, forClass: Class<out Any>)
    {
        val toDefs = to.get(defsName) as ObjectNode

        if (from.has(defsName)) {
            val fromDefs = from.get(defsName) as ObjectNode

            for (fieldName in fromDefs.fieldNames()) {
                if (!toDefs.has(fieldName)) {
                    toDefs.set<ObjectNode>(fieldName, fromDefs.get(fieldName))
                }
            }

        }

        fixTopLevelDef(from, forClass)
        toDefs.set<ObjectNode>(forClass.simpleName, from)

    }

    private fun fixTopLevelDef(def: ObjectNode, forClass: Class<out Any>)
    {
        // Remove $schema and $defs so that "from" only contains the definition forClass and then add it also
        // to "to"'s defs.
        def.remove(defsName)
        def.remove("\$schema")

        // For the top level definition change "$ref": "#" to "$ref": "#/$defs/TopLevelType"
        if (def.has("properties")) {
            val props = def.get("properties") as ObjectNode
            for (fieldName in props.fieldNames()) {
                val field = props.get(fieldName) as ObjectNode
                // Single ref
                if (field.has("\$ref")) {
                    if (field.get("\$ref").asText() == "#") {
                        field.put("\$ref", "#/$defsName/${forClass.simpleName}")
                    }
                }
                // Ref of items of an array type
                if (field.has("items")) {
                    val items = (field.get("items") as ObjectNode);
                    if (items.has("\$ref")) {
                        if (items.get("\$ref").asText() == "#") {
                            items.put("\$ref", "#/$defsName/${forClass.simpleName}")
                        }
                    }
                }
            }
        }
    }

    /**
     * Add colelcted join entities to the final schema.
     */
    private fun addJoinTypes(schema: ObjectNode) {
        val  defs = schema.get(defsName) as ObjectNode

        for (mapEntry in joinTypes) {
            val entityNode = defs.putObject(mapEntry.key)

            entityNode.put("type", "object")
            val properties = entityNode.putObject("properties")

            var node = properties.putObject(mapEntry.value.fromIdName)
            node.put("type", mapEntry.value.fromIdType);
            properties.putObject(mapEntry.value.fromAccessor).put("\$ref", "#/$defsName/"+mapEntry.value.fromAccessorType)

            node = properties.putObject(mapEntry.value.toIdName)
            node.put("type", mapEntry.value.toIdType);
            properties.putObject(mapEntry.value.toAccessor).put("\$ref", "#/$defsName/"+mapEntry.value.toAccessorType)

            mapEntry.value.orderField?.let {
                node = properties.putObject(mapEntry.value.orderField)
                node.put("type", mapEntry.value.orderFieldType)
                node.customNode.put("orderField", true)
            }

            entityNode.customNode.put("tableName", mapEntry.value.tableName)
            entityNode.customNode.put("joinType", true)
        }
    }

    /**
     * Add join entity description with the provided values
     * @param type the join type descriptor to add
     */
    fun addJoinType(type: JoinType) {
        joinTypes.putIfAbsent(type.name, type)
    }

    /**
     * Add hasura sepcific field values.
     * @param field field to add hasura specific values for
     * @param clazz class defining the field
     * @param specValues teh hasura specific values
     */
    fun addSpecValue(
            field: Field,
            clazz: Class<out Any>,
            specValues: HasuraSpecPropValues
    )
    {
        hasuraSpecPropValuesMap.putIfAbsent(clazz.name+"-"+field.name, specValues)
    }


    fun addSpecValue(
            clazz: Class<out Any>,
            specValues: HasuraSpecTypeValues
    )
    {
        hasuraSpecTypeValuesMap.putIfAbsent(clazz.name, specValues)
    }

}