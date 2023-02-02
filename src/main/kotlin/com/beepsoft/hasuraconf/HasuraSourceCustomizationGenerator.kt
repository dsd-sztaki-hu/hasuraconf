package com.beepsoft.hasuraconf

import com.beepsoft.hasuraconf.annotation.HasuraAction
import com.beepsoft.hasuraconf.annotation.HasuraNamingConvention
import com.beepsoft.hasuraconf.annotation.HasuraSourceCustomization
import io.hasura.metadata.v3.CustomRootFields
import io.hasura.metadata.v3.SourceCustomization
import io.hasura.metadata.v3.SourceTypeCustomization
import javax.persistence.metamodel.EntityType
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import java.lang.reflect.Method

class HasuraSourceCustomizationGenerator {

    fun generateSourceCustomization(entities: Set<EntityType<*>>) : SourceCustomization? {
        val annot = entities
            .filter { entityType -> entityType.javaType.isAnnotationPresent(HasuraSourceCustomization::class.java)}
            .map { entityType ->  entityType.javaType.getAnnotation(HasuraSourceCustomization::class.java)}
            .firstOrNull()
        if (annot != null) {
            return generateSourceCustomization(annot)
        }
        return null
    }

    fun generateSourceCustomization(annot: HasuraSourceCustomization) : SourceCustomization{
        val sourceCustomization = SourceCustomization(
            if (annot.namingConvention == HasuraNamingConvention.HASURA_DEFAULT) "hasura-default" else "graphql-default",
            CustomRootFields(),
            SourceTypeCustomization(annot.typeName.prefix, annot.typeName.suffix)
        )

        if (!annot.rootFields.delete.isEmpty()) {
            sourceCustomization.rootFields!!.delete = annot.rootFields.delete
        }
        if (!annot.rootFields.deleteByPk.isEmpty()) {
            sourceCustomization.rootFields!!.deleteByPk = annot.rootFields.deleteByPk
        }
        if (!annot.rootFields.insert.isEmpty()) {
            sourceCustomization.rootFields!!.insert = annot.rootFields.insert
        }
        if (!annot.rootFields.insertOne.isEmpty()) {
            sourceCustomization.rootFields!!.insertOne = annot.rootFields.insertOne
        }
        if (!annot.rootFields.select.isEmpty()) {
            sourceCustomization.rootFields!!.select = annot.rootFields.select
        }
        if (!annot.rootFields.selectByPk.isEmpty()) {
            sourceCustomization.rootFields!!.selectByPk = annot.rootFields.selectByPk
        }
        if (!annot.rootFields.selectAggregate.isEmpty()) {
            sourceCustomization.rootFields!!.selectAggregate = annot.rootFields.selectAggregate
        }
        if (!annot.rootFields.update.isEmpty()) {
            sourceCustomization.rootFields!!.update = annot.rootFields.update
        }
        if (!annot.rootFields.updateByPk.isEmpty()) {
            sourceCustomization.rootFields!!.updateByPk = annot.rootFields.updateByPk
        }
        return sourceCustomization
    }
}
