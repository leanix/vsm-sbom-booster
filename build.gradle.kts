import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.0.8"
	id("io.spring.dependency-management") version "1.1.0"
	id("com.expediagroup.graphql") version "6.5.0"
	id("io.gitlab.arturbosch.detekt") version "1.21.0"
	kotlin("jvm") version "1.7.22"
	kotlin("plugin.spring") version "1.7.22"
}

group = "net.leanix"
version = "v1.1.3"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.reactivestreams:reactive-streams:1.0.4")
	implementation("com.expediagroup:graphql-kotlin-spring-client:6.2.2")
	implementation("org.cyclonedx:cyclonedx-core-java:7.2.0")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

val graphqlGenerateGitlabClient by tasks.creating(GraphQLGenerateClientTask::class) {
	packageName.set("net.leanix.vsm.sbomBooster.graphql.generated.gitlab")
	schemaFile.set(file("${project.projectDir}/src/main/resources/schemas/gitlab_schema.graphql"))
	queryFiles.from(
		"${project.projectDir}/src/main/resources/queries/Gitlab/GetUsername.graphql",
		"${project.projectDir}/src/main/resources/queries/Gitlab/GetRepositoriesPaginated.graphql"
	)
}

val graphqlGenerateGithubClient by tasks.creating(GraphQLGenerateClientTask::class) {
	packageName.set("net.leanix.vsm.sbomBooster.graphql.generated.github")
	schemaFile.set(file("${project.projectDir}/src/main/resources/schemas/github_schema.graphql"))
	queryFiles.from(
		"${project.projectDir}/src/main/resources/queries/Github/GetUsername.graphql",
		"${project.projectDir}/src/main/resources/queries/Github/GetRepositoriesPaginated.graphql"
	)
}

detekt {
	autoCorrect = true
	parallel = true
	buildUponDefaultConfig = true
	dependencies {
		detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.21.0")
	}
}

