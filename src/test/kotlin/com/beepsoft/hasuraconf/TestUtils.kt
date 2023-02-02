package com.beepsoft.hasuraconf

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

fun readFileUsingGetResource(fileName: String)
        = TestApp::class.java.getResource(fileName).readText(Charsets.UTF_8)


fun normalize(json: String) : String {
    return normalize(Json.decodeFromString<JsonObject>(json)).toString()
}

fun normalize(elem: JsonElement): JsonElement {
    return when (elem) {
        is JsonObject -> JsonObject(
            elem.entries.map { it.key to normalize(it.value) }.sortedBy { it.first }.toMap())
        is JsonArray -> JsonArray(elem.map { normalize(it) })
        else -> elem
    }
}
