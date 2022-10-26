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
            @Value("\${hasuraconf.schemaName:public}") schemaName: String,
            @Value("\${hasuraconf.hasuraSchemaEndpoint:http://localhost:8080/v2/query}") hasuraSchemaEndpoint: String,
            @Value("\${hasuraconf.hasuraMetadataEndpoint:http://localhost:8080/v1/metadata}") hasuraMetadataEndpoint: String,
            @Value("\${hasuraconf.hasuraAdminSecret:#{null}}") hasuraAdminSecret: String?,
            @Value("\${hasuraconf.jsonSchema.schemaVersion:DRAFT_2019_09}") schemaVersion: String,
            @Value("\${hasuraconf.jsonSchema.customPropsFieldName:hasura}") customPropsFieldName: String,
            @Value("\${hasuraconf.jsonSchema.ignore:false}") ignoreJsonSchema: Boolean,
            @Value("\${hasuraconf.actionRoots:#{null}}") actionRoots: String?,
            rootFieldNameProvider: RootFieldNameProvider
    ): HasuraConfigurator {
        return HasuraConfigurator(
                entityManagerFactory,
                schemaName,
                hasuraSchemaEndpoint,
                hasuraMetadataEndpoint,
                hasuraAdminSecret,
                schemaVersion,
                customPropsFieldName,
                ignoreJsonSchema,
                actionRoots?.split(",\\s*".toRegex()),
                rootFieldNameProvider
        )
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
        @Value("\${hasuraconf.hasuraSchemaEndpoint:http://localhost:8080/v2/query}") hasuraSchemaEndpoint: String,
        @Value("\${hasuraconf.hasuraMetadataEndpoint:http://localhost:8080/v2/query}") hasuraMetadataEndpoint: String,
        @Value("\${hasuraconf.hasuraAdminSecret:#{null}}") hasuraAdminSecret: String?
    ) : HasuraStaticConfigurator
    {
        return HasuraStaticConfigurator(hasuraSchemaEndpoint, hasuraMetadataEndpoint, hasuraAdminSecret)
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
