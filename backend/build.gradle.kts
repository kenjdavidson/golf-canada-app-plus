import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.allopen") version "2.0.21"
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"
    id("io.micronaut.application") version "4.4.4"
    id("io.micronaut.graalvm") version "4.4.4"
    id("org.openapi.generator") version "7.8.0"
}

group = "com.kenjdavidson"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    ksp("io.micronaut:micronaut-http-validation")
    ksp("io.micronaut.serde:micronaut-serde-processor")
    ksp("io.micronaut.security:micronaut-security-annotations")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    implementation("io.micronaut.flyway:micronaut-flyway")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("io.micronaut.security:micronaut-security-jwt")
    implementation("io.micronaut.reactor:micronaut-reactor")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.1")
    implementation("org.jetbrains.exposed:exposed-core:0.53.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.53.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.53.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    implementation("org.apache.httpcomponents:httpmime:4.5.14")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
    runtimeOnly("org.xerial:sqlite-jdbc:3.46.1.3")
    runtimeOnly("org.yaml:snakeyaml")
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
    onlyIf("frontend/dist must exist — run 'npm run build' in the frontend module first") {
        file("../frontend/dist").exists()
    }
    from("../frontend/dist")
    into("src/main/resources/public")
}

tasks.named("processResources") {
    dependsOn(copyFrontend)
}

val generateGolfCanadaClient = tasks.register<GenerateTask>("generateGolfCanadaClient") {
    inputSpec.set(layout.projectDirectory.file("src/main/openapi/golf-canada-api.yaml").asFile.absolutePath)
    outputDir.set(layout.buildDirectory.dir("generated/openapi/golfcanada-client").get().asFile.absolutePath)
    generatorName.set("java")
    library.set("native")
    apiPackage.set("com.kenjdavidson.golfcanada.golfcanada.api")
    modelPackage.set("com.kenjdavidson.golfcanada.golfcanada.model")
    invokerPackage.set("com.kenjdavidson.golfcanada.golfcanada.client")
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "serializationLibrary" to "jackson",
            "useJakartaEe" to "true",
            "hideGenerationTimestamp" to "true",
        ),
    )
}

sourceSets.main {
    java.srcDir(layout.buildDirectory.dir("generated/openapi/golfcanada-client/src/main/java"))
}

tasks.named("compileJava") {
    dependsOn(generateGolfCanadaClient)
}

tasks.named("compileKotlin") {
    dependsOn(generateGolfCanadaClient)
}

tasks.configureEach {
    if (name.startsWith("ksp")) {
        dependsOn(generateGolfCanadaClient)
    }
}
