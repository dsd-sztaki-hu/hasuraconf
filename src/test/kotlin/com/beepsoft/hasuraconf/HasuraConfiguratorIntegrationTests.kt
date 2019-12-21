package com.beepsoft.hasuraconf

import org.apache.commons.lang3.SystemUtils
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Tests HasuraConfigurator with Postgresql + Hasura
 */
@SpringBootTest(
		// More config in the Initializer
		properties = [
			"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQL94Dialect",
			"spring.datasource.initialization-mode=always",
			"spring.datasource.data=classpath:/sql/postgresql/data_import_values.sql",
			"spring.jpa.hibernate.ddl-auto=update"
		//	"logging.level.org.hibernate=DEBUG"
		],
		classes = [TestApp::class]
)
@ContextConfiguration(initializers = [HasuraConfiguratorIntegrationTests.Companion.Initializer::class])
@Testcontainers
@ExtendWith(SpringExtension::class)
class HasuraConfiguratorIntegrationTests {


	// https://stackoverflow.com/questions/53854572/how-to-override-spring-application-properties-in-test-classes-spring-s-context
	//https://www.baeldung.com/spring-boot-testcontainers-integration-test
	// https://www.baeldung.com/spring-boot-testcontainers-integration-test
	companion object {

		private val LOG = getLogger(this::class.java.enclosingClass)

		var postgresqlContainer: PostgreSQLContainer<*>
		var hasuraContainer: GenericContainer<*>

		init {
			val host = if (SystemUtils.IS_OS_MAC_OSX || SystemUtils.IS_OS_WINDOWS) "host.docker.internal" else "172.17.0.1"

			println("Hasura connecting to host $host")
			val logConsumer = Slf4jLogConsumer(LOG)
			postgresqlContainer = PostgreSQLContainer<Nothing>("postgres:11.5-alpine").
				apply {
					withUsername("hasuraconf")
					withPassword("hasuraconf")
					withDatabaseName("hasuraconf")
				}
			postgresqlContainer.start()
			postgresqlContainer.followOutput(logConsumer)

			hasuraContainer = GenericContainer<Nothing>("hasura/graphql-engine:v1.0.0")
				.apply {
					//dependsOn(postgresqlContainer)
					val postgresUrl = "postgres://hasuraconf:hasuraconf@${host}:${postgresqlContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)}/hasuraconf"
					val jdbc = postgresqlContainer.getJdbcUrl()
					println("jdbc $jdbc")
					println("postgresUrl $postgresUrl")
					withExposedPorts(8080)
					withEnv(mapOf(
							"HASURA_GRAPHQL_DATABASE_URL" to postgresUrl,
							"HASURA_GRAPHQL_ACCESS_KEY" to "hasuraconf",
							"HASURA_GRAPHQL_STRINGIFY_NUMERIC_TYPES" to "true"
					))
				}
			hasuraContainer.start()
			hasuraContainer.followOutput(logConsumer)
		}

		// This would be the default use of the container, however it doesn't account for dependencies among them
//		@Container
//		@JvmField
//		val postgresqlContainer: PostgreSQLContainer<*> = PostgreSQLContainer<Nothing>("postgres:11.5-alpine").
//				apply {
//					withUsername("hasuraconf")
//					withPassword("hasuraconf")
//					withDatabaseName("hasuraconf")
//					withExposedPorts(5432)
//				}
//
//		// https://github.com/testcontainers/testcontainers-java/issues/318
//		@Container
//		@JvmField
//		val hasuraContainer: GenericContainer<*> = GenericContainer<Nothing>("hasura/graphql-engine:v1.0.0")
//				.apply{
//					dependsOn(postgresqlContainer)
//					withExposedPorts(8080)
//					withEnv(mapOf(
//							"HASURA_GRAPHQL_DATABASE_URL" to postgresqlContainer.getJdbcUrl().replace("jdbc:", ""),
//							"HASURA_GRAPHQL_ACCESS_KEY" to "hasuraconf",
//							"HASURA_GRAPHQL_STRINGIFY_NUMERIC_TYPES" to "true"
//					))
//				}


		// Dynamic initialization of properties. This is necessary because we only know the
		// spring.datasource.url once the postgresqlContainer is up and running.
		class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
			override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
				TestPropertyValues.of(
						"spring.datasource.url=" + postgresqlContainer.getJdbcUrl(),
						"spring.datasource.username=" + postgresqlContainer.getUsername(),
						"spring.datasource.password=" + postgresqlContainer.getPassword()
				).applyTo(configurableApplicationContext.environment)
			}
		}
	}

	@Autowired lateinit var conf: HasuraConfigurator

	@DisplayName("Test generated hasura conf JSON validity with snapshot")
	@Test
	fun testJsonWithSnapshot() {
		conf.loadConf = false
		conf.configure()
		println(conf.confJson)
		val snapshot = readFileUsingGetResource("/hasura_config_snapshot1.json")
		JSONAssert.assertEquals(conf.confJson,snapshot, false)
	}

	@DisplayName("Test generated hasura conf JSON by loading into Hasura")
	@Test
	fun testLoadingIntoHasura() {
		conf.loadConf = true
		conf.hasuraEndpoint = "http://localhost:${hasuraContainer.getMappedPort(8080)}/v1/query"
		conf.hasuraAdminSecret = "hasuraconf"
		conf.configure()
	}
}
