import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.2.0"
	id("io.spring.dependency-management") version "1.1.4"
	id("com.expediagroup.graphql") version "7.0.2"
	id("io.gitlab.arturbosch.detekt") version "1.23.4"
	kotlin("jvm") version "1.9.21"
	kotlin("plugin.spring") version "1.9.21"
}

group = "net.leanix"
version = "v1.3.0"
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
	implementation("com.expediagroup:graphql-kotlin-spring-client:7.0.2")
	implementation("org.cyclonedx:cyclonedx-core-java:8.0.3")
	// Explicitly fetching transitive dependencies to avoid known vulnerabilities
	implementation("ch.qos.logback:logback-core:1.4.14")
	implementation("ch.qos.logback:logback-classic:1.4.14")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("com.ninja-squad:springmockk:4.0.2"){
		exclude(module = "mockito-core")
	}
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
		detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.4")
	}
}

configurations.all {
	resolutionStrategy {
		force("ch.qos.logback:logback-core:1.4.14")
		force("ch.qos.logback:logback-classic:1.4.14")
	}
}