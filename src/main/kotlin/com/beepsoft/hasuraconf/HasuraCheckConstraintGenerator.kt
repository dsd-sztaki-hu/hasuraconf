package com.beepsoft.hasuraconf

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.hibernate.MappingException
import org.hibernate.metamodel.spi.MetamodelImplementor
import org.hibernate.persister.entity.AbstractEntityPersister
import java.lang.reflect.Field
import javax.persistence.metamodel.EntityType
import javax.validation.constraints.Email
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

interface HasuraCheckConstraintGenerator {
    fun generateCheckConstraints(metaModel: MetamodelImplementor, entity: EntityType<*>, sourceName: String) : List<JsonObject>
}

class DefaultHasuraCheckConstraintGenerator : HasuraCheckConstraintGenerator {

    data class FieldInfo (
        var field: Field,
        var column: String,
        var tableName: String,
        var sourceName: String,
        var index: Int
    )

    override fun generateCheckConstraints(metaModel: MetamodelImplementor, entity: EntityType<*>, sourceName: String) : List<JsonObject> {
        val targetEntityClassMetadata = metaModel.entityPersister(entity.javaType.typeName) as AbstractEntityPersister
        val tableName = targetEntityClassMetadata.tableName
        val classMetadata = metaModel.entityPersister(entity.javaType.typeName) as AbstractEntityPersister

        val runSqls = mutableListOf<JsonObject>()

        val fields = Utils.findDeclaredFields(entity.javaType)
        fields.forEach { f ->
            try {
                val columnNames = classMetadata.getPropertyColumnNames(f.name)
                val fi = FieldInfo(f, columnNames[0], tableName, sourceName, 0)
                runSqls.addAll(generateCheckConstraints(fi))
            }
            catch (ex: MappingException) {
                // It must be transient field, ignore.
            }
        }
        return runSqls
    }

    protected fun generateCheckConstraints(fi: FieldInfo) : List<JsonObject> {
        val runSqls = mutableListOf<JsonObject>()
        fi.field.getAnnotation(Size::class.java)?.let {
            runSqls.add(handleSize(fi))
        }
        fi.field.getAnnotation(Min::class.java)?.let {
            runSqls.add(handleMin(fi))
        }
        fi.field.getAnnotation(Max::class.java)?.let {
            runSqls.add(handleMax(fi))
        }
        fi.field.getAnnotation(Email::class.java)?.let {
            runSqls.add(handleEmail(fi))
        }
        fi.field.getAnnotation(Pattern::class.java)?.let {
            runSqls.add(handlePattern(fi))
        }
        return runSqls
    }

    protected fun handleSize(fi: FieldInfo) : JsonObject {
        val sizeAnnot = fi.field.getAnnotation(Size::class.java)
        val constraintName = constraintName(fi, "size")
        return createRunSql(
            fi,
            constraintName,
            """
            ${
                if (sizeAnnot.min > 0) "length(${fi.column}) >= ${sizeAnnot.min}" else ""
            }${
                if (sizeAnnot.min > 0 && sizeAnnot.max < kotlin.Int.Companion.MAX_VALUE) " and " else ""
            }${
                if (sizeAnnot.max < kotlin.Int.Companion.MAX_VALUE) "length(${fi.column}) <= ${sizeAnnot.max}" else ""
            }
            """.trimIndent()
        )
    }

    protected fun handleMin(fi: FieldInfo) : JsonObject{
        val annot = fi.field.getAnnotation(Min::class.java)
        val constraintName = constraintName(fi, "min")
        return createRunSql(
            fi,
            constraintName,
            """
                ${fi.column} >= ${annot.value}               
            """.trimIndent()
        )
    }

    protected fun handleMax(fi: FieldInfo) : JsonObject {
        val annot = fi.field.getAnnotation(Max::class.java)
        val constraintName = constraintName(fi, "max")
        return createRunSql(
            fi,
            constraintName,
            """${fi.column} <= ${annot.value}"""
        )
    }

    protected fun handleEmail(fi: FieldInfo) : JsonObject {
        val annot = fi.field.getAnnotation(Email::class.java)
        val constraintName = constraintName(fi, "email")
        return createRunSql(
            fi,
            constraintName,
            """
                ${fi.column} ~* ${if (annot.regexp != ".*") annot.regexp else "'^[A-Za-z0-9._+%-]+@[A-Za-z0-9.-]+[.][A-Za-z]+${'$'}'"}
            """.trimIndent()
        )
    }

    protected fun handlePattern(fi: FieldInfo) : JsonObject {
        val annot = fi.field.getAnnotation(Pattern::class.java)
        val constraintName = constraintName(fi, "pattern")
        return createRunSql(
            fi,
            constraintName,
            """ ${fi.column} ~* ${annot.regexp}"""
        )
    }

    protected fun createRunSql(fi: FieldInfo, constraintName: String, checkExpression: String) : JsonObject {
        return buildJsonObject {
            put("hasuraconfComment", """Constraint for ${fi.tableName}.${fi.column}""")
            put("type", "run_sql")
            putJsonObject("args") {
                put("source", fi.sourceName)
                put("sql",
                    """
                    ALTER TABLE ${fi.tableName} DROP CONSTRAINT IF EXISTS ${constraintName};
                    ALTER TABLE ${fi.tableName} ADD CONSTRAINT ${constraintName} 
                        CHECK (${checkExpression});
                    """.trimIndent()
                );
                put("cascade", false)
                put("read_only", false)
            }
        }
    }

    protected fun constraintName(fi: FieldInfo, category: String) : String {
        return "${fi.tableName}_${fi.column}_${category}_check"
    }


}
