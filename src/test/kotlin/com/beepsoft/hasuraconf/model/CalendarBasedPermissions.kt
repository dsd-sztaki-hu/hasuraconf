package com.beepsoft.hasuraconf.model

import com.beepsoft.hasuraconf.annotation.HasuraOperation
import com.beepsoft.hasuraconf.annotation.HasuraPermission
import com.beepsoft.hasuraconf.annotation.HasuraPermissions

@HasuraPermissions(
        [
            HasuraPermission(
                    operation = HasuraOperation.INSERT,
                    role="user"),
            HasuraPermission(
                    operation = HasuraOperation.SELECT,
                    role="user",
                    jsonFile = "/permissions/via_cal_read_permission_fragment.json"),
            HasuraPermission(
                    operation = HasuraOperation.UPDATE,
                    role="user",
                    json="{calendar: '@include(/permissions/update_permission_fragment.json)'}"),
            HasuraPermission(
                    operation = HasuraOperation.DELETE,
                    role="user",
                    json="{calendar: '@include(/permissions/delete_permission_fragment.json)'}")
        ]
)
annotation class CalendarBasedPermissions