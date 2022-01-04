package com.beepsoft.hasuraconf.model

import com.beepsoft.hasuraconf.annotation.*
import javax.persistence.*

/**
 * Layout of a theme for a specific kind of device
 */
@Entity
@Table(
    indexes = arrayOf(
        Index(columnList = "createdAt"),
        Index(columnList = "updatedAt"),
        Index(columnList = "id")
    )
)
// Testing default permissions
@HasuraPermissions(
    [
        // Default roles to provide default values for all SELECT permissions regardless of role
        HasuraPermission(
            default = true,
            operation = HasuraOperation.SELECT,
            role = "",
            allowAggregations = AllowAggregationsEnum.TRUE
        ),

        // Default roles to provide default values for all SELECT permissions for FRIEND role.
        HasuraPermission(
            default = true,
            operation = HasuraOperation.SELECT,
            role = "FRIEND",
            fields = ["title", "description"]
        ),

        // Default roles to provide default values for all SELECT permissions for WORKER role.
        HasuraPermission(
            default = true,
            operation = HasuraOperation.SELECT,
            role = "WORKER",
            fields = ["mediaQuery", "minHeight"]
        ),

        // Default roles to provide default values for all UPDATE permissions
        HasuraPermission(
            default = true,
            operation = HasuraOperation.UPDATE,
            role = "",
            excludeFields = ["title"],
        ),


        // Set from default: allowAggregations = TRUE, fields = ["mediaQuery", "minHeight"]
        HasuraPermission(
            operation = HasuraOperation.SELECT,
            role = "WORKER",
            excludeFields = ["maxHeight"] // will be ignorred
        ),

        // allowAggregations = AllowAggregationsEnum.TRUE from default
        HasuraPermission(
            operation = HasuraOperation.SELECT,
            role = "EDITOR",
        ),

        // excludeFields = ["title"], from default
        HasuraPermission(
            operation = HasuraOperation.UPDATE,
            role = "EDITOR",
        ),

        // No defaults
        HasuraPermission(
            operation = HasuraOperation.DELETE,
            role = "EDITOR",
        ),

        // For testing all default values for any operation of roles
        HasuraPermission(
            default = true,
            operation = HasuraOperation.ALL,
            role = "ALLDEFAULTS",
            fields = ["mnemonic", "mediaQuery"],
            excludeFields = ["minWidth", "maxWidth"],
            json="{media_query: {_eq: \"10\"}}",
            fieldPresets = HasuraFieldPresets([
                HasuraFieldPreset(field="description", value = "Default description")
            ]),
            allowAggregations = AllowAggregationsEnum.TRUE
        ),

        HasuraPermission(
            operation = HasuraOperation.SELECT,
            role = "ALLDEFAULTS",
        ),

        HasuraPermission(
            operation = HasuraOperation.UPDATE,
            role = "ALLDEFAULTS",
            json="{media_query: {_eq: \"99\"}}", // override default
        ),

        // For testing for some operations and some roles
        HasuraPermission(
            default = true,
            operations = [HasuraOperation.UPDATE, HasuraOperation.INSERT],
            roles = ["ROLE1", "ROLE2"],
            json="{mnemonic: {_eq: \"foo\"}}"
        ),

        // No default json inherited
        HasuraPermission(
            operation = HasuraOperation.SELECT,
            role = "ROLE1",
        ),

        // default json inherited and default for UPDATE kicks is and "title" gets gnored
        HasuraPermission(
            operation = HasuraOperation.UPDATE,
            role = "ROLE1",
        ),
        HasuraPermission(
            operation = HasuraOperation.INSERT,
            role = "ROLE1",
        ),
        // default json inherited and default for UPDATE kicks is and "title" gets gnored
        HasuraPermission(
            operation = HasuraOperation.UPDATE,
            role = "ROLE2",
        ),
        HasuraPermission(
            operation = HasuraOperation.INSERT,
            role = "ROLE2",
        ),
    ]
)
class Layout : BaseObject() {
    var mnemonic: String? = null
    var title: String? = null
    var description: String? = null

    @Lob
    var data: ByteArray? = null

    @ManyToOne
    var theme: Theme? = null

    var mediaQuery: String? = null
    var minWidth: Int? = null
    var maxWidth: Int? = null
    var minHeight: Int? = null
    var maxHeight: Int? = null

    var userAgentRegexp: String? = null
}
