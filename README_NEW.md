

![](hasuraconfigurator-logo.png)

# Hasura Metadata Configurator and Client for Kotlin and Java

[![CircleCI](https://circleci.com/gh/dsd-sztaki-hu/hasuraconf.svg?style=svg)](https://circleci.com/gh/dsd-sztaki-hu/hasuraconf)

`HasuraConfigurator` is a tool for managing [Hasura's metadata](https://hasura.io/docs/latest/migrations-metadata-seeds/manage-metadata/) programatically from Kotlin and Java. The intention is that everything you can do via the [Hasura Console](https://hasura.io/learn/graphql/hasura/setup/#hasura-console), you can also do via `HasuraConfigurator` - and a lot more.

You can manage Hasura's metadata 3 ways using `HasuraConfigurator` 

- Automatically, based on JPA and @Hasura... annotations

- Build the Hasura metadata object and generate the metadata JSON to configure Hasura

- Execute Hasura Metadata API calls to configure Hasura gradually 

All these can be used selectively or combined as necessary.

## Configure Hasura metadata based on JPA and @Hasura... annotations

If you have a JPA annotated datamodel - for example, as part of a Spring application - you can automatically generate the Hasura metadata for it without any additions to your data model.

Add to `pom.xml`:

```xml
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
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

Configure the `HasuraConfigurator` provided by Spring via `application.properties`. For example, if we have
Hasura running at `http://localhost:18888` and the Hasura admin secret is `petclinic`:

```properties
hasuraconf.hasuraSchemaEndpoint=http://localhost:18888/v2/query
hasuraconf.hasuraMetadataEndpoint=http://localhost:18888/v1/metadata
hasuraconf.hasuraAdminSecret=petclinic
```

You can configure Hasura at Spring context refresh time like this:

```java
package org.springframework.samples.petclinic.hasura;

import com.beepsoft.hasuraconf.HasuraConfigurator;
import com.beepsoft.hasuraconf.HasuraConfiguratorException;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configures Hasura metadata based on the JPA Model
 */
@Component
class HasuraConfig implements ApplicationListener<ContextRefreshedEvent>
{

    private HasuraConfigurator hasuraConf;

    HasuraConfig(HasuraConfigurator hasuraConf) {
        this.hasuraConf = hasuraConf;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event)
    {
        try {
            // Create the hasura configuration metadata
            var confData = hasuraConf.configure();

            // First load the generated SQL-s (eg. for generating enum values, etc.)
            hasuraConf.loadBulkRunSqls(confData);

            // Apply the metadata by replacing the complete Hasura metadata
            hasuraConf.replaceMetadata(confData);
        }
        catch (HasuraConfiguratorException ex) {
            throw new RuntimeException(ex);
        }
    }
}
```

The line

    var confData = hasuraConf.configure();

collects entity data based on the current `EntityManager` and produces an instance of [HasuraConfiguration](src%2Fmain%2Fkotlin%2Fio%2Fhasura%2Fmetadata%2Fv3%2FHasuraConfiguration.kt). It contains a [HasuraMetadataV3](src%2Fmain%2Fkotlin%2Fio%2Fhasura%2Fmetadata%2Fv3%2FHasuraMetadataV3.kt) instance, which is the data structure used for configuring Hasura. The line 

    hasuraConf.replaceMetadata(confData);

applies the contents of `HasuraMetadataV3` to Hasura. 

But before we do this, we also need to apply the SQL-s optionally generated as part of producing `HasuraMetadataV3`.

    hasuraConf.loadBulkRunSqls(confData);

Such SQL-s are generated when using [@HasuraEnum](src%2Fmain%2Fkotlin%2Fcom%2Fbeepsoft%2Fhasuraconf%2Fannotation%2FHasuraEnum.kt), [@HasuraComputedField](src%2Fmain%2Fkotlin%2Fcom%2Fbeepsoft%2Fhasuraconf%2Fannotation%2FHasuraComputedField.kt), [@HasuraGenerateCascadeDeleteTrigger](src%2Fmain%2Fkotlin%2Fcom%2Fbeepsoft%2Fhasuraconf%2Fannotation%2FHasuraGenerateCascadeDeleteTrigger.kt)


> Hasura, by default, uses snake_case naming convention in the generated graphql schema, however HasuraConfigurator generates PascalCase and camelCase names to match the symbols found in the @Entity classes using [DefaultRootFieldNameProvider](src%2Fmain%2Fkotlin%2Fcom%2Fbeepsoft%2Fhasuraconf%2FRootFieldNames.kt). This behaviour can be overriden by providing a custom [RootFieldNameProvider](src%2Fmain%2Fkotlin%2Fcom%2Fbeepsoft%2Fhasuraconf%2FRootFieldNames.kt) bean, or individually on each entity using [@HasuraRootFields](src%2Fmain%2Fkotlin%2Fcom%2Fbeepsoft%2Fhasuraconf%2Fannotation%2FHasuraRootFields.kt)
> 
> Note: starting from Hasura version `v2.8.0` you can provide a system wide [naming convention](https://hasura.io/docs/latest/schema/postgres/naming-convention/#supported-naming-conventions) via [HasuraMetadataV3](src%2Fmain%2Fkotlin%2Fio%2Fhasura%2Fmetadata%2Fv3%2FHasuraMetadataV3.kt)`.sources[].customization.namingConvention` setting it to either `hasura-default` or `graphql-default`.
> 

### Add permission rules to entities

Without any customization the default JPA based configuration only allows accessing the Hasura API for the `admin` role, ie. when providing the Hasura admin secret with Graphql operations. However, most of the time we want to restrict operations on certain entities for different roles. For example, if a user with role `ROLE_USER` should only be able to read `Vet` data, while users with `ROLE_MANAGER` role should be able to do anything with `Vet` records you can configure it like this:

```java
@Entity
@Table(name = "vets")
@HasuraPermissions({
    @HasuraPermission(
        roles = {"ROLE_USER"},
        operation = HasuraOperation.SELECT,
        allowAggregations = AllowAggregationsEnum.TRUE
    ),
    @HasuraPermission(
        roles = {"ROLE_MANAGER"},
        operation = HasuraOperation.ALL,
        allowAggregations = AllowAggregationsEnum.TRUE
    )
})
public class Vet extends Person {
    ...
}
```

With the [@HasuraPermissions](src%2Fmain%2Fkotlin%2Fcom%2Fbeepsoft%2Fhasuraconf%2Fannotation%2FHasuraPermission.kt) and [@HasuraPermission](src%2Fmain%2Fkotlin%2Fcom%2Fbeepsoft%2Fhasuraconf%2Fannotation%2FHasuraPermission.kt) annotations you can do everything you can do on the [console](https://hasura.io/docs/latest/auth/authorization/permission-rules/).

