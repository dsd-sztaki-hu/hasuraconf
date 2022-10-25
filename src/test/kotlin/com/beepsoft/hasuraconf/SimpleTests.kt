package com.beepsoft.hasuraconf

import io.hasura.metadata.v3.InsertPermission
import io.hasura.metadata.v3.InsertPermissionEntry
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.jupiter.api.Test

class SimpleTests {

    @Test
    fun testDeserialization() {
        val json = """[{"role":"USER","permission":{"columns":"*","computed_fields":["titleAndDescription"],"check":{},"set":{"locale_lang":"en","locale_country":"us"}}}]"""
        val jsonArray = Json.decodeFromString<JsonArray>(json)
        val x = Json.decodeFromJsonElement<List<InsertPermissionEntry>>(jsonArray)
        println(x)
    }
}
