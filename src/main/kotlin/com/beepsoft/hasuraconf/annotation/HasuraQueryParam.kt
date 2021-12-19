package com.beepsoft.hasuraconf.annotation

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * A query param as key-value pair
 */
@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraQueryParam (
    val key: String,
    val value: String
)
