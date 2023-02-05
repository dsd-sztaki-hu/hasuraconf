package com.beepsoft.hasuraconf

import com.beepsoft.hasura.actions.HasuraActionController
import io.hasura.metadata.v3.SourceCustomization
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import javax.persistence.EntityManagerFactory

@Configuration
@ComponentScan(basePackageClasses=[HasuraActionController::class])
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
            @Value("\${hasuraconf.generateCheckConstraintsForJSRValidationAnnnotations:true}") generateCheckConstraintsForJSRValidationAnnnotations: Boolean,
            rootFieldNameProvider: RootFieldNameProvider,
            sourceCustomization: SourceCustomization
    ): HasuraConfigurator {
        return HasuraConfigurator(
                entityManagerFactory,
                schemaName,
                hasuraSchemaEndpoint,
                hasuraMetadataEndpoint,
                hasuraAdminSecret,
                actionRoots?.split(",\\s*".toRegex()),
                sourceCustomization,
                rootFieldNameProvider,

                schemaVersion,
                customPropsFieldName,
                ignoreJsonSchema,
                DefaultHasuraCheckConstraintGenerator(),
                generateCheckConstraintsForJSRValidationAnnnotations
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

    /**
     * Provide default SourceCustomization using hasuraconf.namingConvention config value for
     * naming convention.
     */
    @Bean
    @ConditionalOnMissingBean
    fun sourceCustomization(
        @Value("\${hasuraconf.namingConvention:hasura-default}") namingConvention: String,
    ): SourceCustomization {
        return SourceCustomization(
            namingConvention
        )
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
