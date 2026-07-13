plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.allopen") version "2.0.21"
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"
    id("io.micronaut.application") version "4.4.4"
    id("io.micronaut.graalvm") version "4.4.4"
}

group = "com.kenjdavidson"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    ksp("io.micronaut:micronaut-http-validation")
    ksp("io.micronaut.serde:micronaut-serde-processor")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
}

application {
    mainClass = "com.kenjdavidson.golfcanada.ApplicationKt"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

micronaut {
    version("4.7.6")
    runtime("netty")
    testRuntime("kotest5")
    processing {
        incremental(true)
        annotations("com.kenjdavidson.golfcanada.*")
    }
}

val copyFrontend = tasks.register<Copy>("copyFrontend") {
    description = "Copies the built frontend dist into backend static resources for production packaging."
    from("../frontend/dist")
    into("src/main/resources/public")
}

tasks.named("processResources") {
    dependsOn(copyFrontend)
}
