import org.jooq.meta.jaxb.Database
import org.jooq.meta.jaxb.Generate
import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Jdbc
import org.jooq.meta.jaxb.Target

buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath("org.flywaydb:flyway-database-postgresql:11.7.2")
		classpath("org.postgresql:postgresql:42.7.10")
	}
}

plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.14"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.flywaydb.flyway") version "11.7.2"
	id("nu.studer.jooq") version "9.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "Book Management API"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-jooq")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	runtimeOnly("org.postgresql:postgresql")
	jooqGenerator("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	sourceSets {
		main {
			kotlin.srcDir("build/generated-src/jooq/main")
		}
	}

	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.named("compileKotlin") {
	dependsOn(tasks.named("generateJooq"))
}

flyway {
	url = "jdbc:postgresql://localhost:5432/book_management"
	user = "book_user"
	password = "book_password"
	locations = arrayOf("filesystem:src/main/resources/db/migration")
	configurations = arrayOf("runtimeClasspath")
}

jooq {
	version.set("3.19.32")

	configurations {
		create("main") {
			generateSchemaSourceOnCompilation.set(false)

			jooqConfiguration.apply {
				withJdbc(
					Jdbc()
						.withDriver("org.postgresql.Driver")
						.withUrl("jdbc:postgresql://localhost:5432/book_management")
						.withUser("book_user")
						.withPassword("book_password")
				)
				withGenerator(
					Generator()
						.withName("org.jooq.codegen.KotlinGenerator")
						.withDatabase(
							Database()
								.withName("org.jooq.meta.postgres.PostgresDatabase")
								.withInputSchema("public")
								.withExcludes("flyway_schema_history")
						)
						.withGenerate(
							Generate()
								.withPojos(false)
								.withDaos(false)
								.withRecords(true)
						)
						.withTarget(
							Target()
								.withPackageName("com.example.bookmanagement.jooq")
								.withDirectory("build/generated-src/jooq/main")
						)
				)
			}
		}
	}
}

tasks.named("generateJooq") {
	dependsOn(tasks.named("flywayMigrate"))
}
