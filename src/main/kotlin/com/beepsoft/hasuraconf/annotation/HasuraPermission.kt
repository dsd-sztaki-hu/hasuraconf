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
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
annotation class HasuraPermission (
        val operation: HasuraOperation = HasuraOperation.SELECT,
        val role: String = "",
        val json: String = "",
        val jsonFile: String = "",
        val fields: Array<String> = [],
        val excludeFields: Array<String> = [],
        val fieldPresets: HasuraFieldPresets = HasuraFieldPresets([]),
        val allowAggregations: Boolean = false
)

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
annotation class HasuraPermissions (
        val value: Array<HasuraPermission>
)

/**
 * Fields presets for INSERT and UPDATE operations
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
annotation class HasuraFieldPresets (
    val value: Array<HasuraFieldPreset>
)

@Repeatable
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
annotation class HasuraFieldPreset (
    /**
     * Field name on the Java class. Will be converted to appropriate column name for the Hasura metadata "set".
     */
    val field: String = "",

    /**
     * Column name of the table the class is mapped to. You should use either field or column
     */
    val column: String = "",

    /**
     * Value of the field.
     */
    val value: String
)


enum class HasuraOperation {
    SELECT,
    INSERT,
    UPDATE,
    DELETE
}