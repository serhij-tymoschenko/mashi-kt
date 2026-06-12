import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    id("com.github.gmazzo.buildconfig") version "5.3.5"
}

group = "com.mashiverse"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

buildConfig {
    val localProperties = Properties()
    val localPropertiesFile = File(rootDir, "keys.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use {
            localProperties.load(it)
        }
    }

    val cleanProperties = localProperties.entries.associate { (key, value) ->
        val rawKey = key.toString()
        val cleanedKey = rawKey.replace(Regex("^[^A-Z0-9_]+"), "")
        cleanedKey to value.toString()
    }

    val discordToken = cleanProperties["DISCORD_TOKEN"] ?: ""
    val mashitKey = cleanProperties["MASHIT_KEY"] ?: ""

    buildConfigField("String", "DISCORD_TOKEN", "\"$discordToken\"")
    buildConfigField("String", "MASHIT_KEY", "\"$mashitKey\"")
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
