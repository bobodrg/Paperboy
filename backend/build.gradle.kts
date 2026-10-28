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
    implementation(libs.logback.classic)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
}
