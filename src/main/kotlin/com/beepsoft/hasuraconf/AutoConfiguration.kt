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
            @Value("\${hasuraconf.loadConf:false}") loadConf: Boolean,
            @Value("\${hasuraconf.metadataFile:metadata.json}") metadataJsonFile: String?,
            @Value("\${hasuraconf.loadMetadata:false}") loadMetadata: Boolean,
            @Value("\${hasuraconf.cascadeDeleteFile:cascade-delete.json}") cascadeDeleteJsonFile: String?,
            @Value("\${hasuraconf.loadCascadeDelete:false}") loadCascadeDelete: Boolean,
            @Value("\${hasuraconf.schemaName:public}") schemaName: String,
            @Value("\${hasuraconf.hasuraEndpoint:http://localhost:8080/v1/query}") hasuraEndpoint: String,
            @Value("\${hasuraconf.hasuraAdminSecret:#{null}}") hasuraAdminSecret: String?,
            @Value("\${hasuraconf.jsonSchema.schemaFile:hasura-json-schema.json}") schemaFile: String?,
            @Value("\${hasuraconf.jsonSchema.schemaVersion:DRAFT_2019_09}") schemaVersion: String,
            @Value("\${hasuraconf.jsonSchema.customPropsFieldName:hasura}") customPropsFieldName: String,
            @Value("\${hasuraconf.jsonSchema.ignore:false}") ignoreJsonSchema: Boolean,
            rootFieldNameProvider: RootFieldNameProvider
    ): HasuraConfigurator {
        return HasuraConfigurator(
                entityManagerFactory,
                confFile,
                loadConf,
                metadataJsonFile,
                loadMetadata,
                cascadeDeleteJsonFile,
                loadCascadeDelete,
                schemaName,
                hasuraEndpoint,
                hasuraAdminSecret,
                schemaFile,
                schemaVersion,
                customPropsFieldName,
                ignoreJsonSchema,
                rootFieldNameProvider)
    }

    /**
     * Provide default implementation of RootFieldNameProvider.
     */
    @Bean
    @ConditionalOnMissingBean
    fun rootFieldNameProvider(): RootFieldNameProvider {
        return DefaultRootFieldNameProvider()
    }

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    @ConditionalOnMissingBean
    fun hasuraStaticConfigurator(
            @Value("\${hasuraconf.hasuraEndpoint:http://localhost:8080/v1/query}") hasuraEndpoint: String,
            @Value("\${hasuraconf.hasuraAdminSecret:#{null}}") hasuraAdminSecret: String?
    ) : HasuraStaticConfigurator
    {
        return HasuraStaticConfigurator(hasuraEndpoint, hasuraAdminSecret)
    }
//    @Bean
//    @ConditionalOnMissingBean
//    fun hasuraJsonSchemaGenerator(
//            @Value("\${hasuraconf.jsonSchema.customPropsFieldName:hasura}") customPropsFieldName: String
//    ) : HasuraJsonSchemaGenerator
//    {
//        return HasuraJsonSchemaGenerator(customPropsFieldName)
//    }
}