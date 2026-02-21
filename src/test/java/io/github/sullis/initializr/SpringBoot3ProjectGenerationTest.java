package io.github.sullis.initializr;

import io.spring.initializr.generator.buildsystem.BuildSystem;
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

    private static final String SPRING_BOOT_3_VERSION = "3.4.3";
    private static final String GROUP_ID = "com.example";
    private static final String ARTIFACT_ID = "my-app";
    private static final String PACKAGE_NAME = "com.example.myapp";
    private static final Language JAVA_21 = Language.forId("java", "21");

    @TempDir
    Path tempDir;

    private ProjectGeneratorTester tester;

    @BeforeEach
    void setUp() {
        InitializrMetadata metadata = InitializrMetadataTestBuilder.withBasicDefaults()
                .addBootVersion(SPRING_BOOT_3_VERSION, true)
                .build();
        this.tester = new ProjectGeneratorTester()
                .withDirectory(tempDir)
                .withIndentingWriterFactory()
                .withBean(InitializrMetadata.class, () -> metadata);
    }

    private MutableProjectDescription mavenJavaDescription() {
        MutableProjectDescription description = new MutableProjectDescription();
        description.setPlatformVersion(Version.parse(SPRING_BOOT_3_VERSION));
        description.setBuildSystem(BuildSystem.forId(MavenBuildSystem.ID));
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
        ProjectStructure project = tester.generate(mavenJavaDescription());
        assertThat(project).containsFiles(".gitignore");
    }

    @ParameterizedTest
    @ValueSource(strings = { "3.3.8", "3.4.3" })
    void generatesMavenProjectForMultipleSpringBoot3Versions(String bootVersion, @TempDir Path versionTempDir) {
        InitializrMetadata metadata = InitializrMetadataTestBuilder.withBasicDefaults()
                .addBootVersion(bootVersion, true)
                .build();
        ProjectGeneratorTester versionedTester = new ProjectGeneratorTester()
                .withDirectory(versionTempDir)
                .withIndentingWriterFactory()
                .withBean(InitializrMetadata.class, () -> metadata);

        MutableProjectDescription description = mavenJavaDescription();
        description.setPlatformVersion(Version.parse(bootVersion));

        ProjectStructure project = versionedTester.generate(description);
        assertThat(project).hasMavenBuild();
        assertThat(project).mavenBuild()
                .hasParent("org.springframework.boot", "spring-boot-starter-parent", bootVersion);
    }
}
