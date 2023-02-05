package com.beepsoft.hasuraconf

import com.beepsoft.hasuraconf.model.BaseObject
import com.beepsoft.hasuraconf.model2.Comment1
import com.beepsoft.hasuraconf.model4.ConstraintTestModel
import io.hasura.metadata.v3.metadataJson
import io.hasura.metadata.v3.toBulkRunSql
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.domain.EntityScan

@EntityScan(basePackageClasses=[BaseObject::class, ConstraintTestModel::class])
class CheckConstraintGenerationTest : IntegrationTestBase() {

    @Autowired
    lateinit var conf: HasuraConfigurator

    @DisplayName("Test generation of CHECK constraints based on JSR validation annotations")
    @Test
    fun testCheckConstraintGeneration() {
        conf.hasuraSchemaEndpoint = "http://localhost:${hasuraContainer.getMappedPort(8080)}/v2/query"
        conf.hasuraMetadataEndpoint = "http://localhost:${hasuraContainer.getMappedPort(8080)}/v1/metadata"
        conf.hasuraAdminSecret = "hasuraconf"

        var confData = conf.configure()
        var metadataJson = normalize(confData.metadataJson)
        println("Metadata JSON generated:\n${metadataJson}")

        // Check snapshot
        println("Bulk run sql generated:\n${confData.toBulkRunSql()}")
        var snapshot = readFileUsingGetResource("/check_constraints_generator_snapshot1.json")
        JSONAssert.assertEquals(snapshot, confData.toBulkRunSql().toString(), false)
        JSONAssert.assertEquals(confData.toBulkRunSql().toString(), snapshot, false)

        // Check loading to DB
        conf.loadBulkRunSqls(confData)
    }
}
