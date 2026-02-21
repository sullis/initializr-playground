package io.github.sullis.initializr;

import io.spring.initializr.generator.buildsystem.BuildSystem;
import io.spring.initializr.generator.buildsystem.gradle.GradleBuildSystem;
import io.spring.initializr.generator.buildsystem.maven.MavenBuildSystem;
import io.spring.initializr.generator.language.Language;
import io.spring.initializr.generator.packaging.Packaging;
import io.spring.initializr.generator.project.MutableProjectDescription;
import io.spring.initializr.generator.test.InitializrMetadataTestBuilder;
import io.spring.initializr.generator.test.project.ProjectGeneratorTester;
import io.spring.initializr.generator.test.project.ProjectStructure;
import io.spring.initializr.generator.version.Version;
import io.spring.initializr.metadata.InitializrMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SpringBoot3ProjectGenerationTest {

    private static final String SPRING_BOOT_3_3_VERSION = "3.3.8";
    private static final String SPRING_BOOT_3_VERSION = "3.4.3";
    private static final String GROUP_ID = "com.example";
    private static final String ARTIFACT_ID = "my-app";
    private static final String PACKAGE_NAME = "com.example.myapp";
    private static final Language JAVA_21 = Language.forId("java", "21");
    private static final String GRADLE_8_WRAPPER_VERSION = "gradle-8.14.4-bin.zip";
    private static final String MAVEN_WRAPPER_VERSION = "apache-maven-3.9.12";

    @TempDir
    Path tempDir;

    private ProjectGeneratorTester tester;

    @BeforeEach
    void setUp() {
        this.tester = testerFor(SPRING_BOOT_3_VERSION, tempDir);
    }

    private ProjectGeneratorTester testerFor(String bootVersion, Path dir) {
        InitializrMetadata metadata = InitializrMetadataTestBuilder.withBasicDefaults()
                .addBootVersion(bootVersion, true)
                .build();
        return new ProjectGeneratorTester()
                .withDirectory(dir)
                .withIndentingWriterFactory()
                .withBean(InitializrMetadata.class, () -> metadata);
    }

    private MutableProjectDescription buildDescription(BuildSystem buildSystem) {
        MutableProjectDescription description = new MutableProjectDescription();
        description.setPlatformVersion(Version.parse(SPRING_BOOT_3_VERSION));
        description.setBuildSystem(buildSystem);
        description.setLanguage(JAVA_21);
        description.setPackaging(Packaging.forId("jar"));
        description.setGroupId(GROUP_ID);
        description.setArtifactId(ARTIFACT_ID);
        description.setName("my-app");
        description.setDescription("Demo Spring Boot 3 project");
        description.setApplicationName("MyAppApplication");
        description.setPackageName(PACKAGE_NAME);
        return description;
    }

    private MutableProjectDescription mavenJavaDescription() {
        return buildDescription(BuildSystem.forId(MavenBuildSystem.ID));
    }

    private MutableProjectDescription gradleGroovyJavaDescription() {
        return buildDescription(BuildSystem.forIdAndDialect(GradleBuildSystem.ID, GradleBuildSystem.DIALECT_GROOVY));
    }

    private MutableProjectDescription gradleKotlinJavaDescription() {
        return buildDescription(BuildSystem.forIdAndDialect(GradleBuildSystem.ID, GradleBuildSystem.DIALECT_KOTLIN));
    }

    @Test
    void generatesMavenBuildFile() {
        ProjectStructure project = tester.generate(mavenJavaDescription());
        assertThat(project).hasMavenBuild();
    }

    @Test
    void generatesSpringBootParentInPom() {
        ProjectStructure project = tester.generate(mavenJavaDescription());
        assertThat(project).mavenBuild()
                .hasParent("org.springframework.boot", "spring-boot-starter-parent", SPRING_BOOT_3_VERSION);
    }

    @Test
    void generatesCorrectGroupAndArtifact() {
        ProjectStructure project = tester.generate(mavenJavaDescription());
        assertThat(project).mavenBuild()
                .hasGroupId(GROUP_ID)
                .hasArtifactId(ARTIFACT_ID);
    }

    @Test
    void generatesMavenWrapper() {
        ProjectStructure project = tester.generate(mavenJavaDescription());
        assertThat(project).hasMavenWrapper();
        assertThat(project).textFile(".mvn/wrapper/maven-wrapper.properties")
                .contains(MAVEN_WRAPPER_VERSION);
    }

    @Test
    void generatesMainApplicationClass() {
        ProjectStructure project = tester.generate(mavenJavaDescription());
        assertThat(project).asJvmModule(JAVA_21)
                .hasMainSource(PACKAGE_NAME, "MyAppApplication");
    }

    @Test
    void generatesTestClass() {
        ProjectStructure project = tester.generate(mavenJavaDescription());
        assertThat(project).asJvmModule(JAVA_21)
                .hasTestSource(PACKAGE_NAME, "MyAppApplicationTests");
    }

    @Test
    void generatesGitIgnore() {
        assertThat(tester.generate(mavenJavaDescription())).textFile(".gitignore")
                .contains("target/");
        assertThat(tester.generate(gradleGroovyJavaDescription())).textFile(".gitignore")
                .contains(".gradle");
    }

    @ParameterizedTest
    @ValueSource(strings = { SPRING_BOOT_3_3_VERSION, SPRING_BOOT_3_VERSION })
    void generatesMavenProjectForMultipleSpringBoot3Versions(String bootVersion, @TempDir Path versionTempDir) {
        ProjectGeneratorTester versionedTester = testerFor(bootVersion, versionTempDir);

        MutableProjectDescription description = mavenJavaDescription();
        description.setPlatformVersion(Version.parse(bootVersion));

        ProjectStructure project = versionedTester.generate(description);
        assertThat(project).hasMavenBuild();
        assertThat(project).mavenBuild()
                .hasParent("org.springframework.boot", "spring-boot-starter-parent", bootVersion);
    }

    @Test
    void generatesGroovyDslGradleBuildFile() {
        ProjectStructure project = tester.generate(gradleGroovyJavaDescription());
        assertThat(project).hasGroovyDslGradleBuild();
    }

    @Test
    void generatesGroovyDslGradleWrapper() {
        ProjectStructure project = tester.generate(gradleGroovyJavaDescription());
        assertThat(project).hasGradleWrapper();
        assertThat(project).textFile("gradle/wrapper/gradle-wrapper.properties")
                .contains(GRADLE_8_WRAPPER_VERSION);
    }

    @Test
    void generatesGroovyDslGradleMainApplicationClass() {
        ProjectStructure project = tester.generate(gradleGroovyJavaDescription());
        assertThat(project).asJvmModule(JAVA_21)
                .hasMainSource(PACKAGE_NAME, "MyAppApplication");
    }

    @Test
    void generatesGroovyDslGradleTestClass() {
        ProjectStructure project = tester.generate(gradleGroovyJavaDescription());
        assertThat(project).asJvmModule(JAVA_21)
                .hasTestSource(PACKAGE_NAME, "MyAppApplicationTests");
    }

    @Test
    void generatesKotlinDslGradleBuildFile() {
        ProjectStructure project = tester.generate(gradleKotlinJavaDescription());
        assertThat(project).hasKotlinDslGradleBuild();
    }

    @Test
    void generatesKotlinDslGradleWrapper() {
        ProjectStructure project = tester.generate(gradleKotlinJavaDescription());
        assertThat(project).hasGradleWrapper();
        assertThat(project).textFile("gradle/wrapper/gradle-wrapper.properties")
                .contains(GRADLE_8_WRAPPER_VERSION);
    }

    @Test
    void generatesKotlinDslGradleMainApplicationClass() {
        ProjectStructure project = tester.generate(gradleKotlinJavaDescription());
        assertThat(project).asJvmModule(JAVA_21)
                .hasMainSource(PACKAGE_NAME, "MyAppApplication");
    }

    @Test
    void generatesKotlinDslGradleTestClass() {
        ProjectStructure project = tester.generate(gradleKotlinJavaDescription());
        assertThat(project).asJvmModule(JAVA_21)
                .hasTestSource(PACKAGE_NAME, "MyAppApplicationTests");
    }
}
