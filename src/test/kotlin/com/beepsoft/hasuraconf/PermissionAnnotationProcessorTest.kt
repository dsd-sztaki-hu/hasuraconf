package com.beepsoft.hasuraconf

import com.beepsoft.hasuraconf.model.Calendar
import com.beepsoft.hasuraconf.model.Day
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

        private val LOG = getLogger(javaClass.enclosingClass)
        lateinit var postgresqlContainer: PostgreSQLContainer<*>

        init {
            val logConsumer = Slf4jLogConsumer(LOG);
            postgresqlContainer = PostgreSQLContainer<Nothing>("postgres:11.5-alpine").
                    apply {
                        withUsername("hasuraconf")
                        withPassword("hasuraconf")
                        withDatabaseName("hasuraconf")
                    }
            postgresqlContainer.start()
            postgresqlContainer.followOutput(logConsumer);
        }
        // Dynamic initialization of properties. This is necessary because we only know the
        // spring.datasource.url once the postgresqlContainer is up and running.
        class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
            override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
                TestPropertyValues.of(
                        "spring.datasource.url=" + postgresqlContainer.getJdbcUrl(),
                        "spring.datasource.username=" + postgresqlContainer.getUsername(),
                        "spring.datasource.password=" + postgresqlContainer.getPassword()
                ).applyTo(configurableApplicationContext.environment);
            }
        }
    }

    @Autowired lateinit var entityManagerFactory: EntityManagerFactory;

    @DisplayName("Test Hasura permission annotations")
    @Test
    fun testAnnotations() {
        val proc = PermissionAnnotationProcessor(entityManagerFactory);

        val sessionFactoryImpl = entityManagerFactory.unwrap(SessionFactory::class.java) as SessionFactoryImpl
        val metaModel = sessionFactoryImpl.metamodel as MetamodelImplementor
        var entity: EntityTypeDescriptor<*> = metaModel.entity(Calendar::class.java);
        var permissions = proc.process(entity);
        //println(permissions)

        //
        // Calendar has 4 permissions with inline json and jsonFile as well.
        //
        Assertions.assertEquals( 4, permissions.size, "Calendar permissions have 4 elements")

        val calSnapshot1 = readFileUsingGetResource("/calendar_perm_snapshot1.json");
        val calSnapshot2 = readFileUsingGetResource("/calendar_perm_snapshot2.json");
        val calSnapshot3 = readFileUsingGetResource("/calendar_perm_snapshot3.json");
        val calSnapshot4 = readFileUsingGetResource("/calendar_perm_snapshot4.json");
        val calSnapshot5 = readFileUsingGetResource("/calendar_perm_snapshot5.json");
        val calSnapshot6 = readFileUsingGetResource("/calendar_perm_snapshot6.json");


        Assertions.assertEquals( "", permissions[0].json, "Insert permission on Calendar")

        JSONAssert.assertEquals("Read permission on Calendar", permissions[1].json, calSnapshot2, false);
        JSONAssert.assertEquals("Update permission on Calendar", permissions[2].json, calSnapshot1, false);
        JSONAssert.assertEquals("Delete permission on Calendar", permissions[3].json, calSnapshot1, false);

        JSONAssert.assertEquals("Insert permission Hasura JSON on Calendar", permissions[0].toHasuraJson(), calSnapshot3, false);
        JSONAssert.assertEquals("Insert permission Hasura JSON on Calendar with other_schema", permissions[0].toHasuraJson("other_schema"), calSnapshot4, false);
        JSONAssert.assertEquals("Update permission Hasura JSON on Calendar", permissions[2].toHasuraJson(), calSnapshot5, false);
        JSONAssert.assertEquals("Delete permission Hasura JSON on Calendar with excluded fields", permissions[3].toHasuraJson(), calSnapshot6, false);

        //
        // Day has 1 permission with a jsonFile including another file.
        //
        entity = metaModel.entity(Day::class.java);
        permissions = proc.process(entity);
        //println(permissions)
        Assertions.assertEquals( 1, permissions.size, "Day permissions have 1 elements")

        val daySnapshot1 = readFileUsingGetResource("/day_perm_snapshot1.json");
        JSONAssert.assertEquals("Read permission on Day (with @include from jsonFile)", permissions[0].json, daySnapshot1, false);


    }

}