package com.beepsoft.hasuraconf

import io.hasura.metadata.v3.cascadeDeleteJson
import io.hasura.metadata.v3.metadataJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.apache.commons.lang3.SystemUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.core.publisher.Mono
import java.io.File

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

			hasuraContainer = GenericContainer<Nothing>("hasura/graphql-engine:v2.13.0")
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
						"HASURA_GRAPHQL_STRINGIFY_NUMERIC_TYPES" to "true",
						"HANDLER_URL" to "http://some.domain/actions"
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
	@Autowired lateinit var staticConf: HasuraStaticConfigurator

	@DisplayName("Test generated hasura conf JSON validity with snapshot")
	@Test
	fun testJsonWithSnapshot() {
		var confData = conf.configure()

//		println("Hasura conf generated:\n${conf.confJson}")
//		var snapshot = readFileUsingGetResource("/hasura_config_snapshot1.json")
//		JSONAssert.assertEquals(snapshot, conf.confJson, false)
//		JSONAssert.assertEquals(conf.confJson, snapshot, false)

		var snapshot = readFileUsingGetResource("/metadata_snapshot1.json")
		snapshot = normalize(snapshot)

		var metadataJson = normalize(confData.metadataJson)
		println("Metadata JSON generated:\n${metadataJson}")
		println("snapshot:\n${snapshot}")
		// Check in both directions
		JSONAssert.assertEquals(snapshot, metadataJson, false)
		JSONAssert.assertEquals(metadataJson, snapshot, false)

		snapshot = readFileUsingGetResource("/cascade_delete_snapshot1.json")
		println("Cascade delete JSON generated:\n${confData.cascadeDeleteJson}")
		// Check in both directions
		JSONAssert.assertEquals(snapshot, confData.cascadeDeleteJson, false)
		JSONAssert.assertEquals(confData.cascadeDeleteJson, snapshot, false)

		println("JSON schema generated:\n${confData.jsonSchema}")
		snapshot = readFileUsingGetResource("/json_schema_snapshot1.json")
		JSONAssert.assertEquals(confData.jsonSchema, snapshot, false)
		JSONAssert.assertEquals(snapshot, confData.jsonSchema, false)

	}

	private fun normalize(elem: JsonElement): JsonElement {
		return when (elem) {
			is JsonObject -> JsonObject(
				elem.entries.map { it.key to normalize(it.value) }.sortedBy { it.first }.toMap())
			is JsonArray -> JsonArray(elem.map { normalize(it) })
			else -> elem
		}
	}

	private fun normalize(json: String) : String {
		return normalize(Json.decodeFromString<JsonObject>(json)).toString()
	}

	@DisplayName("Test generated metadata JSON by loading into Hasura")
	@Test
	fun testLoadingMetadataJsonIntoHasura() {
		conf.hasuraSchemaEndpoint = "http://localhost:${hasuraContainer.getMappedPort(8080)}/v2/query"
		conf.hasuraMetadataEndpoint = "http://localhost:${hasuraContainer.getMappedPort(8080)}/v1/metadata"
		conf.hasuraAdminSecret = "hasuraconf"

		conf.actionRoots = listOf(
			"com.beepsoft.hasuraconf.model.actions1",
			"com.beepsoft.hasuraconf.model.actions2",
			"com.beepsoft.hasuraconf.model.actions3",
		)
		val confData = conf.configure()

		var newMeta = normalize(confData.metadataJson)
		println("**** conf.metadataJson: ${newMeta}")

		conf.loadConfiguration(confData)

		// Load metadata again and compare with what we had with the previous algorithm
		val origMeta = normalize(conf.exportMetadataJson())
		println("**** export_meta result: $origMeta")
		var snapshot = normalize(readFileUsingGetResource("/metadata_snapshot-with-actions1.json"))
		JSONAssert.assertEquals(snapshot, newMeta, false)
		JSONAssert.assertEquals(newMeta, snapshot, false)
	}

//
//	@DisplayName("Test HasuraStaticConfigurator")
//	@Test
//	fun testHasuraStaticConfigurator() {
//		staticConf.hasuraEndpoint = "http://localhost:${hasuraContainer.getMappedPort(8080)}/v1/query"
//		staticConf.hasuraAdminSecret = "hasuraconf"
//
//		val conf1 = """
//			{
//			  "type": "bulk",
//			  "args": [
//			    {
//			      "type": "run_sql",
//			      "args": {
//			        "sql": "ALTER TABLE \"public\".\"calendar_user\" ADD COLUMN \"custom_user_data\" text NULL;",
//			        "cascade": false,
//			        "read_only": false
//			      }
//			    }
//			  ]
//			}
//		""".trimIndent()
//
//		// First it should not fail
//		staticConf.loadStaticConf(conf1)
//		// Should fail
//		try {
//			staticConf.loadStaticConf(conf1)
//			fail { "Should have got an error" }
//		}
//		catch (ex: Exception) {
//			println("Received exception $ex")
//		}
//
//		val conf2 = """
//			{
//			  "hasuraconfLoadSeparately": true,
//			  "type": "bulk",
//			  "args": [
//			    {
//			      "hasuraconfIgnoreError": {
//			        "message":"column \"custom_user_data\" of relation \"calendar_user\" already exists",
//			        "status_code":"42701"
//			      },
//			      "type": "run_sql",
//			      "args": {
//			        "sql": "ALTER TABLE \"public\".\"calendar_user\" ADD COLUMN \"custom_user_data\" text NULL;",
//			        "cascade": false,
//			        "read_only": false
//			      }
//			    }
//			  ]
//			}
//		""".trimIndent()
//		// Should not fail
//		staticConf.loadStaticConf(conf2)
//
//		val conf3 = """
//			{
//			  "hasuraconfLoadSeparately": true,
//			  "type": "bulk",
//			  "args": [
//			    {
//			      "hasuraconfIgnoreError": {
//			        "message":"column \"custom_user_data2\" of relation \"calendar_user\" already exists",
//			        "status_code":"42701"
//			      },
//			      "type": "run_sql",
//			      "args": {
//			        "sql": "ALTER TABLE \"public\".\"calendar_user\" ADD COLUMN \"custom_user_data2\" text NULL;",
//			        "cascade": false,
//			        "read_only": false
//			      }
//			    }
//			  ]
//			}
//		""".trimIndent()
//		// Should not fail no mattter how many times we execute it
//		staticConf.loadStaticConf(conf3)
//		staticConf.loadStaticConf(conf3)
//		staticConf.loadStaticConf(conf3)
//
//		val conf4 = """
//			{
//			  "hasuraconfLoadSeparately": true,
//			  "type": "bulk",
//			  "args": [
//			    {
//			      "hasuraconfIgnoreError": {
//			        "message":"column \"any_field\" of relation \"bad_table\" already exists",
//			        "status_code":"42701"
//			      },
//			      "type": "run_sql",
//			      "args": {
//			        "sql": "ALTER TABLE \"public\".\"bad_table\" ADD COLUMN \"any_field\" text NULL;",
//			        "cascade": false,
//			        "read_only": false
//			      }
//			    }
//			  ]
//			}
//		""".trimIndent()
//		// Should fail even for the first execution, because bad_table doesn't exist and so we won't get the
//		// expected "column \"any_field\" of relation \"bad_table\" already exists" message, but
//		// "relation \"public.bad_table\" does not exist","status_code":"42P01"
//		try {
//			staticConf.loadStaticConf(conf4)
//			fail { "Should have got an error" }
//		}
//		catch (ex: Exception) {
//			println("Received exception $ex")
//		}
//
//
//		val conf5 = """
//			{
//			  "hasuraconfLoadSeparately": true,
//			  "type": "bulk",
//			  "args": [
//			    {
//			      "hasuraconfIgnoreError": {
//			        "message":"column \"custom_user_data3\" of relation \"calendar_user\" already exists",
//			        "status_code":"42701"
//			      },
//			      "type": "run_sql",
//			      "args": {
//			        "sql": "ALTER TABLE \"public\".\"calendar_user\" ADD COLUMN \"custom_user_data3\" text NULL;",
//			        "cascade": false,
//			        "read_only": false
//			      }
//			    },
//			    {
//			      "hasuraconfIgnoreError": {
//			        "message":"column \"custom_user_data4\" of relation \"calendar_user\" already exists",
//			        "status_code":"42701"
//			      },
//			      "type": "run_sql",
//			      "args": {
//			        "sql": "ALTER TABLE \"public\".\"calendar_user\" ADD COLUMN \"custom_user_data4\" text NULL;",
//			        "cascade": false,
//			        "read_only": false
//			      }
//			    },
//			    {
//			      "hasuraconfIgnoreError": {
//			        "message":"column \"custom_user_data5\" of relation \"calendar_user\" already exists",
//			        "status_code":"42701"
//			      },
//			      "type": "run_sql",
//			      "args": {
//			        "sql": "ALTER TABLE \"public\".\"calendar_user\" ADD COLUMN \"custom_user_data5\" text NULL;",
//			        "cascade": false,
//			        "read_only": false
//			      }
//			    }
//			  ]
//			}
//		""".trimIndent()
//		// Multiple operatioons, should not fail no matter how many times we execute it
//		staticConf.loadStaticConf(conf5)
//		staticConf.loadStaticConf(conf5)
//		staticConf.loadStaticConf(conf5)
//	}
//

	@DisplayName("Test action generation via HasuraConfigurator - 1")
	@Test
	fun testActionGenerationViaConfigurator1() {
		conf.actionRoots = listOf("com.beepsoft.hasuraconf.model.actions1")
		val confData = conf.configure()
		val actionsJson = buildJsonObject {
			put("actions", Json.encodeToJsonElement(confData.metadata.actions))
			put("custom_types", Json.encodeToJsonElement(confData.metadata.customTypes))
		}.toString()
		print(actionsJson)
		JSONAssert.assertEquals(
			"""{"actions":[{"name":"createUserAndCalendar5","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"[UserAndCalendarOutput5!]","arguments":[{"name":"args","type":"UserAndCalendarInput!"}]}},{"name":"createUserAndCalendar4","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"UserAndCalendarOutput","arguments":[{"name":"args","type":"UserAndCalendarInput!"}]}},{"name":"createUserAndCalendar2","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"String","arguments":[{"name":"name","type":"String!"},{"name":"descriptions","type":"[String!]!"},{"name":"calendarTypes","type":"[CalendarType!]!"}]}},{"name":"createUserAndCalendar3","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"[String!]","arguments":[{"name":"args","type":"UserAndCalendarInput!"}]}},{"name":"createUserAndCalendar","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"String","arguments":[{"name":"userName","type":"String!"},{"name":"name","type":"String!"},{"name":"description","type":"String!"},{"name":"calendarType","type":"CalendarType!"}]}},{"name":"signUpWithExternalRestApi","definition":{"handler":"http://some.rest.endpoint","type":"mutation","kind":"synchronous","forward_client_headers":true,"request_transform":{"body":"{\"email\":{{${'$'}body.input.args.email}},\"password\":{{${'$'}body.input.args.password}}}","url":"{{${'$'}base_url}}/signup/email-password","content_type":"application/json","method":"POST","query_params":{},"template_engine":"Kriti"},"output_type":"SignUpWithExternalRestApiOutput","arguments":[{"name":"args","type":"SignUpWithExternalRestApiInput!"}]}}],"custom_types":{"input_objects":[{"name":"UserAndCalendarInput","fields":[{"name":"name","type":"String!"},{"name":"description","type":"String!"},{"name":"isPublic","type":"Boolean!"},{"name":"hasColors","type":"Boolean"}]},{"name":"SignUpWithExternalRestApiInput","fields":[{"name":"email","type":"String!"},{"name":"password","type":"String!"}]}],"objects":[{"name":"UserAndCalendarOutput5","description":"The description of UserAndCalendarOutput","fields":[{"description":"The user's name","name":"userFullName","type":"String!"},{"description":"The user's age","name":"userAge","type":"Int"},{"description":"Field is not defined in Kotlin as nullable, but explicitly set so","name":"explicitNullable","type":"String"},{"description":"Field is defined in Kotlin as nullable, but explicitly set to not nullable","name":"explicitlyNotNullable","type":"String!"},{"description":"User identifier","name":"userId","type":"bigint!"},{"name":"calendarId","type":"bigint!"},{"name":"differentCalendarId","type":"bigint!"}],"relationships":[{"name":"calendar","type":"object","remote_table":{"schema":"public","name":"calendar"},"field_mapping":{"calendarId":"id"}},{"name":"otherCalendar","type":"object","remote_table":{"schema":"public","name":"calendar"},"field_mapping":{"differentCalendarId":"id"}}]},{"name":"UserAndCalendarOutput","description":"The description of UserAndCalendarOutput","fields":[{"description":"The user's name","name":"userFullName","type":"String!"},{"description":"The user's age","name":"userAge","type":"Int"},{"description":"Field is not defined in Kotlin as nullable, but explicitly set so","name":"explicitNullable","type":"String"},{"description":"Field is defined in Kotlin as nullable, but explicitly set to not nullable","name":"explicitlyNotNullable","type":"String!"},{"description":"User identifier","name":"userId","type":"bigint!"},{"name":"calendarId","type":"bigint!"},{"name":"differentCalendarId","type":"bigint!"}],"relationships":[{"name":"calendar","type":"object","remote_table":{"schema":"public","name":"calendar"},"field_mapping":{"calendarId":"id"}},{"name":"otherCalendar","type":"object","remote_table":{"schema":"public","name":"calendar"},"field_mapping":{"differentCalendarId":"id"}}]},{"name":"SignUpWithExternalRestApiOutput","fields":[{"name":"mfa","type":"String!"},{"name":"session","type":"String!"}]}],"scalars":[{"name":"bigint","description":"bigint type"}],"enums":[{"name":"CalendarType","values":[{"value":"PRIVATE"},{"value":"PUBLIC"},{"value":"SHARED"}]}]}}""",
			actionsJson,
			true
		)
	}

	@DisplayName("Test action generation via HasuraConfigurator - 2")
	@Test
	fun testActionGenerationWithHibernateActions2() {
		conf.actionRoots = listOf("com.beepsoft.hasuraconf.model.actions2")
		val confData = conf.configure()
		val actionsJson = buildJsonObject {
			put("actions", Json.encodeToJsonElement(confData.metadata.actions))
			put("custom_types", Json.encodeToJsonElement(confData.metadata.customTypes))
		}.toString()
		print(actionsJson)
		JSONAssert.assertEquals(
			"""{"actions":[{"name":"createUserAndCalendar4","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"UserAndCalendarOutput","arguments":[{"name":"args","type":"UserAndCalendarInput!"}]}},{"name":"createUserAndCalendar","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"String","arguments":[{"name":"name","type":"String!"},{"name":"description","type":"String!"},{"name":"calendarRoleType","type":"calendar_role_type_enum!"}]}},{"name":"createUserAndCalendar5","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"[UserAndCalendarOutput5!]","arguments":[{"name":"args","type":"UserAndCalendarInput!"}]}},{"name":"createUserAndCalendar3","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"[String!]","arguments":[{"name":"args","type":"UserAndCalendarInput!"}]}}],"custom_types":{"input_objects":[{"name":"UserAndCalendarInput","fields":[{"name":"name","type":"String!"},{"name":"description","type":"String!"},{"name":"isPublic","type":"Boolean!"}]}],"objects":[{"name":"UserAndCalendarOutput","fields":[{"name":"userName","type":"String!"},{"name":"userId","type":"bigint!"},{"name":"automaticCalendarId","type":"bigint!"},{"name":"differentCalendarId","type":"bigint!"}],"relationships":[{"name":"automaticCalendar","type":"object","remote_table":{"schema":"public","name":"calendar"},"field_mapping":{"automaticCalendarId":"id"}},{"name":"otherCalendar","type":"object","remote_table":{"schema":"public","name":"calendar"},"field_mapping":{"differentCalendarId":"id"}}]},{"name":"UserAndCalendarOutput5","fields":[{"name":"userName","type":"String!"},{"name":"userId","type":"bigint!"},{"name":"automaticCalendarId","type":"bigint!"},{"name":"differentCalendarId","type":"bigint!"}],"relationships":[{"name":"automaticCalendar","type":"object","remote_table":{"schema":"public","name":"calendar"},"field_mapping":{"automaticCalendarId":"id"}},{"name":"otherCalendar","type":"object","remote_table":{"schema":"public","name":"calendar"},"field_mapping":{"differentCalendarId":"id"}}]}],"scalars":[{"name":"bigint"}],"enums":[{"name":"calendar_role_type_enum","values":[{"value":"OWNER"},{"value":"EDITOR"},{"value":"VIEWER"}]}]}}""",
			actionsJson,
			true
		)
	}
	@DisplayName("Test action generation via HasuraConfigurator - 3")
	@Test
	fun testActionGenerationWithHibernateActions3() {
		conf.actionRoots = listOf("com.beepsoft.hasuraconf.model.actions3")
		conf.configure()
		val confData = conf.configure()
		val actionsJson = buildJsonObject {
			put("actions", Json.encodeToJsonElement(confData.metadata.actions))
			put("custom_types", Json.encodeToJsonElement(confData.metadata.customTypes))
		}.toString()
		print(actionsJson)
		JSONAssert.assertEquals(
			"""{"actions":[{"name":"createUserAndCalendar2","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"UserAndCalendar","arguments":[{"name":"name","type":"String!"},{"name":"description","type":"String!"},{"name":"calendarType","type":"CalendarType!"}]}},{"name":"createUserAndCalendar","definition":{"handler":"{{HANDLER_URL}}","type":"mutation","kind":"synchronous","forward_client_headers":true,"output_type":"String","arguments":[{"name":"args","type":"UserAndCalendarInput!"}]}}],"custom_types":{"input_objects":[{"name":"UserAndCalendarInput","fields":[{"name":"userName","type":"String!"},{"name":"name","type":"String!"},{"name":"description","type":"String!"}]}],"objects":[{"name":"UserAndCalendar","fields":[{"name":"userName","type":"String"},{"name":"userId","type":"bigint!"},{"name":"calendarId","type":"bigint!"},{"name":"differentCalendarId","type":"bigint!"}],"relationships":[{"name":"calendar","type":"object","remote_table":{"schema":"public","name":"calendar"},"field_mapping":{"calendarId":"id"}},{"name":"otherCalendar","type":"object","remote_table":{"schema":"public","name":"calendar"},"field_mapping":{"differentCalendarId":"id"}}]}],"scalars":[{"name":"bigint"}],"enums":[{"name":"CalendarType","values":[{"value":"PRIVATE"},{"value":"PUBLIC"},{"value":"SHARED"}]}]}}""",
			actionsJson,
			true
		)
	}

}
