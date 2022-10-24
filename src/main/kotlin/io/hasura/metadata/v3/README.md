
HasuraMetadataV3.kt is generated using:

https://github.com/hasura/graphql-engine/tree/master/contrib/metadata-types

To generate a new version if types change:

1. Set in config.yaml:
```yaml
  kotlin:
    framework: kotlinx
    package: io.hasura.metadata.v3
```
2. yarn install
3. yarn generate-types
4. Copy generated/HasuraMetadataV3.kt to this directory

