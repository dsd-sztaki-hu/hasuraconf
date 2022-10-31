package com.beepsoft.hasuraconf

import com.beepsoft.hasura.actions.HasuraActionFilter
import com.google.common.net.HttpHeaders
import io.hasura.metadata.v3.metadataJson
import io.hasura.metadata.v3.toBulkMetadataAPIOperationJson
import io.hasura.metadata.v3.toBulkRunSql
import io.hasura.metadata.v3.toCascadeDeleteJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.apache.commons.lang3.SystemUtils
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ApplicationEvent
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.method.HandlerMethod
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import reactor.core.publisher.Mono
import java.util.function.BiConsumer


/**
 * Tests HasuraConfigurator with Postgresql + Hasura
 */
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
		HasuraConfiguratorIntegrationTests.Companion.BackendUrlSetter::class,
		HasuraActionFilter::class,
		com.beepsoft.hasura.actions.HasuraActionController::class,
	]
)
@ContextConfiguration(initializers = [HasuraConfiguratorIntegrationTests.Companion.Initializer::class])
//@Testcontainers
@ExtendWith(SpringExtension::class)
class HasuraConfiguratorIntegrationTests {


	// https://stackoverflow.com/questions/53854572/how-to-override-spring-application-properties-in-test-classes-spring-s-context
	//https://www.baeldung.com/spring-boot-testcontainers-integration-test
	// https://www.baeldung.com/spring-boot-testcontainers-integration-test
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
			postgresUrl = "postgres://hasuraconf:hasuraconf@${host}:${postgresqlContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)}/hasuraconf"
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
				serverBaseUrlInHasura = "http://host.docker.internal:${event.webServer.port}"

				hasuraContainer = GenericContainer<Nothing>("hasura/graphql-engine:v2.13.0")
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

	@Autowired lateinit var conf: HasuraConfigurator

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
		val cascadeDelete = confData.toCascadeDeleteJson().toString()
		println("Cascade delete JSON generated:\n${cascadeDelete}")
		// Check in both directions
		JSONAssert.assertEquals(snapshot, cascadeDelete, false)
		JSONAssert.assertEquals(cascadeDelete, snapshot, false)

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

		var importMeta = normalize(confData.metadataJson)
		println("**** importMeta: ${importMeta}")

		conf.replaceConfiguration(confData)

		// Load metadata again and compare with what we had with the previous algorithm
		val exportMeta = normalize(conf.exportMetadataJson())
		println("**** export_meta result: $exportMeta")
		var snapshot = normalize(readFileUsingGetResource("/metadata_snapshot-with-actions1.json"))
		JSONAssert.assertEquals(snapshot, exportMeta, false)
		JSONAssert.assertEquals(exportMeta, snapshot, false)
	}


	@DisplayName("Test wether metadata built using bulk API calls results in same metadata as exported")
	@Test
	fun testCompareMetadataAPIvsMetadata() {
		conf.hasuraSchemaEndpoint = "http://localhost:${hasuraContainer.getMappedPort(8080)}/v2/query"
		conf.hasuraMetadataEndpoint = "http://localhost:${hasuraContainer.getMappedPort(8080)}/v1/metadata"
		conf.hasuraAdminSecret = "hasuraconf"

		conf.actionRoots = listOf(
			"com.beepsoft.hasuraconf.model.actions1",
			"com.beepsoft.hasuraconf.model.actions2",
			"com.beepsoft.hasuraconf.model.actions3",
		)
		val confData = conf.configure()

		var bulkApiOperations = confData.metadata.toBulkMetadataAPIOperationJson()
		println("**** confData.metadata.toBulkMetadataAPIOperationJson(): ${bulkApiOperations}")

		// Configure metadata by call to the metadata API with one bulk operation for all the configuration ops.
		conf.clearMetadata()
		conf.executeSchemaApi(confData.toBulkRunSql())
		conf.executeMetadataApi(bulkApiOperations)

		// Load metadata again and compare with what we had with the previous algorithm
		val exportMeta = normalize(conf.exportMetadataJson())
		println("**** export_meta result: $exportMeta")
		var snapshot = normalize(readFileUsingGetResource("/metadata_snapshot-with-actions1.json"))
		JSONAssert.assertEquals(snapshot, exportMeta, false)
		JSONAssert.assertEquals(exportMeta, snapshot, false)
	}

	@DisplayName("Test executing schema API with safety configs")
	@Test
	fun testExecuteShcemaApiSafely() {
		conf.hasuraSchemaEndpoint = "http://localhost:${hasuraContainer.getMappedPort(8080)}/v2/query"
		conf.hasuraMetadataEndpoint = "http://localhost:${hasuraContainer.getMappedPort(8080)}/v1/metadata"
		conf.hasuraAdminSecret = "hasuraconf"

		val conf1 = """
			{
			  "type": "bulk",
			  "args": [
			    {
			      "type": "run_sql",
			      "args": {
			        "sql": "ALTER TABLE \"public\".\"calendar_user\" ADD COLUMN \"custom_user_data\" text NULL;",
			        "cascade": false,
			        "read_only": false
			      }
			    }
			  ]
			}
		""".trimIndent()

		// First it should not fail
		conf.executeSchemaApi(conf1, true)
		// Should fail
		try {
			conf.executeSchemaApi(conf1, true)
			fail { "Should have got an error" }
		}
		catch (ex: Exception) {
			println("Received exception $ex")
		}

		val conf2 = """
			{
			  "hasuraconfLoadSeparately": true,
			  "type": "bulk",
			  "args": [
			    {
			      "hasuraconfIgnoreError": {
			        "message":"column \"custom_user_data\" of relation \"calendar_user\" already exists",
			        "status_code":"42701"
			      },
			      "type": "run_sql",
			      "args": {
			        "sql": "ALTER TABLE \"public\".\"calendar_user\" ADD COLUMN \"custom_user_data\" text NULL;",
			        "cascade": false,
			        "read_only": false
			      }
			    }
			  ]
			}
		""".trimIndent()
		// Should not fail
		conf.executeSchemaApi(conf2, true)

		val conf3 = """
			{
			  "hasuraconfLoadSeparately": true,
			  "type": "bulk",
			  "args": [
			    {
			      "hasuraconfIgnoreError": {
			        "message":"column \"custom_user_data2\" of relation \"calendar_user\" already exists",
			        "status_code":"42701"
			      },
			      "type": "run_sql",
			      "args": {
			        "sql": "ALTER TABLE \"public\".\"calendar_user\" ADD COLUMN \"custom_user_data2\" text NULL;",
			        "cascade": false,
			        "read_only": false
			      }
			    }
			  ]
			}
		""".trimIndent()
		// Should not fail no mattter how many times we execute it
		conf.executeSchemaApi(conf3, true)
		conf.executeSchemaApi(conf3, true)
		conf.executeSchemaApi(conf3, true)

		val conf4 = """
			{
			  "hasuraconfLoadSeparately": true,
			  "type": "bulk",
			  "args": [
			    {
			      "hasuraconfIgnoreError": {
			        "message":"column \"any_field\" of relation \"bad_table\" already exists",
			        "status_code":"42701"
			      },
			      "type": "run_sql",
			      "args": {
			        "sql": "ALTER TABLE \"public\".\"bad_table\" ADD COLUMN \"any_field\" text NULL;",
			        "cascade": false,
			        "read_only": false
			      }
			    }
			  ]
			}
		""".trimIndent()
		// Should fail even for the first execution, because bad_table doesn't exist and so we won't get the
		// expected "column \"any_field\" of relation \"bad_table\" already exists" message, but
		// "relation \"public.bad_table\" does not exist","status_code":"42P01"
		try {
			conf.executeSchemaApi(conf4, true)
			fail { "Should have got an error" }
		}
		catch (ex: Exception) {
			println("Received exception $ex")
		}


		val conf5 = """
			{
			  "hasuraconfLoadSeparately": true,
			  "type": "bulk",
			  "args": [
			    {
			      "hasuraconfIgnoreError": {
			        "message":"column \"custom_user_data3\" of relation \"calendar_user\" already exists",
			        "status_code":"42701"
			      },
			      "type": "run_sql",
			      "args": {
			        "sql": "ALTER TABLE \"public\".\"calendar_user\" ADD COLUMN \"custom_user_data3\" text NULL;",
			        "cascade": false,
			        "read_only": false
			      }
			    },
			    {
			      "hasuraconfIgnoreError": {
			        "message":"column \"custom_user_data4\" of relation \"calendar_user\" already exists",
			        "status_code":"42701"
			      },
			      "type": "run_sql",
			      "args": {
			        "sql": "ALTER TABLE \"public\".\"calendar_user\" ADD COLUMN \"custom_user_data4\" text NULL;",
			        "cascade": false,
			        "read_only": false
			      }
			    },
			    {
			      "hasuraconfIgnoreError": {
			        "message":"column \"custom_user_data5\" of relation \"calendar_user\" already exists",
			        "status_code":"42701"
			      },
			      "type": "run_sql",
			      "args": {
			        "sql": "ALTER TABLE \"public\".\"calendar_user\" ADD COLUMN \"custom_user_data5\" text NULL;",
			        "cascade": false,
			        "read_only": false
			      }
			    }
			  ]
			}
		""".trimIndent()
		// Multiple operatioons, should not fail no matter how many times we execute it
		conf.executeSchemaApi(conf5, true)
		conf.executeSchemaApi(conf5, true)
		conf.executeSchemaApi(conf5, true)
	}


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
			"""{"actions":[{"definition":{"arguments":[{"name":"args","type":"UserAndCalendarInput!"}],"forward_client_headers":true,"handler":"{{HANDLER_URL}}","kind":"synchronous","output_type":"[UserAndCalendarOutput5!]","type":"mutation"},"name":"createUserAndCalendar5"},{"definition":{"arguments":[{"name":"args","type":"UserAndCalendarInput!"}],"forward_client_headers":true,"handler":"{{HANDLER_URL}}","kind":"synchronous","output_type":"UserAndCalendarOutput","type":"mutation"},"name":"createUserAndCalendar4"},{"definition":{"arguments":[{"name":"name","type":"String!"},{"name":"descriptions","type":"[String!]!"},{"name":"calendarTypes","type":"[CalendarType!]!"}],"forward_client_headers":true,"handler":"{{HANDLER_URL}}","kind":"synchronous","output_type":"String","type":"mutation"},"name":"createUserAndCalendar2"},{"definition":{"arguments":[{"name":"args","type":"UserAndCalendarInput!"}],"forward_client_headers":true,"handler":"{{HANDLER_URL}}","kind":"synchronous","output_type":"[String!]","type":"mutation"},"name":"createUserAndCalendar3"},{"definition":{"arguments":[{"name":"userName","type":"String!"},{"name":"name","type":"String!"},{"name":"description","type":"String!"},{"name":"calendarType","type":"CalendarType!"}],"forward_client_headers":true,"handler":"{{HANDLER_URL}}","kind":"synchronous","output_type":"String","type":"mutation"},"name":"createUserAndCalendar"},{"definition":{"arguments":[{"name":"args","type":"SignUpWithExternalRestApiInput!"}],"forward_client_headers":true,"handler":"http://some.rest.endpoint","kind":"synchronous","output_type":"SignUpWithExternalRestApiOutput","type":"mutation","request_transform":{"version":1,"method":"POST","url":"{{${'$'}base_url}}/signup/email-password","body":"{\"email\":{{${'$'}body.input.args.email}},\"password\":{{${'$'}body.input.args.password}}}","query_params":{},"template_engine":"Kriti"}},"name":"signUpWithExternalRestApi"}],"custom_types":{"enums":[{"name":"CalendarType","values":[{"value":"PRIVATE"},{"value":"PUBLIC"},{"value":"SHARED"}]}],"input_objects":[{"fields":[{"name":"name","type":"String!"},{"name":"description","type":"String!"},{"name":"isPublic","type":"Boolean!"},{"name":"hasColors","type":"Boolean"}],"name":"UserAndCalendarInput"},{"fields":[{"name":"email","type":"String!"},{"name":"password","type":"String!"}],"name":"SignUpWithExternalRestApiInput"}],"objects":[{"description":"The description of UserAndCalendarOutput","fields":[{"description":"The user's name","name":"userFullName","type":"String!"},{"description":"The user's age","name":"userAge","type":"Int"},{"description":"Field is not defined in Kotlin as nullable, but explicitly set so","name":"explicitNullable","type":"String"},{"description":"Field is defined in Kotlin as nullable, but explicitly set to not nullable","name":"explicitlyNotNullable","type":"String!"},{"description":"User identifier","name":"userId","type":"bigint!"},{"name":"calendarId","type":"bigint!"},{"name":"differentCalendarId","type":"bigint!"}],"name":"UserAndCalendarOutput5","relationships":[{"field_mapping":{"calendarId":"id"},"name":"calendar","remote_table":{"name":"calendar","schema":"public"},"type":"object"},{"field_mapping":{"differentCalendarId":"id"},"name":"otherCalendar","remote_table":{"name":"calendar","schema":"public"},"type":"object"}]},{"description":"The description of UserAndCalendarOutput","fields":[{"description":"The user's name","name":"userFullName","type":"String!"},{"description":"The user's age","name":"userAge","type":"Int"},{"description":"Field is not defined in Kotlin as nullable, but explicitly set so","name":"explicitNullable","type":"String"},{"description":"Field is defined in Kotlin as nullable, but explicitly set to not nullable","name":"explicitlyNotNullable","type":"String!"},{"description":"User identifier","name":"userId","type":"bigint!"},{"name":"calendarId","type":"bigint!"},{"name":"differentCalendarId","type":"bigint!"}],"name":"UserAndCalendarOutput","relationships":[{"field_mapping":{"calendarId":"id"},"name":"calendar","remote_table":{"name":"calendar","schema":"public"},"type":"object"},{"field_mapping":{"differentCalendarId":"id"},"name":"otherCalendar","remote_table":{"name":"calendar","schema":"public"},"type":"object"}]},{"fields":[{"name":"mfa","type":"String!"},{"name":"session","type":"String!"}],"name":"SignUpWithExternalRestApiOutput"}],"scalars":[{"description":"bigint type","name":"bigint"}]}}""",
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

	@DisplayName("Test action generation via HasuraConfigurator - 3")
	@Test
	fun testHasuraActionHander() {
		conf.hasuraSchemaEndpoint = "http://localhost:${hasuraContainer.getMappedPort(8080)}/v2/query"
		conf.hasuraMetadataEndpoint = "http://localhost:${hasuraContainer.getMappedPort(8080)}/v1/metadata"
		conf.hasuraAdminSecret = "hasuraconf"
		conf.actionRoots = listOf("com.beepsoft.hasuraconf.actions")
		val confData = conf.configure()
		conf.replaceConfiguration(confData)

		val hasuraGraphqlEndpoint = "http://localhost:${hasuraContainer.getMappedPort(8080)}/v1/graphql"
		println("serverBaseUrlInHasura: $serverBaseUrlInHasura")
		println("serverBaseUrlInHost: $serverBaseUrlInHost")
		println("hasuraGraphqlEndpoint: $hasuraGraphqlEndpoint")

		// First call an action with a request transform
		var res = executeHasuraGraphql(
			buildJsonObject {
				put("query", """
					mutation {
						signIn(args: {
							usernameOrEmail: "foo",
							password: "bar"
						}) {
							accessToken
						}
					}					
				""".trimMargin())
			}.toString(),
			hasuraGraphqlEndpoint
		)
		println(res)
		JSONAssert.assertEquals(
			res,
			"""{"data":{"signIn":{"accessToken":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"}}}""",
			false)

		// First call an actual action implemented as a Spring @RestController operation
		res = executeHasuraGraphql(
			buildJsonObject {
				put("query", """
					mutation {
  						startTask(args:{taskId:123}) {
    						executedTaskId
  						}
					}				
				""".trimMargin())
			}.toString(),
			hasuraGraphqlEndpoint
		)
		println(res)
		JSONAssert.assertEquals(
			res,
			"""{"data":{"startTask":{"executedTaskId":"123"}}}""",
			false)
	}

	private fun executeHasuraGraphql(json: String, endpoint: String, hasuraAdminSecret: String? = null) : String {
		val client = WebClient
			.builder()
			.baseUrl(endpoint)
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.let {
				if (hasuraAdminSecret != null) {
					it.defaultHeader("X-Hasura-Admin-Secret", hasuraAdminSecret)
				}
				it
			}
			.build()
		val request = client.post()
			.body<String, Mono<String>>(Mono.just(json), String::class.java)
			.retrieve()
			.bodyToMono(String::class.java)
		// Make it synchronous for now
		try {
			val result = request.block()
			HasuraConfigurator.LOG.debug("loadIntoHasura done {}", result)
			return result!!
		} catch (ex: WebClientResponseException) {
			HasuraConfigurator.LOG.error("executeHasuraGraphql failed", ex)
			HasuraConfigurator.LOG.error("executeHasuraGraphql response text: {}", ex.responseBodyAsString)
			throw ex
		}
	}

}
