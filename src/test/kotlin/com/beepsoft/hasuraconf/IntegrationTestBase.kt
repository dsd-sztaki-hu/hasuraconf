package com.beepsoft.hasuraconf

import com.beepsoft.hasura.actions.HasuraActionFilter
import org.apache.commons.lang3.SystemUtils
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ApplicationEvent
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    // More config in the Initializer
    properties = [
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQL94Dialect",
        "spring.datasource.initialization-mode=always",
        "spring.datasource.data=classpath:/sql/postgresql/data_import_values.sql",
        "spring.jpa.hibernate.ddl-auto=update"
        //	"logging.level.org.hibernate=DEBUG"
    ],
    classes = [
        TestApp::class,
        IntegrationTestBase.Companion.BackendUrlSetter::class,
        HasuraActionFilter::class,
        com.beepsoft.hasura.actions.HasuraActionController::class,
    ]
)
@ContextConfiguration(initializers = [IntegrationTestBase.Companion.Initializer::class])
@ExtendWith(SpringExtension::class)
open class IntegrationTestBase {

    companion object {

        private val LOG = getLogger(this::class.java.enclosingClass)

        var postgresqlContainer: PostgreSQLContainer<*>
        lateinit var hasuraContainer: GenericContainer<*>
        var host : String
        val logConsumer = Slf4jLogConsumer(LOG)
        val postgresUrl: String
        val jdbc: String
        lateinit var serverBaseUrlInHasura: String
        lateinit var serverBaseUrlInHost: String

        // Create postgresqlContainer before starting up the application
        init {
            host = if (SystemUtils.IS_OS_MAC_OSX || SystemUtils.IS_OS_WINDOWS) "host.docker.internal" else "172.17.0.1"

            println("Hasura connecting to host $host")
            postgresqlContainer = PostgreSQLContainer<Nothing>("postgres:11.5-alpine").
            apply {
                withUsername("hasuraconf")
                withPassword("hasuraconf")
                withDatabaseName("hasuraconf")
            }
            postgresqlContainer.start()
            postgresqlContainer.followOutput(logConsumer)
            postgresUrl = "postgres://hasuraconf:hasuraconf@${host}:${postgresqlContainer.getMappedPort(
                PostgreSQLContainer.POSTGRESQL_PORT)}/hasuraconf"
            jdbc = postgresqlContainer.getJdbcUrl()

            // Hasura started when servlet started. See  BackendUrlSetter
        }

        // Dynamic initialization of properties. This is necessary because we only know the
        // spring.datasource.url once the postgresqlContainer is up and running.
        class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
            override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
                TestPropertyValues.of(
                    "spring.datasource.url=" + postgresqlContainer.getJdbcUrl(),
                    "spring.datasource.username=" + postgresqlContainer.getUsername(),
                    "spring.datasource.password=" + postgresqlContainer.getPassword(),
                    // Enable ActionsFilter
                    "hasuraconf.action-controller.enabled="+true
                ).applyTo(configurableApplicationContext.environment)
            }
        }

        /**
         * Gets the runtime port of the backend webserver and calculates value of [backendUrl]
         */
        @Service
        class BackendUrlSetter  { // ServletWebServerInitializedEvent

            // debug
            @EventListener
            fun onApplicationEvent(event: ApplicationEvent) {
                //println("${event.javaClass.simpleName}")
            }

            // debug
            @EventListener
            fun handleContextRefresh(event: ContextRefreshedEvent) {
                val applicationContext: ApplicationContext = event.applicationContext
                val requestMappingHandlerMapping: RequestMappingHandlerMapping = applicationContext
                    .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping::class.java)
                val map: Map<RequestMappingInfo, HandlerMethod> = requestMappingHandlerMapping
                    .getHandlerMethods()
                map.forEach { key: RequestMappingInfo?, value: HandlerMethod? ->
                    println("mapping: $key -- $value")
                }
            }

            @EventListener
            fun onApplicationEvent(event: ServletWebServerInitializedEvent) {
                // This how the service can be accessed on the host
                serverBaseUrlInHost = "http://localhost:${event.webServer.port}"

                // as Hasura runs in a container to access the Spring service running on host we must use
                // http://host.docker.internal
                // Note:we actually use $host, becuase on mac we need host.docker.internal, on linux 172.17.0.1
                serverBaseUrlInHasura = "http://$host:${event.webServer.port}"

                hasuraContainer = GenericContainer<Nothing>("hasura/graphql-engine:v2.17.0")
                    .apply {
                        //dependsOn(postgresqlContainer)
                        println("jdbc $jdbc")
                        println("postgresUrl $postgresUrl")
                        withExposedPorts(8080)
                        withEnv(mapOf(
                            "HASURA_GRAPHQL_DATABASE_URL" to postgresUrl,
                            "HASURA_GRAPHQL_ACCESS_KEY" to "hasuraconf",
                            "HASURA_GRAPHQL_STRINGIFY_NUMERIC_TYPES" to "true",
                            "HASURA_GRAPHQL_ENABLE_CONSOLE" to "true",
                            "HANDLER_URL" to "http://some.domain/actions",
                            "SERVER_BASE_URL" to serverBaseUrlInHasura,
                            "ACTION_HANDLER_ENDPOINT" to "$serverBaseUrlInHasura/actions"
                        ))
                    }
                hasuraContainer.start()
                hasuraContainer.followOutput(logConsumer)
            }

        }
    }

}
