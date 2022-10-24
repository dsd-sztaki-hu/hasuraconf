package com.beepsoft.hasuraconf

import org.atteo.evo.inflector.English

/**
 * Root field aliases for a specific type
 */
class RootFieldNames(
    var select : String,
    var selectByPk : String,
    var selectAggregate : String,
    var insert : String,
    var insertOne : String,
    var update : String,
    var updateByPk : String,
    var delete : String,
    var deleteByPk : String
)

/**
 * A dummy, empty RootFieldNames instance
 */
val EmptyRootFieldNames = RootFieldNames("", "", "", "", "", "", "", "", "")

interface RootFieldNameProvider {
    fun rootFieldFor(fieldName: String, entityName: String, entityNameLower: String, tableName: String) : String
}

open class DefaultRootFieldNameProvider : RootFieldNameProvider
{
    override fun rootFieldFor(fieldName: String, entityName: String, entityNameLower: String, tableName: String) : String {
        return when(fieldName) {
            "select" -> English.plural(entityNameLower)
            "selectByPk" -> entityNameLower
            "selectAggregate" -> entityNameLower+"Aggregate"
            "insert" -> "create"+ English.plural(entityName)
            "insertOne" -> "create"+entityName
            "update" -> "update"+ English.plural(entityName)
            "updateByPk" -> "update"+entityName
            "delete" -> "delete"+ English.plural(entityName)
            "deleteByPk" -> "delete"+entityName
            else -> throw HasuraConfiguratorException("Unknown root field name: $fieldName")
        }
    }
}
