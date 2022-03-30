package com.beepsoft.hasuraconf.annotation

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Defines permission for a type. Either {@code json} or {@code jsonFile} should set with the Hasura graphql permission
 * JSON value. In case of @{code jsonFile} it should be point to a classapth resource containing the permission
 * definition. @{code jsonFile} could be used in case the JSON too big and would be difficult to read/edit in the
 * @{code json} field. The {@code fields} can be a list of fields/columns that will be accessible for those users
 * who match the permission criteria. An empty {@code fields} means all fields/columns. It is also possible to
 * use {@code excludeFields} together with an empty {@code field}. In this case the fields listed in
 * {@code excludeFields} will not be accessible by this permission.
 * <p></p>
 * In some cases the permission definitions maybe too long or could be reused on more than one entity.
 * In this case a new annotation can be created and {@code @HasuraPermissions/@HasuraPermission} can be applied to it
 * as a meta annotation. For example the following {@code @CalendarBasedPermissions} can be used on any entity
 * which has a reference to 'calendar' and permissions are defined in relation to the calendar
 * <pre>
 *     @HasuraPermissions(
 *          [
 *              HasuraPermission(
 *                  operation = HasuraOperation.INSERT,
 *                  role="user"),
 *              HasuraPermission(
 *                  operation = HasuraOperation.SELECT,
 *                  role="user",
 *                  json="{calendar: '@include(/permissions/read_permission_fragment.json)'}"),
 *              HasuraPermission(
 *                  operation = HasuraOperation.UPDATE,
 *                  role="user",
 *                  json="{calendar: '@include(/permissions/update_permission_fragment.json)'}"),
 *              HasuraPermission(
 *                  operation = HasuraOperation.DELETE,
 *                  role="user",
 *                  json="{calendar: '@include(/permissions/delete_permission_fragment.json)'}")
 *          ]
 *     )
 *     annotation class CalendarBasedPermissions
 * </pre>
 *
 * This could be used on entities like this:
 *
 * <pre>
 * @Entity
 * @CalendarBasedPermissions
 * class Event {
 *       /** The Calendar it belongs to.  */
 *      @ManyToOne(optional = false)
 *      var calendar: Calendar? = null
 *      ...
 * }
 *
 * @Entity
 * @CalendarBasedPermissions
 * class Day {
 *       /** The Calendar it belongs to.  */
 *      @ManyToOne(optional = false)
 *      var calendar: Calendar? = null
 *      ...
 * }
 * </pre>
 *
 */
@Repeatable
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraPermission(
    val operation: HasuraOperation = HasuraOperation.SELECT,
    // multiple operations to generate the same permission values for
    val operations: Array<HasuraOperation> = [HasuraOperation.NOTSET],
    val role: String = NOTSET,
    // multiple roles to generate the same permission values for
    val roles: Array<String> = [NOTSET],
    val json: String = NOTSET,
    val jsonFile: String = NOTSET,
    val fields: Array<String> = [NOTSET],
    val excludeFields: Array<String> = [NOTSET],
    val fieldPresets: HasuraFieldPresets = HasuraFieldPresets([HasuraFieldPreset(value = NOTSET)]),
    val allowAggregations: AllowAggregationsEnum = AllowAggregationsEnum.NOTSET,
    // Mark it as a default permission, which can define entity wide global setting per role and/or operation
    // The order of evaluation, application to the actual HasuraPermission:
    // 1. Matching operation without role set
    // 2. Matching operation with role set
    // 3. Actual HasuraPermission
    // This allows to define defaults for all SELECT operations and some specific defaults for SELECT operation
    // and WORKER role.
    val default: Boolean = false,
)

// An object representation of a @HasuraPermission annotation
// Default values must match @HasuraPermission default values
data class HasuraPermissionValues(
    var operation: HasuraOperation,
    var operations: List<HasuraOperation>,
    var role: String,
    var roles: List<String> = listOf(NOTSET),
    var json: String = NOTSET,
    var jsonFile: String = NOTSET,
    var fields: List<String> = listOf(NOTSET),
    var excludeFields: List<String> = listOf(NOTSET),
    var fieldPresets:  List<HasuraFieldPresetValues> = listOf(HasuraFieldPresetValues(NOTSET, NOTSET, "")),
    var allowAggregations: AllowAggregationsEnum = AllowAggregationsEnum.NOTSET,
    val default: Boolean = false,
    ) {
    companion object {
        fun from(annot: HasuraPermission) =
            HasuraPermissionValues(
                annot.operation,
                annot.operations.toList(),
                annot.role,
                annot.roles.toList(),
                annot.json,
                annot.jsonFile,
                annot.fields.toList(),
                annot.excludeFields.toList(),
                annot.fieldPresets.value.toList().map { hasuraFieldPreset ->  HasuraFieldPresetValues.from(hasuraFieldPreset)},
                annot.allowAggregations,
                annot.default
            )
    }

    fun merge(other: HasuraPermissionValues) =
        HasuraPermissionValues(
            // operation and role must be set to a meaningful value,
            if (other.operation != HasuraOperation.NOTSET) other.operation else this.operation,
            if (!other.operations.isNotEmpty() && other.operations[0] != HasuraOperation.NOTSET) other.operations else this.operations,
            if (other.role != NOTSET) other.role else this.role,
            if (!other.roles.contains(NOTSET)) other.roles else this.roles,
            // Use the one, which is not set to the default value.
            if (other.json != NOTSET) other.json else this.json,
            if (other.jsonFile != NOTSET) other.jsonFile else this.jsonFile,
            if (!other.fields.contains(NOTSET)) other.fields else this.fields,
            if (!other.excludeFields.contains(NOTSET)) other.excludeFields else this.excludeFields,
            if (other.fieldPresets.isNotEmpty() && other.fieldPresets[0].value != NOTSET) other.fieldPresets else this.fieldPresets,
            if (other.allowAggregations != AllowAggregationsEnum.NOTSET) other.allowAggregations else this.allowAggregations,
        )

}

const val NOTSET = "__NOTSET__"
const val EMPTY = "__EMPTY__"

enum class AllowAggregationsEnum {
    NOTSET,
    TRUE,
    FALSE
}

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraPermissions(
    val value: Array<HasuraPermission>
)

/**
 * Fields presets for INSERT and UPDATE operations
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraFieldPresets(
    val value: Array<HasuraFieldPreset>
)

@Repeatable
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraFieldPreset(
    /**
     * Field name on the Java class. Will be converted to appropriate column name for the Hasura metadata "set".
     */
    val field: String = NOTSET,

    /**
     * Column name of the table the class is mapped to. You should use either field or column
     */
    val column: String = NOTSET,

    /**
     * Value of the field.
     */
    val value: String
)

// An object representation of a @HasuraFieldPreset annotation
data class HasuraFieldPresetValues(
    val field: String = NOTSET,
    val column: String = NOTSET,
    val value: String = ""
) {
    companion object {
        fun from(annot: HasuraFieldPreset) : HasuraFieldPresetValues {
            return HasuraFieldPresetValues(
                annot.field,
                annot.column,
                annot.value
            )
        }
    }
}

enum class HasuraOperation {
    SELECT,
    INSERT,
    UPDATE,
    DELETE,
    ALL,
    NOTSET,
}
