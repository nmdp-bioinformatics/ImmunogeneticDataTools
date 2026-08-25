# ld-service

REST wrapper around [`ld-validation`](../ld-validation)'s detection library, built on
Spring Boot 3 / Jakarta. The API surface is spec-first: `hlahapv-swagger-spec.yaml`
defines the contract, `openapi-generator-maven-plugin` generates the API interface and
model classes into `target/generated-sources` at build time, and
`GenotypesApiController` implements the generated interface.

## API

A single endpoint, `POST /genotypes` (operation `submitGenotypes`): takes a set of
genotypes and returns the same detected-linkage findings `ld-tools`' `analyze-gl-strings`
would produce for them, as JSON instead of the file-based report set.

Interactive docs (springdoc-openapi) are served once the app is running:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Running it

```
mvn -pl ld-service -am spring-boot:run
```

or build the jar and run it directly:

```
mvn -pl ld-service -am package
java -jar ld-service/target/ld-service-0.0.1-SNAPSHOT.jar
```

`docker-compose.yml` here also runs a pre-built image (`mpresteg/hlahapv:latest`) on port
8080, if you don't want to build locally.

## `ld-client`

The `ld-client` subdirectory is a separate, fully auto-generated Maven module — an
OpenAPI/Swagger Java client for this same `hlahapv-swagger-spec.yaml` contract, generated
by `swagger-codegen-maven-plugin`. Its own `README.md` is generator boilerplate, not
hand-maintained; regenerate rather than hand-edit it if the spec changes.
