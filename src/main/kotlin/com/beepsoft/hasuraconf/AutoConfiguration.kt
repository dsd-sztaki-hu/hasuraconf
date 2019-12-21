package com.beepsoft.hasuraconf

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.persistence.EntityManagerFactory

@Configuration
class AutoConfiguration
{
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    @ConditionalOnMissingBean
    fun hasuraConfigurator(
            entityManagerFactory: EntityManagerFactory,
            @Value("\${hasuraconf.confFile:hasura-conf.json}") confFile: String?,
            @Value("\${hasuraconf.schemaName:public}") schemaName: String,
            @Value("\${hasuraconf.loadConf:false}") loadConf: Boolean,
            @Value("\${hasuraconf.hasuraEndpoint:http://localhost:8080/v1/query}") hasuraEndpoint: String,
            @Value("\${hasuraconf.hasuraAdminSecret:#{null}}") hasuraAdminSecret: String?
    ): HasuraConfigurator {
        return HasuraConfigurator(entityManagerFactory, confFile, schemaName, loadConf, hasuraEndpoint, hasuraAdminSecret)
    }
}