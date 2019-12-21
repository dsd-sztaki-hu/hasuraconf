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
 * In some cases it would be tedious to repeat the same {@code @HasuraPermission} definition on many entities. Instead one
 * can define a new annotation and annotate it with {@code @HasuraPermission}. The following defines a
 * {@code @UserPermission} annotation, which can be used on all classes where we want to allow insert permission for
 * User role.
 * <pre>
 *  @Retention(RetentionPolicy.RUNTIME)
 *   @Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
 *   @HasuraPermission
 *   annotation class UserInsertPermission (
 *       val operation: HasuraPermissionOperation = HasuraPermissionOperation.INSERT,
 *       val json: String = "{\"roles\": {\"userId\": {\"_eq\": \"X-Hasura-User-Id\"}}}",
 *       val fields: Array<String> = [],
 *       val fields: Array<String> = [],
 *       val excludeFields: Array<String> = ["desscription", "alternativeName"]
 *   )
 * </pre>
 *
 */
@Repeatable
@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
annotation class HasuraPermission (
        val operation: HasuraOperation = HasuraOperation.SELECT,
        val role: String = "",
        val json: String = "",
        val jsonFile: String = "",
        val fields: Array<String> = [],
        val excludeFields: Array<String> = []
)

annotation class HasuraPermissions (
        val value: Array<HasuraPermission>
)

enum class HasuraOperation {
    SELECT,
    INSERT,
    UPDATE,
    DELETE
}