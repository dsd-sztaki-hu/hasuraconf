package com.beepsoft.hasuraconf

import com.beepsoft.hasuraconf.model.Calendar
import com.beepsoft.hasuraconf.model.Day
import com.beepsoft.hasuraconf.model.Layout
import com.beepsoft.hasuraconf.model.Operation
import org.hibernate.SessionFactory
import org.hibernate.internal.SessionFactoryImpl
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor
import org.hibernate.metamodel.spi.MetamodelImplementor
import org.junit.jupiter.api.Assertions
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
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.junit.jupiter.Testcontainers
import javax.persistence.EntityManagerFactory

/**
 * Tests HasuraConfigurator with Postgresql
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
@ContextConfiguration(initializers = [PermissionAnnotationProcessorTest.Companion.Initializer::class])
@Testcontainers
@ExtendWith(SpringExtension::class)
class PermissionAnnotationProcessorTest {

    companion object {

        private val LOG = getLogger(this::class.java.enclosingClass)
        var postgresqlContainer: PostgreSQLContainer<*>

        init {
            val logConsumer = Slf4jLogConsumer(LOG)
            postgresqlContainer = PostgreSQLContainer<Nothing>("postgres:11.5-alpine").
                    apply {
                        withUsername("hasuraconf")
                        withPassword("hasuraconf")
                        withDatabaseName("hasuraconf")
                    }
            postgresqlContainer.start()
            postgresqlContainer.followOutput(logConsumer)
        }
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

    @Autowired lateinit var entityManagerFactory: EntityManagerFactory

    @DisplayName("Test Hasura permission annotations")
    @Test
    fun testAnnotations() {
        val proc = PermissionAnnotationProcessor(entityManagerFactory)

        val sessionFactoryImpl = entityManagerFactory.unwrap(SessionFactory::class.java) as SessionFactoryImpl
        val metaModel = sessionFactoryImpl.metamodel as MetamodelImplementor
        var entity: EntityTypeDescriptor<*> = metaModel.entity(Calendar::class.java)
        var permissions = proc.process(entity)
        //println(permissions)

        //
        // Calendar has 4 permissions with inline json and jsonFile as well.
        //
        Assertions.assertEquals( 5, permissions.size, "Calendar permissions have 5 elements")

        val calSnapshot1 = readFileUsingGetResource("/calendar_perm_snapshot1.json")
        val calSnapshot2 = readFileUsingGetResource("/calendar_perm_snapshot2.json")
        val calSnapshot3 = readFileUsingGetResource("/calendar_perm_snapshot3.json")
        val calSnapshot4 = readFileUsingGetResource("/calendar_perm_snapshot4.json")
        val calSnapshot5 = readFileUsingGetResource("/calendar_perm_snapshot5.json")
        val calSnapshot6 = readFileUsingGetResource("/calendar_perm_snapshot6.json")
        val calSnapshot7 = readFileUsingGetResource("/calendar_perm_snapshot7.json")


        Assertions.assertEquals( "", permissions[0].json, "Insert permission on Calendar")

        println(permissions[1].json)
        JSONAssert.assertEquals("Read permission on Calendar", permissions[1].json, calSnapshot2, false)
        println(permissions[2].json)
        JSONAssert.assertEquals("Update permission on Calendar", permissions[2].json, calSnapshot1, false)
        println(permissions[3].json)
        JSONAssert.assertEquals("Delete permission on Calendar", permissions[3].json, calSnapshot1, false)
        println(permissions[1].toHasuraApiJson())
        JSONAssert.assertEquals("Select permission Hasura JSON on Calendar", permissions[1].toHasuraApiJson(), calSnapshot7, false)

        println(permissions[0].toHasuraApiJson())
        JSONAssert.assertEquals("Insert permission Hasura JSON on Calendar", permissions[0].toHasuraApiJson(), calSnapshot3, false)
        println(permissions[0].toHasuraApiJson("other_schema"))
        JSONAssert.assertEquals("Insert permission Hasura JSON on Calendar with other_schema", permissions[0].toHasuraApiJson("other_schema"), calSnapshot4, false)
        println(permissions[2].toHasuraApiJson())
        JSONAssert.assertEquals("Update permission Hasura JSON on Calendar", permissions[2].toHasuraApiJson(), calSnapshot5, false)
        println(permissions[3].toHasuraApiJson())
        JSONAssert.assertEquals("Delete permission Hasura JSON on Calendar with excluded fields", permissions[3].toHasuraApiJson(), calSnapshot6, false)

        //
        // Day has 1 permission with a jsonFile including another file.
        //
        entity = metaModel.entity(Day::class.java)
        permissions = proc.process(entity)
        //println(permissions)
        Assertions.assertEquals( 4, permissions.size, "Day permissions have 4 elements")

        val daySnapshot1 = readFileUsingGetResource("/day_perm_snapshot1.json")
        val daySnapshot2 = readFileUsingGetResource("/day_perm_snapshot2.json")
        JSONAssert.assertEquals("Read permission on Day (with @include from jsonFile)", permissions[1].json, daySnapshot1, false)
        JSONAssert.assertEquals("Full day SELECT permission metadata (with @include from jsonFile)", permissions[1].toHasuraApiJson(), daySnapshot2, false)
    }

    @DisplayName("Test Hasura permission annotations with defaults")
    @Test
    fun testAnnotationsWithDefaults() {
        val proc = PermissionAnnotationProcessor(entityManagerFactory)

        val sessionFactoryImpl = entityManagerFactory.unwrap(SessionFactory::class.java) as SessionFactoryImpl
        val metaModel = sessionFactoryImpl.metamodel as MetamodelImplementor
        var entity: EntityTypeDescriptor<*> = metaModel.entity(Layout::class.java)
        var permissions = proc.process(entity)

        // Generates hasuraMetadataJsons to stdout so that we can copy it back to test when it changes
        printHasuraMetadataJson(permissions)

        var hasuraMetadataJsons = listOf(
            listOf("SELECT", """{"role":"WORKER","permission":{"columns":["media_query","min_height"],"allow_aggregations":true,"filter":{}}}"""),
            listOf("SELECT", """{"role":"EDITOR","permission":{"columns":"*","allow_aggregations":true,"filter":{}}}"""),
            listOf("UPDATE", """{"role":"EDITOR","permission":{"columns":["created_at","tag","updated_at","data","description","max_height","max_width","media_query","min_height","min_width","mnemonic","theme_id","user_agent_regexp","id"],"check":null,"set":{},"filter":{}}}"""),
            listOf("DELETE", """{"role":"EDITOR","permission":{"filter":{}}}"""),
            listOf("SELECT", """{"role":"ALLDEFAULTS","permission":{"columns":["mnemonic","media_query"],"allow_aggregations":true,"filter":{"media_query":{"_eq":"10"}}}}"""),
            listOf("UPDATE", """{"role":"ALLDEFAULTS","permission":{"columns":["mnemonic","media_query"],"check":null,"set":{},"filter":{"media_query":{"_eq":"99"}}}}"""),
            listOf("SELECT", """{"role":"ROLE1","permission":{"columns":"*","allow_aggregations":true,"filter":{}}}"""),
            listOf("UPDATE", """{"role":"ROLE1","permission":{"columns":["created_at","tag","updated_at","data","description","max_height","max_width","media_query","min_height","min_width","mnemonic","theme_id","user_agent_regexp","id"],"check":null,"set":{},"filter":{"mnemonic":{"_eq":"foo"}}}}"""),
            listOf("INSERT", """{"role":"ROLE1","permission":{"columns":"*","check":{"mnemonic":{"_eq":"foo"}},"set":{}}}"""),
            listOf("UPDATE", """{"role":"ROLE2","permission":{"columns":["created_at","tag","updated_at","data","description","max_height","max_width","media_query","min_height","min_width","mnemonic","theme_id","user_agent_regexp","id"],"check":null,"set":{},"filter":{"mnemonic":{"_eq":"foo"}}}}"""),
            listOf("INSERT", """{"role":"ROLE2","permission":{"columns":"*","check":{"mnemonic":{"_eq":"foo"}},"set":{}}}"""),
        )

        permissions.forEachIndexed { index, permissionData ->
            Assertions.assertEquals(hasuraMetadataJsons[index][0], permissionData.operation.toString())
            Assertions.assertEquals(hasuraMetadataJsons[index][1], permissionData.toJsonObject().toString())
        }
    }

    @DisplayName("Test Hasura permission annotations with permutations")
    @Test
    fun testAnnotationsWithPermutations() {
        val proc = PermissionAnnotationProcessor(entityManagerFactory)

        val sessionFactoryImpl = entityManagerFactory.unwrap(SessionFactory::class.java) as SessionFactoryImpl
        val metaModel = sessionFactoryImpl.metamodel as MetamodelImplementor
        var entity: EntityTypeDescriptor<*> = metaModel.entity(Operation::class.java)
        var permissions = proc.process(entity)

        // Generates hasuraMetadataJsons to stdout so that we can copy it back to test when it changes
        printHasuraMetadataJson(permissions)

        var hasuraMetadataJsons = listOf(
            listOf("SELECT", """{"role":"AUTHOR","permission":{"columns":"*","allow_aggregations":false,"filter":{"id":{"_gt":10}}}}"""),
            listOf("SELECT", """{"role":"EDITOR","permission":{"columns":"*","allow_aggregations":false,"filter":{"id":{"_gt":10}}}}"""),
            listOf("INSERT", """{"role":"AUTHOR","permission":{"columns":"*","check":{"id":{"_gt":10}},"set":{}}}"""),
            listOf("INSERT", """{"role":"EDITOR","permission":{"columns":"*","check":{"id":{"_gt":10}},"set":{}}}"""),
            listOf("UPDATE", """{"role":"AUTHOR","permission":{"columns":"*","check":null,"set":{},"filter":{"id":{"_gt":10}}}}"""),
            listOf("UPDATE", """{"role":"EDITOR","permission":{"columns":"*","check":null,"set":{},"filter":{"id":{"_gt":10}}}}"""),
            listOf("DELETE", """{"role":"AUTHOR","permission":{"filter":{"id":{"_gt":10}}}}"""),
            listOf("DELETE", """{"role":"EDITOR","permission":{"filter":{"id":{"_gt":10}}}}"""),
            listOf("SELECT", """{"role":"WORKER","permission":{"columns":"*","allow_aggregations":true,"filter":{"name":{"_like":"%some_value%"}}}}"""),
            listOf("SELECT", """{"role":"BOSS","permission":{"columns":"*","allow_aggregations":true,"filter":{"name":{"_like":"%some_value%"}}}}"""),
            listOf("UPDATE", """{"role":"WORKER","permission":{"columns":"*","check":null,"set":{},"filter":{"name":{"_like":"%some_value%"}}}}"""),
            listOf("UPDATE", """{"role":"BOSS","permission":{"columns":"*","check":null,"set":{},"filter":{"name":{"_like":"%some_value%"}}}}"""),
        )

        permissions.forEachIndexed { index, permissionData ->
            Assertions.assertEquals(hasuraMetadataJsons[index][0], permissionData.operation.toString())
            Assertions.assertEquals(hasuraMetadataJsons[index][1], permissionData.toJsonObject().toString())
        }

    }

    private fun printHasuraMetadataJson(permissions: List<PermissionData>)  {
        // Generates hasuraMetadataJsons to stdout so that we can copy it back to test when it changes
        println("""        var hasuraMetadataJsons = listOf(""")
        permissions.forEach {
            println("            listOf(\"${it.operation}\", \"\"\"${it.toJsonObject()}\"\"\"),")
        }
        println("""        )""")
    }
}
