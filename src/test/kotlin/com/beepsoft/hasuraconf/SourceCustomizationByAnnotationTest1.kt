package com.beepsoft.hasuraconf

import com.beepsoft.hasuraconf.model.BaseObject
import com.beepsoft.hasuraconf.model2.Comment1
import io.hasura.metadata.v3.metadataJson
import io.hasura.metadata.v3.toCascadeDeleteJson
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.domain.EntityScan


/**
 * Tests HasuraConfigurator with a custom source customization provided by @HasuraSourceCustomization annotation
 */
@EntityScan(basePackageClasses=[BaseObject::class, Comment1::class])
class SourceCustomizationByAnnotationTest1 : IntegrationTestBase() {

	@Autowired lateinit var conf: HasuraConfigurator

	@DisplayName("Test generated hasura conf JSON validity with snapshot")
	@Test
	fun testJsonWithSnapshot() {
		var confData = conf.configure()

		var snapshot = readFileUsingGetResource("/metadata_snapshot3.json")
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
		snapshot = readFileUsingGetResource("/json_schema_snapshot2.json")
		JSONAssert.assertEquals(confData.jsonSchema, snapshot, false)
		JSONAssert.assertEquals(snapshot, confData.jsonSchema, false)
	}
}

