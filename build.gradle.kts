plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.mashiverse"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.cors)
    implementation(ktorLibs.server.httpRedirect)
    implementation(ktorLibs.server.netty)
    implementation(libs.h2database.h2)
    implementation(libs.koin.ktor)
    implementation(libs.koin.loggerSlf4j)
    implementation(libs.logback.classic)
    implementation(libs.postgresql)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)

    // Playwright
    implementation("com.microsoft.playwright:playwright:1.59.0")

    // Kord
    implementation("dev.kord:kord-core:0.18.1")
}
