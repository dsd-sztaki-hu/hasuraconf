[![CircleCI](https://circleci.com/gh/dsd-sztaki-hu/hasuraconf.svg?style=svg)](https://circleci.com/gh/dsd-sztaki-hu/hasuraconf)

# Hasura Configuration Based on JPA Annotations

`HasuraConfigurator` can be used to generate a [JSON file](https://docs.hasura.io/1.0/graphql/manual/api-reference/index.html) that can be loaded into [Hasura](https://hasura.io/) to configure it. 

Configuration of Hasura is usually done via the Hasura console, however during development the database schema may change a lot and it is tedious to always set table and relationship tracking, permissions etc. on a newly generated database. 

With `HasuraConfigurator` when the data model changes in Java/Kotlin the Hasura configurations can be regenerated and automatically loaded into Hasura thus the data model and the Hasura configuration will always be in sync. 

# Notable changes

## 0.8.0

- Adds support for permissions on @ManyToMany join tables
- Adds support for computed fields with possibility to define the backing sql function inline in the @HasuraComputedFields annotation.

For example, the following would define a computed field `titleAndDescription` which would call the SQL function defined with the `functionDefinition` parameter. Also, in HasuraPermission you can now define permissions for the `titleAndDescription` field in the `fields` or `excludeFields` parameters:

```kotlin
@Entity
@HasuraPermissions(
    [
        HasuraPermission(
            operation = HasuraOperation.SELECT,
            role = "FRIEND",
            fields = ["tag", "titleAndDescription"],
            json = "{roles: { user_id: { _eq: 'X-Hasura-User-Id' } }}",
            allowAggregations = true
        )
    ]
)
class Calendar : BaseObject() {

    /** Title of the calendar  */
    var title: String? = null

    /** Short description of the calendar  */
    var description: String? = null

    @HasuraComputedField(
        functionName = "title_and_description",
        functionDefinition = """
            CREATE OR REPLACE FUNCTION title_and_description(calendar_row calendar)
            RETURNS TEXT as ${'$'}${'$'}
                SELECT calendar_row.title || '---' || calendar_row.description
            ${'$'}${'$'} LANGUAGE sql STABLE;
        """
    )
    @Transient
    var titleAndDescription: String? = null
}

```

## 0.7.0

Adds support for generating Actions.

## 0.6.0

Version 0.6.0 adds `HasuraStaticConfigurator`, which can be used to execute Hasura API "bulk" operations in an idempontent way ie. if an operation fails, we can define a match criteria for the errors which are "acceptable" and thus can be ignorred. This way the same bulk operations can be executed any number of times without failing for reasons that we know in advance can be safely ignorred.   

## 0.5.0

Version 0.5.0 the configuration algorithm has been completely rewritten and `hasuraconf.confFile` and `hasuraconf.loadConf` "bulk JSON" based configuration is now deprecated. Use `hasuraconf.metadataFile`/`hasuraconf.loadMetadata` and `hasuraconf.cascadeDeleteFile`/`hasuraconf.loadCascadeDelete` instead. Old format `confJson` is still generated, but its loading into Hasura is extremely slow compared to the new metadata loading mechanism, which is now the predferred way.

# Configurations this tool handles
* Tracking for tables that are defined as JPA entities.

* Tracking of relationships that match the relationships defined in JPA.

* Custom GraphQL root field generation. For an entity named `SomeEntity` the root queries/mutations will be renamed from → to:
    * insert → createSomeEntites
    * insert_one → createSomeEntity
    * update → updateSomeEntites
    * update_by_pk → updateSomeEntity
    * delete → deleteSomeEntities
    * delete_by_pk → deleteSomeEntity
    * select → someEntites
    * select_by_pk → someEntity
    * select_aggregate → someEntityAggregate

* Root field names can be redefined individually using the `@HasuraRootFields` annotation. Here you can define either a `baseName`, which will be used instead of the default base name (`SomeEntity` above) derived from the class name, or you can sepcify a custom name for each of the root fields with the matching field of the `@HasuraRootFields` annotation.

* Root field name generation can be globally redefined by providing an implementation of  `RootFieldNameProvider` as a @Bean. (Its default implementation `DefaultRootFieldNameProvider` provides the names `createSomeEntites`, `createSomeEntity`, etc. listed above).

* Custom column name definition for fields so that they appear in Hasura generated graphql schema the same way as they are defined in the Java models. Hasura, by default, generates names in the graphql schema based on the Postgresql field names which are in snake_case. The custom column name definitions converts these back to camelCase. Ie. If there's a field in Java called `veryLongPropertyName`, it becomes `very_long_property_name` in the database, but `HasuraConfigurator` generates a configuration json to make it appear as `veryLongPropertyName` again in the graphql schema

* Default cutom column names can be redefined using the `@HasuraAlias` annotation:
    * `fieldAlias`: Instead of using the field name as defined in Java, use this alias

    * Values relevant for `@ManyToMany` relationships 
    
        * `joinColumnAlias` or `keyColumnAlias`: In case of many-to-many associations use this explicit name instead of the one generated based on the in the join column / key column name (which is also settable via `@JoinTable(joinColumn=...)`)

        * `inverseJoinColumnAlias` or `relatedColumnAlias`: In case of many-to-many associations use this explicit name instead of the one generated based on the in the inverse join / related column column name (which is also settable via `@JoinTable(inverseJoinColumn=...)`)
        
        * `joinFieldAlias` In case of many-to-many association the object reference's name is derived from the joined table's name (by convertin it from snake_case to camelCase). In most cases this is satisfactory, but sometimes you want to override this behaviour. `joinFieldAlias` can be used in this case

        * rootFieldAliases: a `@HasuraRootFields` annotation defining redefinitions for the join table's root fields.    

* Mapping Java enums to Hasura enums using the `@HasuraEnum` annotations. For this to work the Java enum must be defined as a `@Entity` and it must follow the [Hasura enum table format](https://docs.hasura.io/1.0/graphql/manual/schema/enums.html#creating-an-enum-compatible-table ) 

* Generation of select/insert/update/delete permission rules using `@HasuraPermissions` and `@HasuraPermission` annotations. With `@HasuraPermission` the same JSON can be provided, which can also be constructed in the "With custom checks" permission settings. `HasuraConfigurator` uses a relaxed JSON syntax (JSON5) in the annotations, and also support includion of permission JSON from files (classpath) as well inclusion in one file from another thus providing flexibility in composing permission rules.

* Generation of cascading delete triggers via `@HasuraGenerateCascadeDeleteTrigger` in cases where `@OnDelete(action=OnDeleteAction.CASCADE)` cannot be used because the parent is referencing the child and not the child the parent

* For JPA annotated relationships that you do not want to appear in the graphql add `@HasuraIgnoreRelationship`. (By default all relationships present in the model will automatically will be tracked and be present in the graphql model)

* Starting from version `0.5.0` `HasuraConfigurator` now also generates [Hasura Metadata V2](https://github.com/hasura/graphql-engine/blob/master/contrib/metadata-types/src/metadata-schemas/HasuraMetadataV2.schema.json) JSON accessible as `HasuraConfigurator.metadataJson`. This JSON can be used as the `args` to the Hasura [replace_metadata](https://hasura.io/docs/1.0/graphql/core/api-reference/schema-metadata-api/manage-metadata.html#replace-metadata) API operation.

* Generation of Hasura actions based on Java/Kotlin methods/functions. A method or function may be annotated with `@HasuraAction` and as a result `HasuraConfigurator` will generate Hasura actions metadata with the name of the method/function. As part of the action generation it also generates the necessary input types based on the method/function parameters and output types based on the method/function return type

TODO:
* More documentation on use of `HasuraConfigurator` especially `@HasuraPermission`.

* Annotation for generating trigger that sets default value for fields if a value is not already set. `@HasuraDefaultValueTrigger(value="defaultValue")`

* Annotation for generating "Check Constraints" in hasura-conf.json (Later: a way to generate directives based on the constrains into the graphql schema? This way the clients of Hasura could not what validations to perform on data before sending to Hasura)

# Usage

The easiest way to use it in a Maven project is via jitpack

```
<repositories>
    ...
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
...

<dependencies>
    <dependency>
        <groupId>com.github.beepsoft</groupId>
        <artifactId>hasuraconf</artifactId>
        <version>0.8.4</version>
    </dependency>
</dependencies>
```

Then you should use `HasuraConfigurator` like this:

```java
@Autowired HasuraConfigurator hasuraConf;
...
// Setting these two to true will fully configure Hasura
hasuraConf.setLoadMetadata(true);
hasuraConf.setLoadCascadeDelete(true);

// Generates metadataJson and cascadeDeleteJson and loads them to Hasura
hasuraConf.configure();

// See what has been generated
String metadataJson = hasuraConf.getMetadataJson();
String cascadeDeleteJson = hasuraConf.getMetadataJson();
String jsonSchema = hasuraConf.getJsonSchema();
```

```kotlin
@Autowired hasuraStaticConf: HasuraSaticConfigurator 
...
hasuraStaticConf.loadStaticConf(
"""
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
)





```

To provide global root field naming create a bean of type RootFieldNameProvider. For example:

```kotlin
@Configuration
class MyConfiguration {
    class MyRootFieldNameProvider : RootFieldNameProvider {
        override fun rootFieldFor(fieldName: String, entityName: String, entityNameLower: String, tableName: String) : String {
            return when(fieldName) {
                "select" -> "my_select_$tableName"
                "selectByPk" -> "my_selectByPk_$tableName"
                "selectAggregate" -> "my_selectAggregate_$tableName"
                "insert" -> "my_insert_$tableName"
                "insertOne" -> "my_insertOne_$tableName"
                "update" -> "my_update_$tableName"
                "updateByPk" -> "my_updateByPk_$tableName"
                "delete" -> "my_delete_$tableName"
                "deleteByPk" -> "my_deleteByPk_$tableName"
                else -> throw HasuraConfiguratorException("Unknown root field name: $fieldName")
            }
        }
    }

    @Bean
    fun customProvider(): RootFieldNameProvider
    {
        return MyRootFieldNameProvider()
    }
}
```

## Action generation

Annotate a function to generate an action based on it. The only requirement  is to define `handler` URL, or the enviroment variable configured in Hasura, where the action will be served.   

```kotlin
@HasuraAction(
    handler = "{{HANDLER_URL}}"
)
fun createUserAndCalendar(
    args: UserAndCalendarInput
): String {
    TODO()
}

data class UserAndCalendarInput(
    val userName: String,
    val name: String,
    val description: String
)

@HasuraAction(
    handler = "{{HANDLER_URL}}",
)
fun createUserAndCalendar2(
    name: String,
    description: String,
    calendarType: CalendarType
): UserAndCalendar
{
    TODO()
}

data class UserAndCalendar(
    var userName: String,

    @HasuraField(type="bigint!")
    var userId: Long,

    @HasuraRelationship
    var calendar: Calendar,

    @HasuraRelationship(
        name="otherCalendar",
        remoteTable = "calendar",
        fieldMappings = [
            HasuraFieldMapping(fromField="differentCalendarId", toField="id")
        ]
    )
    var differentCalendarId: Long
)

enum class CalendarType {
    PRIVATE,
    PUBLIC,
    SHARED
}
```

Set `actionsRoot` of `HasuraConfigurator` to define the packages where to look for `@HasuraAction` annotation methods/functions

```kotlin
@Autowired 
var hasuraConf: HasuraConfigurator
...
hasuraConf.actionRoots = listOf("some.package.name")
hasuraConf.configure()
print(hasuraConf.actions)
```

It generates tthe following actions metadata JSON:

```
{
  "actions": [
    {
      "name": "createUserAndCalendar",
      "definition": {
        "handler": "{{HANDLER_URL}}",
        "type": "mutation",
        "kind": "synchronous",
        "forward_client_headers": true,
        "output_type": "String",
        "arguments": [
          {
            "name": "args",
            "type": "UserAndCalendarInput"
          }
        ]
      }
    },
        {
      "name": "createUserAndCalendar2",
      "definition": {
        "handler": "{{HANDLER_URL}}",
        "type": "mutation",
        "kind": "synchronous",
        "forward_client_headers": true,
        "output_type": "UserAndCalendar",
        "arguments": [
          {
            "name": "name",
            "type": "String"
          },
          {
            "name": "description",
            "type": "String"
          },
          {
            "name": "calendarType",
            "type": "CalendarType"
          }
        ]
      }
    }
  ],
  "custom_types": {
    "input_objects": [
      {
        "name": "UserAndCalendarInput",
        "fields": [
          {
            "name": "userName",
            "type": "String"
          },
          {
            "name": "name",
            "type": "String"
          },
          {
            "name": "description",
            "type": "String"
          }
        ]
      }
    ],
    "objects": [
      {
        "name": "UserAndCalendar",
        "fields": [
          {
            "name": "userName",
            "type": "String"
          },
          {
            "name": "userId",
            "type": "bigint!"
          },
          {
            "name": "calendarId",
            "type": "bigint"
          },
          {
            "name": "differentCalendarId",
            "type": "bigint"
          }
        ],
        "relationships": [
          {
            "name": "calendar",
            "type": "object",
            "remote_table": {
              "name": "calendar",
              "schema": "public"
            },
            "field_mappings": [
              {
                "calendarId": "id"
              }
            ]
          },
          {
            "name": "otherCalendar",
            "type": "object",
            "remote_table": {
              "name": "calendar",
              "schema": "public"
            },
            "field_mappings": [
              {
                "differentCalendarId": "id"
              }
            ]
          }
        ]
      }
    ],
    "scalars": [],
    "enums": [
      {
        "name": "CalendarType",
        "values": [
          {
            "name": "PRIVATE"
          },
          {
            "name": "PUBLIC"
          },
          {
            "name": "SHARED"
          }
        ]
      }
    ]
  }
}
```
## JSON Schema generation

The Hasura graphql schema doesn't expose some important aspects of the model, for example the nature of array references. An array reference can be either a `@OneToMany` or `@ManyToMany` relationship. In case of a `@OneToMany` the foreign key is mapped by the "many" side, while in case of `@ManyToMany` Hibernate connects the two entities via a generated join table.

The Hasura graphql doesn't expose any information about the constraints of the entities and their fields either, for example, if they are mandatory, how to validate them, etc.

So, to complements the graphql schema `hasuraconf` can be used to also generate a JSON Schema based on the JPA and `javax.validation` annotations. We use the `https://github.com/victools/jsonschema-generator` project to generate the JSON schema with our own extensions in the `hasura` property of types or properties.

Take for example for this entity:

```kotlin
@MappedSuperclass
abstract class BaseObject {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @ReadOnly
    var id: Long? = null

    var createdAt: Date? = null
    var updatedAt: Date? = null

    @Column(unique = true)
    @ReadOnly(exceptAtCreation = true)
    var tag: String? = null
}

class Calendar : BaseObject() {

    /** Creator/owner of the calendar  */
    @OneToMany(mappedBy = "calendar", cascade = [CascadeType.ALL])
    @OnDelete(action= OnDeleteAction.CASCADE)
    var roles: List<CalendarRole>? = null

    /** Title of the calendar  */
    var title: String? = null

    /** Short description of the calendar  */
    var description: String? = null

    /** Locale language  */
    var localeLang: String? = null

    /** Locale country  */
    var localeCountry: String? = null

    /** Boxes making up the calendar  */
    @OneToMany(mappedBy = "calendar", cascade = [CascadeType.ALL])
    @OnDelete(action= OnDeleteAction.CASCADE)
    var days: List<Day>? = null

    /** Availability of the calendar.  */
    @ManyToOne
    var availability: Availability? = null

    /** Published or not.  */
    @Column(columnDefinition = "boolean default false", nullable = false)
    var published: Boolean = false

    /** Version number of calendar.  */
    var version: Int? = null

    /** Previous version.  */
    @OneToOne
    @HasuraGenerateCascadeDeleteTrigger
    var previousVersion: Calendar? = null

    /** Next version.  */
    @OneToOne
    @HasuraGenerateCascadeDeleteTrigger
    var nextVersion: Calendar? = null

    /** Theme descriptor of the calendar.  */
    @OneToOne(optional = true)
    @HasuraIgnoreRelationship
    var theme: Theme? = null

    /** Theme specific JSON config.  */
    var themeConfig: String? = null

    @ManyToMany
    @JoinTable(name="user_calendar", joinColumns=arrayOf(JoinColumn(name="the_calendar_id")), inverseJoinColumns=arrayOf(JoinColumn(name="the_user_id")))
    @OnDelete(action=OnDeleteAction.CASCADE)
    var users: List<CalendarUser>? = null

    @Entity
    @Table(name = "calendar_availability")
    @HasuraEnum
    enum class Availability(
            @Column(columnDefinition = "TEXT")
            var description: String) {
        PRIVATE("Only users with explicit role have read/write access to the calendar"),
        PUBLIC("Anyone has read access to the calendar");


        @Id
        @Column(columnDefinition = "TEXT")
        var value = toString()

    }
}

```

... this is the JSON Schema generated . Note the `hasura` extension prop in the type and in some of the properties
```json
{
    "Calendar" : {
      "type" : "object",
      "properties" : {
        "availability" : {
          "$ref" : "#/$defs/Availability",
          "hasura" : {
            "relation" : "many-to-one",
            "reference" : "availabilityValue",
            "referenceType" : "string"
          }
        },
        "createdAt" : {
          "type" : "string",
          "format" : "date-time"
        },
        "days" : {
          "hasura" : {
            "relation" : "one-to-many",
            "mappedBy" : "calendar",
            "parentReference" : "calendarId"
          },
          "type" : "array",
          "items" : {
            "$ref" : "#/$defs/Day"
          }
        },
        "description" : {
          "type" : "string"
        },
        "id" : {
          "type" : "integer",
          "hasura" : {
            "graphqlType" : "bigint"
          },
          "readOnly" : true
        },
        "localeCountry" : {
          "type" : "string"
        },
        "localeLang" : {
          "type" : "string"
        },
        "nextVersion" : {
          "$ref" : "#/$defs/Calendar",
          "hasura" : {
            "relation" : "one-to-one",
            "reference" : "nextVersionId",
            "referenceType" : "integer"
          }
        },
        "previousVersion" : {
          "$ref" : "#/$defs/Calendar",
          "hasura" : {
            "relation" : "one-to-one",
            "reference" : "previousVersionId",
            "referenceType" : "integer"
          }
        },
        "published" : {
          "type" : "boolean"
        },
        "roles" : {
          "hasura" : {
            "relation" : "one-to-many",
            "mappedBy" : "calendar",
            "parentReference" : "calendarId"
          },
          "type" : "array",
          "items" : {
            "$ref" : "#/$defs/CalendarRole"
          }
        },
        "tag" : {
          "type" : "string",
          "readOnly" : true,
          "hasura" : {
            "allowWriteAtCreation" : true
          }
        },
        "theme" : {
          "$ref" : "#/$defs/Theme",
          "hasura" : {
            "relation" : "one-to-one",
            "reference" : "themeId",
            "referenceType" : "integer"
          }
        },
        "themeConfig" : {
          "type" : "string"
        },
        "title" : {
          "type" : "string"
        },
        "updatedAt" : {
          "type" : "string",
          "format" : "date-time"
        },
        "users" : {
          "hasura" : {
            "relation" : "many-to-many",
            "join" : {
              "$ref" : "#/$defs/UserCalendar",
              "reference" : "theUserId",
              "item" : "calendarUser",
              "parentReference" : "theCalendarId"
            }
          },
          "type" : "array",
          "items" : {
            "$ref" : "#/$defs/CalendarUser"
          }
        },
        "version" : {
          "type" : "integer"
        }
      },
      "hasura" : {
        "graphqlType" : "calendar",
        "idProp" : "id",
        "rootFieldNames" : {
          "delete" : "deleteCalendars",
          "deleteByPk" : "deleteCalendar",
          "insert" : "createCalendars",
          "insertOne" : "createCalendar",
          "select" : "calendars",
          "selectAggregate" : "calendarAggregate",
          "selectByPk" : "calendar",
          "update" : "updateCalendars",
          "updateByPk" : "updateCalendar"
        },
        "properties" : {
          "availabilityValue" : {
            "type" : "string"
          },
          "nextVersionId" : {
            "type" : "integer"
          },
          "previousVersionId" : {
            "type" : "integer"
          },
          "themeId" : {
            "type" : "integer"
          }
        }
      }
    },
    "CalendarRole" : {},
    "CalendarUser" : {},
    "Theme" : {},
    "Day": {}
}
```

The JSON Schema describes the relations of objects as they are defined in Java. Eg. `Calendar` has a List of `CalendarRole`s associated with it and this is represented by JSON Schema as

```
         "roles" : {
           "type" : "array",
           "items" : {
             "$ref" : "#/$defs/CalendarRole"
           }
         },
```

To add information about the SQL relationship between `Calendar` and `CalendarRole` we added some hasura extension props  describing that this is a one-to-many relationship and it is mapped by the `CalendarRole` entity's `calendar` field
```
           "hasura" : {
             "relation" : "one-to-many",
             "mappedBy" : "calendar"
           },
```

All these information can be used by GUI clients to be able to automatically generate listings, forms etc based on the graphql and JSON Schema of a Hasura exposed database.


## Hasura extension values on properties

Common properties:

* `relation`: can be `one-to-one`, `one-to-many`, `many-to-one`, `many-to-many`
* `reference`: in case this entity holds the reference to the other entity  (ie. holds its ID as a foreign key), then `reference`'s value is the name of the field in the current type holding this reference. In the example above Calnedars can have versions and the `previousVersion` property is a reference to an older `Calendar` object. The relatiosnship between the two calendars are  actually store in SQL in a field calles `previous_version_id`, which is actually exposed by hasuraconf as `previousVersionId`. So, when working with Calendars and we want to make the "previous version" connection between two Calendars althouh we have a `previousVersion` field where we can navigate but we can also know that to edit this relationship we have to set the `previousVersionId` on the new Calendar.
* `referenceType`: the JSON schema type of the `reference`, ie.  the type of the foreign ID field.
* `mappedBy`: in case of one-to-one and one-to-many relations the reference maynot be hold by the source entity but the target entity. In this case `mappedBy` defines the property that is actually responsible for holding this reference in the target.
* `parentReference`: in case on one-to-many relations the field holding the reference (foreign key) of the owning/parent entity.
* `graphqlType`: the graphql type of field. Used on key/ID fields and join type's order property.

In case of many-to-many relationships only:
* `join`: property holding information about the join entity. In the example above `Calendar` has a `users` relationship to `CalendarUsers`.
    * `$ref`: the actual entity type holding the two-way references of the relationship. These join entoties are not visible in Java, these are implementation details from the Java author's point of view. However, for the grpahql/JSON Schema these msut be handled as first class types. To help in this hasuraconf automatically generates a JSON Schema type for the join entities and their type is provided in this field. The type is provided in the "$ref" format,eg. `"#/$defs/UserCalendar"`
    * `item`: the field in the join entity, which cen be used to navigate to the actual target. In case of the `users` property the `CalendarUsers.calendarUser` property will result in the connected CalendarUser
    * `reference`: the field in the join entity actually holding the reference. For the above case `theUserId` field holds the reference, so to change where `CalendarUsers.calendarUser` navigates, we have to change the ID in this field.
    * `parentReference`: the field holding the reference to the parent from where we can navigate to the referenced entity.

## Hasura extension values on types

* `graphqlType`: the type name used in Hasura graphql.
* `idProp`: name of the ID - primary key - property
* `properties`: lists "hidden" reference properties. These are the properties holding ID references to other entities. These are not part of the Java class definitions,  but these are present in the Hasura graphql schema and we reprexent these here in the usual JSON Schema `properties` format.
* `rootFieldNames`: alias for root field names. HasuraConfigurator provides default aliases for these, which can be overriden using `@HasuraRootFields`. See also: https://hasura.io/docs/1.0/graphql/manual/api-reference/schema-metadata-api/table-view.html#set-table-custom-fields

## Configuration properties (application.properties values)

* `hasuraconf.metadataFile`: metadata JSON will be saved here. Default: `metadata.json`

* `hasuraconf.loadMetadata`: if true the generated metadata will  be loaded automatically to Hasura using [`replace_metadata`](https://hasura.io/docs/1.0/graphql/core/api-reference/schema-metadata-api/manage-metadata.html#replace-metadata) when `HasuraConfigurator#configure()` is called. For this to work `hasuraconfig.hasuraEndpoint` and `hasuraconfig.hasuraAdminSecret` must be specified. Startign from version 0.5.0 metadata json based Hasura configuration is the preferred solution as `confJson` based bulk operations are very slow. Default: `false`

* `hasuraconf.cascadeDeleteFile`: cascade delete configuration will be saved here. Default: `cascade-delete.json`

* `hasuraconf.loadCascadeDelete`: if true the generated config will be loaded automatically to Hasura when `HasuraConfigurator#configure()` is called. For this to work `hasuraconfig.hasuraEndpoint` and `hasuraconfig.hasuraAdminSecret` must be specified. When `loadMetadata` is set to true, you usually also want to set `loadCascadeDelete` to true, because these two loads can fully replace the functionality of loadConf.  Default: `false`


* `hasuraconf.confFile`: *Deprecated*: old style bulk configuration JSON will be saved here. Default: `hasura-conf.json`

* `hasuraconf.loadConf`: *Deprecated*: if true the generated config will also be loaded automatically to Hasura when `HasuraConfigurator#configure()` is called. For this to work `hasuraconfig.hasuraEndpoint` and `hasuraconfig.hasuraAdminSecret` must be specified. Default: false

* `hasuraconf.schemaName`: the Postgresql schema of the model. Default: public

* `hasuraconf.hasuraEndpoint`: URL of the Hasura server. Default: http://localhost:8080/v1/query

* `hasuraconf.hasuraAdminSecret`: admin secret of the Hasura server. Default: null

* `hasuraconf.schemaFile`: name of the JSON Schema file to generate. Not set by default, in which case it doesn't generate anything.

* `hasuraconf.schemaVersion`: version of the schema. Supported values: DRAFT_7, DRAFT_2019_09. Defaults to DRAFT_2019_09

* `hasuraconf.customPropsFieldName`: the field holding the hasura specific extenion properties. Defaults to `hasura`

* `hasuraconf.jsonSchema.ignore`: if true, JSON schema generation will ignorred. Defaults to `false`

* `hasuraconf.actionRoots`: comma separated list of package names where to look for `@HasuraAction` annotations. Defaults to `null`.


All `hasuraconf.XXX` values can be set on `HasuraConfigurator` directly before calling `configure()`.

# Caveat

hasuraconf has been tailored specifically to work with Hibernate and there are a couple of assumptions made:

1. All table generation is done by Hibernate. This allows hasuraconf to know how the table names will be derived from class names. This also means that the point of truth regagrding the structure of the database should be defined in Java using JPA annotations. hasuraconf - probably - won't work with an existing database layout that is mapped to Hibernate 
2. Table names are not aliased using `@Table(name=...)`. See the point above. If you use table name aliases the generated JSON Schema and our knowledge of how java class names and table names map to each other will not be in sync.

# Implementation

Written in Kotlin, or more precisely converted from Java to Kotlin and trying to make it more and more idiomatic Kotlin as I proceed learning the language.

# Testing

`HasuraConfiguratorIntegrationTests` uses [Testcontainers](https://www.testcontainers.org/) so you need Docker to run the tests.

# Why?
I usually define my data model in Java using JPA/Hibernate even if the resulting application may not be in Java/Kotlin. While working with Java/Hasura one thing that frustrated me is that whenever I regenerated my database the old Hasura configurations may got invalid and so I had to reset all table tracking, relation tracking etc. again and again.

Instead doing that I now use `HasuraConfigurator`  to keep my data model and Hasura configuratiosn in sync. This assumes, of course, that all configuration is done via `HasuraConfigurator` otherwise `HasuraConfigurator` clears/replaces any hand made configuration in Hasura. 

My goal is to add as many configuration options as possible all based on the JPA model and specific `HasuraConfigurator` annotations.

# Author

Balázs E. Pataki [[@beepsoft](https://github.com/beepsoft)]

# LICENSE

MIT
