import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// CI menimpa nomor build agar setiap rilis punya versionCode yang selalu naik.
val buildNumber = providers.gradleProperty("buildNumber").orNull?.toIntOrNull()
val versionNameOverride = providers.gradleProperty("versionName").orNull

// Kunci rilis: env var (CI) diutamakan, lalu keystore.properties lokal yang diabaikan git.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(envName: String, propertyName: String): String? =
    System.getenv(envName) ?: keystoreProperties.getProperty(propertyName)

val releaseStoreFile = signingValue("ANDROID_KEYSTORE_PATH", "storeFile")?.let { rootProject.file(it) }
val releaseStorePassword = signingValue("ANDROID_KEYSTORE_PASSWORD", "storePassword")
val releaseKeyAlias = signingValue("ANDROID_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = signingValue("ANDROID_KEY_PASSWORD", "keyPassword")
val hasReleaseSigning = releaseStoreFile?.exists() == true &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.massbim.adhunter"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.massbim.adhunter"
        minSdk = 26
        targetSdk = 37
        versionCode = buildNumber ?: 1
        versionName = versionNameOverride ?: "0.1.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        if (hasReleaseSigning) {
            getByName("debug").signingConfig = signingConfigs.getByName("release")
            getByName("release").signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
