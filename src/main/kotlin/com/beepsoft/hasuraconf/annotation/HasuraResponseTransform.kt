package com.beepsoft.hasuraconf.annotation

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Retention(RetentionPolicy.RUNTIME)
annotation class HasuraResponseTransform (
    val body: String = "",
    val templateEngine: String = "Kriti"
)
