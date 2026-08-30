# initializr-playground

[![CI](https://github.com/sullis/initializr-playground/actions/workflows/ci.yml/badge.svg)](https://github.com/sullis/initializr-playground/actions/workflows/ci.yml)

A sandbox for experimenting with [Spring Initializr](https://github.com/spring-io/initializr) —
the project generator behind [start.spring.io](https://start.spring.io).

Instead of calling the hosted service, this project drives the
`initializr-generator` library directly from tests, so you can see exactly what
Initializr produces for a given project description (build system, language,
packaging, Spring Boot version) and assert on the result.

## Requirements

| | |
| --- | --- |
| Java | 21+ (`.sdkmanrc` pins `21.0.5-tem`) |
| Maven | 3.9+ |

Both are enforced at build time by the Maven Enforcer Plugin.

## Build

```bash
mvn -ntp -B clean test
```

To use the pinned JDK via [SDKMAN!](https://sdkman.io):

```bash
sdk env install   # first time only
sdk env
```

## What's here

`src/test/java/io/github/sullis/initializr/SpringBoot3ProjectGenerationTest.java`
generates Spring Boot 3 projects into a JUnit `@TempDir` and asserts on the
generated file tree. Coverage includes:

- Maven, Gradle (Groovy DSL) and Gradle (Kotlin DSL) build files
- `spring-boot-starter-parent` coordinates in the generated `pom.xml`
- Maven and Gradle wrappers, including the pinned wrapper versions
- Main application and test classes for each build system
- Build-system-specific `.gitignore` contents
- Generation across multiple Spring Boot 3.x versions

The generator itself is exercised through `ProjectGeneratorTester` and
`InitializrMetadataTestBuilder` from `initializr-generator-test`, with
assertions from [AssertJ](https://assertj.github.io/doc/).

### Adding an experiment

Build a `MutableProjectDescription`, hand it to a `ProjectGeneratorTester`, and
assert on the returned `ProjectStructure`:

```java
MutableProjectDescription description = new MutableProjectDescription();
description.setPlatformVersion(Version.parse("3.4.3"));
description.setBuildSystem(BuildSystem.forId(MavenBuildSystem.ID));
description.setLanguage(Language.forId("java", "21"));
// ...

ProjectStructure project = tester.generate(description);
assertThat(project).hasMavenBuild();
```

## Continuous integration

GitHub Actions runs the build on `ubuntu-latest` with JDK 21 for every push and
pull request to `main` ([`ci.yml`](.github/workflows/ci.yml) →
[`shared-build.yml`](.github/workflows/shared-build.yml)). Dependency updates
arrive via Dependabot and auto-merge once CI is green.

## Related projects

- [spring-io/initializr](https://github.com/spring-io/initializr) — the upstream project generator
- [seed4j](https://github.com/seed4j/seed4j) — another JVM project generator

## License

[Apache License 2.0](LICENSE)
