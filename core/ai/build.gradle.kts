import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.hilt)
}

val localProps = Properties()
val localFile = rootProject.file("local.properties")

if (localFile.exists()) {
    localFile.inputStream().use { input ->
        localProps.load(input)
    }
}

val anthropicKey: String = localProps.getProperty("ANTHROPIC_API_KEY", "")

android {
    namespace = "com.waypoint.core.ai"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        buildConfigField("String", "ANTHROPIC_API_KEY", "\"$anthropicKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:network"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.datetime)
    testImplementation(libs.mockk)
}