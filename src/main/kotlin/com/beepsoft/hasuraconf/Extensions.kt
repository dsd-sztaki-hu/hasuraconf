package com.beepsoft.hasuraconf

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.hibernate.metamodel.spi.MetamodelImplementor
import org.hibernate.persister.entity.AbstractEntityPersister
import java.util.NoSuchElementException
import javax.persistence.Inheritance
import javax.persistence.InheritanceType
import javax.persistence.metamodel.EntityType
import kotlin.reflect.KCallable
import kotlin.reflect.full.allSuperclasses


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

/**
 * Beautifies the JSON string with proper indentations
 */
fun String.reformatJson(indent: Boolean? = true): String {
    val doIndent = indent ?: true
    // Reformat json
    objectMapper.configure(SerializationFeature.INDENT_OUTPUT, doIndent)
    val tree = objectMapper.readTree(this)
    return objectMapper.writeValueAsString(tree)
}

/**
 * Converts list to a JSON string
 */
fun List<String>.toJson() : String =
        objectMapper.writeValueAsString(this)

/**
 * Returns true if any parent of the class managed by EntityType has
 * @Inheritance(strategy = InheritanceType.SINGLE_TABLE).
 */
fun EntityType<*>.parentHasSingleTableInheritance() : Boolean {
    this.javaType.kotlin.allSuperclasses.forEach {superClass ->
        superClass.annotations.forEach {
            if (it is Inheritance && (it as Inheritance).strategy == InheritanceType.SINGLE_TABLE) {
                return true
            }
        }

    }
    return false
}

/**
 * Collects all EntityTypes related to the target EntityType. In most cases it will be a single EntityType, the target
 * EntityType itself. In case the EntityType (the class it manages) has
 * @Inheritance(strategy = InheritanceType.SINGLE_TABLE) annotation, then fields of all of its subclasses will be
 * handled under this EntityType as all subclasses store their fields in the table generated for the target EntityType.
 */
fun EntityType<*>.relatedEntities(entities: Set<EntityType<*>>) : List<EntityType<*>> {
    // If this entity has single table inheritance, then need to collect all subclass'es class metadata
    val relatedEntities = mutableListOf<EntityType<*>>()
    val rootEntity = this
    relatedEntities.add(this)
    val annot = this.javaType.getAnnotation(Inheritance::class.java)
    if (annot != null && annot.strategy == InheritanceType.SINGLE_TABLE) {
        // Find subclasses of entity
        entities.forEach {subClass ->
            // If subClass is a subclass of entity then add its metadata to classMetadatas
            if (subClass.javaType.kotlin.allSuperclasses.contains(rootEntity.javaType.kotlin)) {
                relatedEntities.add(subClass)
            }
        }
    }
    return relatedEntities
}
