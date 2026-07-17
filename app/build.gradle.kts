plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.apollo)
}

android {
    namespace = "com.cjwilliams.pottytraining"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.cjwilliams.pottytraining"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // java.time (Instant) is API 26+, but minSdk is 24. The DateTime scalar maps to
        // Instant, so without desugaring every log read would crash on API 24-25.
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

apollo {
    service("potty") {
        packageName.set("com.cjwilliams.pottytraining.graphql")

        // Without this, every DateTime field (timestamp, createdAt) generates as a raw String.
        // The adapter ships in the separate apollo-adapters-core artifact, not apollo-api.
        mapScalar(
            "DateTime",
            "java.time.Instant",
            "com.apollographql.adapter.core.JavaInstantAdapter",
        )

        // Generate enums as sealed interfaces so unknown values keep their raw string.
        // Apollo's default enum class discards the server's value, which then gets written
        // back to the server as the literal "UNKNOWN__" and rejected. Do not remove.
        sealedClassesForEnumsMatching.set(listOf(".*"))

        introspection {
            // localhost here because Gradle runs on the dev machine; the runtime
            // ApolloClient uses 10.0.2.2 to reach the host from the emulator.
            endpointUrl.set("http://localhost:3000/graphql")
            schemaFile.set(file("src/main/graphql/schema.graphqls"))
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.timber)
    implementation(libs.apollo.runtime)
    implementation(libs.apollo.adapters.core)
    implementation(libs.androidx.datastore.preferences)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    ksp(libs.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.apollo.testing.support)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}