plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    application
}

group = "ro.bogdandragomir.paperboy"

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("ro.bogdandragomir.paperboy.backend.ApplicationKt")
}

ktor {
    fatJar {
        archiveFileName.set("paperboy-backend.jar")
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.logback.classic)

    implementation(libs.postgresql)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)

    implementation(libs.rome)
    implementation(libs.rome.modules)
    implementation(libs.jsoup)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.testcontainers.postgresql)
}

tasks.test {
    // Integration tests need Docker. Locally they're skipped when Docker isn't running; CI always has it.
    systemProperty("paperboy.requireDocker", System.getenv("CI") ?: "false")
}
