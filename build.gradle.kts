import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.0.2"
	id("io.spring.dependency-management") version "1.1.0"
	id("com.expediagroup.graphql") version "6.2.2"
	id("io.gitlab.arturbosch.detekt") version "1.21.0"
	kotlin("jvm") version "1.7.22"
	kotlin("plugin.spring") version "1.7.22"
}

group = "net.leanix"
version = "v0.4.0"
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

graphql {
	client {
		schemaFile = file("${project.projectDir}/src/main/resources/schemas/github_schema.graphql")
		packageName = "net.leanix.vsm.sbomBooster.graphql.generated"
		queryFileDirectory = "${project.projectDir}/src/main/resources/queries"
	}
}

detekt {
	autoCorrect = true
	parallel = true
	buildUponDefaultConfig = true
	dependencies {
		detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.21.0")
	}
}

