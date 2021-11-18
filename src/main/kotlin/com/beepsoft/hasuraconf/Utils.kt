package com.beepsoft.hasuraconf

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.Field
import java.util.*
import kotlin.reflect.KCallable

fun getLogger(forClass: Class<*>): Logger =
        LoggerFactory.getLogger(forClass)

object Utils {
    private val LOG = getLogger(Utils::class.java)
    private val declaredFields: MutableMap<String?, Field?> =  /*Collections.synchronizedMap(*/HashMap() /*)*/
    private fun dfMapKey(baseClass: Class<*>?, fieldName: String): String? {
        return if (baseClass != null) baseClass.name + "---" + fieldName else null
    }

    @Synchronized
    private fun putToDfMap(dfMapKey: String?, f: Field?): Field? {
        if (declaredFields.containsKey(dfMapKey)) {
            return declaredFields[dfMapKey]
        }
        declaredFields[dfMapKey] = f
        return f
    }

    /**
     * find a declared field in a class or its superclasses
     * @param baseClass
     * @param fieldName
     * @return
     */
    fun findDeclaredFieldUsingReflection(baseClass: Class<*>?, fieldName: String): Field? {
        val dfMapKey = dfMapKey(baseClass, fieldName)
        if (declaredFields.containsKey(dfMapKey)) {
            return declaredFields[dfMapKey]
        }
        var res = baseClass
        while (res != null) {
            try {
                var f = res.getDeclaredField(fieldName)
                f = putToDfMap(dfMapKey, f)
                return f
            } catch (e: NoSuchFieldException) {
                LOG.trace("Field not found in class {}, looking in superclass", res)
                res = res.superclass
            } catch (e: SecurityException) {
                LOG.trace("Field not found in class {}, looking in superclass", res)
                res = res.superclass
            }
        }
        if (putToDfMap(dfMapKey, null) != null) {
            throw RuntimeException("This is madness! ALL IS LOŚ͖̩͇̗̪̏̈́T ALL I​S LOST the pon̷y he comes!")
        }
        return null
    }
}

fun actualSchemaAndName(schemaName: String, tableName: String) : Pair<String, String>
{
    val schemaAndName = tableName.split(".")
    if (schemaAndName.size > 1) {
        return Pair(schemaAndName[0], schemaAndName[1])
    }
    return Pair(schemaName, tableName)
}
