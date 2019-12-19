package com.beepsoft.hasuraconf

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AutoConfiguration
{
    @Bean
    @ConditionalOnMissingBean
    fun hasuraConfigurator(
            @Value("\${hasuraconf.confFile:hasura-conf.json}") confFile: String?,
            @Value("\${hasuraconf.schemaName:public}") schemaName: String,
            @Value("\${hasuraconf.loadConf:false}") loadConf: Boolean,
            @Value("\${hasuraconf.hasuraEndpoint:http://localhost:8080/v1/query}") hasuraEndpoint: String,
            @Value("\${hasuraconf.hasuraAdminSecret:#{null}}") hasuraAdminSecret: String?
    ): HasuraConfigurator {
        return HasuraConfigurator(confFile, schemaName, loadConf, hasuraEndpoint, hasuraAdminSecret);
    }
}