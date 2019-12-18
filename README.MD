[![CircleCI](https://circleci.com/gh/beepsoft/hasuraconf.svg?style=svg)](https://circleci.com/gh/beepsoft/hasuraconf)

# Hasura Configuration Based on JPA Annotations

`HasuraConfigurator` can be used to generate a [JSON file](https://docs.hasura.io/1.0/graphql/manual/api-reference/index.html) that can be loaded into [Hasura](https://hasura.io/) to configure it. 

Configuration of Hasura is usually done via the Hasura console, however during development the database schema may change a lot and it is tedious to always set table and relationship tracking, permissions etc. on a newly generated database. 

With `HasuraConfigurator` when the data model changes in Java the Hasura configurations can be regenerated and automatically loaded into Hasura thus the data model and the Hasura configuration will always be in sync. 

Configurations this tool handles:
* Tracking for tables that are defined as JPA entities.

* Tracking of relationships that match the relationships defined in JPA.

* Custom GraphQL root field generation. For an entity named `SomeEntity` the root queries/mutations will be renamed from → to:
    * delete → deleteSomeEntity
    * insert → createSomeEntites
    * select → someEntites
    * update → updateSomeEntites
    * select_by_pk → someEntity
    * select_aggregate → someEntityAggregate

* Custom column name definition for fields so that they appear in Hasura generated graphql schema the same way as they are defined in the Java models. Hasura, by default, generates names in the graphql schema based on the Postgresql field names which are in snake_case. The custom column name definitions converts these back to camelCase. Ie. If there's a field in Java called `veryLongPropertyName`, it becomes `very_long_property_name` in the database, but `HasuraConfigurator` generates a configuration json to make it appear as `veryLongPropertyName` again in the graphql schema

* Mapping Java enums to Hasura enums using the `@HasuraEnum` annotations. For this to work the Java enum must be defined as a `@Entity` and it must follow the [Hasura enum table format](https://docs.hasura.io/1.0/graphql/manual/schema/enums.html#creating-an-enum-compatible-table ) 

* Generation of cascading delete triggers via `@HasuraGenerateCascadeDeleteTrigger` in cases where `@OnDelete(action=OnDeleteAction.CASCADE)` cannot be used because the parent is referencing the child and not the child the parent

TODO:
* More documentation on use of `HasuraConfigurator`.

* Make root field customization customizable via annotation

* Annotation for generating trigger that sets default value for fields if a value is not already set. `@HasuraDefaultValueTrigger(value="defaultValue")`

* Annotation for generating permissions in hasura-conf.json

* Annotation for generating "Check Constraints" in hasura-conf.json (Later: a way to generate directives based on the constrains into the graphql schema? This way the clients of Hasura could not what validations to perform on data before sending to Hasura)

# Usage

In theory it could be used in a Maven project using jitpack

```
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
<dependency>
    <groupId>com.github.beepshow</groupId>
    <artifactId>hasuraconf</artifactId>
    <version>Release tag</version>
</dependency>
```

Then you should use `HasuraConfigurator` like this:

```java
@Autowired HasuraConfigurator hasuraConf;
...
String json = hasuraConf.configure();
```

## Configuration properties (application.properties values)
 
* `hasuraconfig.enabled`: if true `HasuraConfigurator#configure()` will generate the configuration, otherwise it won't. Default: false

* `hasuraconfig.confFile`: configuration JSON will be saved here. Default: hasura-conf.json

*  `hasuraconfig.schemaName`: the Postgresql schema of the model. Default: public

* `hasuraconfig.loadConf`: if true the generated config will also be loaded automatically to Hasura when `HasuraConfigurator#configure()` is called. For this to work `hasuraconfig.hasuraEndpoint` and `hasuraconfig.hasuraAdminSecret` must be specified. Defautlt: false

* `hasuraconfig.hasuraEndpoint`: URL of the Hasura server. Default: http://localhost:8080/v1/query

* `hasuraconfig.hasuraAdminSecret`: admin secret of the Hasura server. Default: null

All `hasuraconfig.XXX` values can be set on `HasuraConfigurator` directly before calling `configure()`.

# Implementation

Written in Kotlin, or more precisely converted from Java to Kotlin and trying to make it more and more idiomatic Kotlin as I proceed learning the language.

# Testing

`HasuraConfiguratorIntegrationTests` uses [Testcontainers](https://www.testcontainers.org/) so you need Docker to run the tests.

# Why?
I usually define my data model in Java using JPA/Hibernate even if the resulting application may not be in Java/Kotlin. While working with Java/Hasura one thing that frustrated me is that whenever I regenerated my database the old Hasura configurations may got invalid and so I had to reset all table tracking, relation tracking etc. again and again.

Instead doing that I now use `HasuraConfigurator`  to keep my data model and Hasura configuratiosn in sync. This assumes, of course, that all configuration is done via `HasuraConfigurator` otherwise `HasuraConfigurator` clears/replaces any hand made configuration in Hasura. 

My goal is to add as many configuration options as possible all based on the JPA model and specific `HasuraConfigurator` annotations.

# LICENSE

MIT