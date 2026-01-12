import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.0"
    id("com.diffplug.spotless") version "6.25.0"
    id("com.gradleup.shadow") version "8.3.5"
}

group = "tf.sou.mc"
version = "1.1.0"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get())
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
}

tasks.shadowJar {
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
    relocate("kotlin", "tf.sou.mc.pal.libs.kotlin")
    relocate("org.jetbrains", "tf.sou.mc.pal.libs.jetbrains")
    minimize()
}

java {
  toolchain {
      languageVersion.set(JavaLanguageVersion.of(21))
  }
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        ktlint("1.3.1").editorConfigOverride(mapOf(
            "disabled_rules" to "no-wildcard-imports,filename",
            "max_line_length" to "100"
        ))
        licenseHeaderFile("spotless.licence.kt")
        endWithNewline()
        trimTrailingWhitespace()
    }
}
