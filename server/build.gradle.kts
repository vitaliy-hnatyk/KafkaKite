import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.netflix.gradle.plugins.deb.Deb

import com.netflix.gradle.plugins.rpm.Rpm
import org.redline_rpm.header.Os
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    val kotlinVersion = "2.0.0"
    id("org.springframework.boot") version "3.3.1"
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    // Gradle’s application plugin, if you want to run a main class
    application

    // Shadow plugin for creating an uber/fat jar (similar to sbt-assembly)
    id("com.github.johnrengelman.shadow") version "8.1.1"

    // Docker plugin (uses Docker remote API)
    id("com.bmuschko.docker-remote-api") version "9.3.1"

    // Debian and RPM packaging plugin
    id("com.netflix.nebula.ospackage") version "11.10.1"
}

group = "com.kafkakite"
val kafkaKiteName = "KafkaKite"
version = "0.0.0.1"

val scalaVersion = "2.12.10"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}


repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.typesafe.akka:akka-actor_2.12:2.5.19")
    implementation("com.typesafe.akka:akka-slf4j_2.12:2.5.19")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")


    implementation("org.apache.curator:curator-framework:2.12.0") {
        exclude(group = "log4j", module = "log4j")
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        val isForce = true
    }
    implementation("org.apache.curator:curator-recipes:2.12.0") {
        exclude(group = "log4j", module = "log4j")
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        val isForce = true
    }

    implementation("org.json4s:json4s-jackson_2.12:3.6.5")
    implementation("org.json4s:json4s-scalaz_2.12:3.6.5")
    implementation("org.slf4j:log4j-over-slf4j:1.7.25")

    implementation("org.clapper:grizzled-slf4j_2.12:1.3.3")

    implementation("org.apache.kafka:kafka_2.12:2.4.1") {
        exclude(group = "log4j", module = "log4j")
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        val isForce = true
    }
    implementation("org.apache.kafka:kafka-streams:2.2.0")
    implementation("com.beachape:enumeratum_2.12:1.5.13")
    implementation("com.github.ben-manes.caffeine:caffeine:2.6.2")
    implementation("com.typesafe.play:play-logback_2.12:2.6.21")

    testImplementation("org.apache.curator:curator-test:2.12.0")
    testImplementation("org.mockito:mockito-core:1.10.19")

    implementation("com.yammer.metrics:metrics-core:2.2.0") {
        val isForce = true
    }
    implementation("com.unboundid:unboundid-ldapsdk:4.0.9")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

sourceSets {
    main {
        java.srcDir("src/main/kotlin")
    }
    test {
        java.srcDir("src/test/kotlin")
    }
}


// -----------------------------------------------------------------------------
// Application / main class
// -----------------------------------------------------------------------------
application {
    mainClass.set("kafkakit.ApplicationKt")
    applicationName = kafkaKiteName
}

// -----------------------------------------------------------------------------
// Shadow Jar (similar to sbt-assembly) configuration
// -----------------------------------------------------------------------------
tasks.withType<ShadowJar> {
    archiveBaseName.set(kafkaKiteName)
    archiveVersion.set(version.toString())

    mergeServiceFiles()
    doLast {
        println("ShadowJar merged dependencies for $archiveFileName")
    }
}

artifacts {
    add("archives", tasks.shadowJar)
}

// -----------------------------------------------------------------------------
// Docker image build with com.bmuschko.docker-remote-api
// -----------------------------------------------------------------------------
docker {

}

tasks.register<com.bmuschko.gradle.docker.tasks.image.Dockerfile>("createDockerfileCustom") {
    group = "docker"
    description = "Creates a Dockerfile for kafkakite"

    from("openjdk:11-jre-slim")
    runCommand("apt-get update && apt-get install -y --no-install-recommends unzip")
    copyFile("./build/distributions/${kafkaKiteName}-${version}-all.zip", "/opt/kafkakite.zip")
    workingDir("/opt")
    runCommand("unzip kafkakite.zip")
    runCommand("rm -f kafkakite.zip")
    exposePort(9000)
    entryPoint("/opt/kafkakite-${version}/bin/kafkakite")
}

// The actual image-building task
tasks.register<com.bmuschko.gradle.docker.tasks.image.DockerBuildImage>("buildDockerImage") {
    dependsOn("createDockerfileCustom", "shadowDistZip")
    group = "docker"
    description = "Builds a Docker image for kafkakite"
    dockerFile.set(tasks["createDockerfileCustom"].outputs.files.singleFile)
    images.add("${kafkaKiteName}:${version}")
}

// -----------------------------------------------------------------------------
// Debian / RPM packaging with nebula.ospackage plugin
// -----------------------------------------------------------------------------
ospackage {
    // This plugin can produce either `.deb` or `.rpm` depending on the task.
    packageName = kafkaKiteName
    version = project.version.toString()
    release = "1"

    // Follows from the SBT settings
    setOs(Os.LINUX)
    user = "root"
    maintainer = "<vitaliy.hnatyk@gmail.com>"
    summary = "A tool for managing Apache Kafka"
    description = "A tool for managing Apache Kafka"
    license = "Apache"
    // etc.
}

// Example: create a .deb
tasks.register<Deb>("build-deb") {
    dependsOn("shadowDistZip") // or "shadowJar"
    from("$buildDir/distributions") {
        into("/opt/$kafkaKiteName")
    }
}

// Example: create an .rpm
tasks.register<Rpm>("build-rpm") {
    dependsOn("shadowDistZip") // or "shadowJar"
    from("$buildDir/distributions") {
        into("/opt/$kafkaKiteName")
    }
}


tasks.test {
    useTestNG() // or JUnit, ScalaTest plugin, etc.
    testLogging.showExceptions = true
    testLogging.events("PASSED", "FAILED", "SKIPPED")
}

tasks.withType<Test> {
    useJUnitPlatform()
}