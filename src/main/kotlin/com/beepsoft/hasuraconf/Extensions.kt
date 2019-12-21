package com.beepsoft.hasuraconf

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import java.util.NoSuchElementException
import kotlin.reflect.KCallable


fun Annotation.getProp(name: String): KCallable<*>? {
    try {
        return this.annotationClass.members.first { it.name == name }
    }
    catch (ex: NoSuchElementException) {
        return null
    }
}

//fun Annotation.valueOf(name: String): Any? {
//    try {
//        return this.annotationClass.members.first { it.name == name }.call(this)
//    }
//    catch (ex: NoSuchElementException) {
//        return null;
//    }
//}

@Suppress("UNCHECKED_CAST")
fun <T> Annotation.valueOf(name: String): T {
    return this.annotationClass.members.first { it.name == name }.call(this) as T
}

private val objectMapper = ObjectMapper()

fun String.reformatJson(): String {
    // Reformat json
    objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true)
    val tree = objectMapper.readTree(this)
    return objectMapper.writeValueAsString(tree)
}

fun List<String>.toJson() : String =
        objectMapper.writeValueAsString(this)