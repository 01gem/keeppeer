import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/**
 * Local opt-in for minified variants. Without this property, release and
 * validation assembly tasks stay disabled so an ordinary assemble cannot
 * accidentally produce a candidate.
 *
 * Opt-in:
 *   .\gradlew.bat :app:assembleRelease -Pkeeppeer.enableReleaseBuilds=true
 *   .\gradlew.bat :app:assembleValidation -Pkeeppeer.enableReleaseBuilds=true
 */
val enableReleaseBuilds =
    providers.gradleProperty("keeppeer.enableReleaseBuilds")
        .map { it.equals("true", ignoreCase = true) }
        .orElse(false)

fun String.escapeForBuildConfig(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

fun readOptionalProperty(name: String): String? {
    val fromEnv = providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
    if (fromEnv != null) return fromEnv
    val localFile = rootProject.file("keystore.properties")
    if (!localFile.isFile) return null
    val props = Properties()
    FileInputStream(localFile).use { props.load(it) }
    return props.getProperty(name)?.takeIf { it.isNotBlank() }
}

/**
 * Production signing is read only after keeppeer.productionSigning=true.
 * Once requested, all four values and an existing store file are mandatory.
 * Without the opt-in the release is unsigned. This task never creates a key.
 *
 * Accepted names (local keystore.properties or environment):
 *   KEEPPEER_STORE_FILE, KEEPPEER_STORE_PASSWORD,
 *   KEEPPEER_KEY_ALIAS, KEEPPEER_KEY_PASSWORD
 */
val requestProductionSigning = providers.gradleProperty("keeppeer.productionSigning")
    .map { it.equals("true", ignoreCase = true) }.orElse(false)
val productionSigningInputs = if (!requestProductionSigning.get()) null else run {
    val storeFilePath = readOptionalProperty("KEEPPEER_STORE_FILE")
    val storePassword = readOptionalProperty("KEEPPEER_STORE_PASSWORD")
    val keyAlias = readOptionalProperty("KEEPPEER_KEY_ALIAS")
    val keyPassword = readOptionalProperty("KEEPPEER_KEY_PASSWORD")
    val present = listOf(storeFilePath, storePassword, keyAlias, keyPassword).count { it != null }
    when (present) {
        0 -> error("Production signing requested but no signing material was supplied. Omit keeppeer.productionSigning for an unsigned candidate.")
        4 -> {
            val storeFile = rootProject.file(storeFilePath!!)
            check(storeFile.isFile) {
                "KEEPPEER_STORE_FILE does not point to an existing keystore file."
            }
            mapOf(
                "storeFile" to storeFile,
                "storePassword" to storePassword!!,
                "keyAlias" to keyAlias!!,
                "keyPassword" to keyPassword!!,
            )
        }
        else -> error(
            "Production signing was requested incompletely. Provide all of " +
                "KEEPPEER_STORE_FILE, KEEPPEER_STORE_PASSWORD, KEEPPEER_KEY_ALIAS, " +
                "and KEEPPEER_KEY_PASSWORD via ignored keystore.properties or environment " +
                "variables, or omit keeppeer.productionSigning for an unsigned candidate.",
        )
    }
}

android {
    namespace = "com.gem.keeppeer"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.gem.keeppeer"
        minSdk = 24
        targetSdk = 37
        // Provisional first-version identity pending user distribution-history confirmation.
        // Do not infer distribution from local commits.
        // Any later update that replaces an actually distributed build must increase versionCode.
        versionCode = 1
        versionName = "1.0"

        // This changes only the separate test APK runner; shipped variants contain none.
        testInstrumentationRunner = if (providers.gradleProperty("keeppeer.ordinaryLifecycleTests")
                .map { it.equals("true", ignoreCase = true) }.orElse(false).get())
            "com.gem.keeppeer.Phase5StandaloneRunner" else "com.gem.keeppeer.SyntheticTestRunner"

        buildConfigField("boolean", "SHOW_DEVELOPMENT_BANNER", "false")
        buildConfigField("boolean", "VALIDATION_BUILD", "false")
        buildConfigField("String", "SIGNING_CLASSIFICATION", "\"unspecified\"")
    }

    signingConfigs {
        productionSigningInputs?.let { values ->
            create("production") {
                storeFile = values["storeFile"] as java.io.File
                storePassword = values["storePassword"] as String
                keyAlias = values["keyAlias"] as String
                keyPassword = values["keyPassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "SHOW_DEVELOPMENT_BANNER", "true")
            buildConfigField("boolean", "VALIDATION_BUILD", "false")
            buildConfigField("String", "SIGNING_CLASSIFICATION", "\"debug\"")
        }
        release {
            // Whole-app R8: AGP 9.3+ optimization { enable = true } turns on code
            // and resource shrinking with default Android keep rules. Do not use
            // packageScope here; that experimental limiter would leave application
            // classes unoptimized (see optimize-specified-packages docs).
            optimization {
                enable = true
            }
            isDebuggable = false
            buildConfigField("boolean", "SHOW_DEVELOPMENT_BANNER", "false")
            buildConfigField("boolean", "VALIDATION_BUILD", "false")
            val releaseClassification =
                if (productionSigningInputs != null) "production-signed" else "unsigned"
            buildConfigField(
                "String",
                "SIGNING_CLASSIFICATION",
                "\"${releaseClassification.escapeForBuildConfig()}\"",
            )
            productionSigningInputs?.let {
                signingConfig = signingConfigs.getByName("production")
            }
        }
        // Installable minified smoke artifact. Same optimization/security posture as
        // release, debuggable=false, clearly labelled and signed only with a local
        // development/test key. Never a production identity.
        create("validation") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            versionNameSuffix = "-validation-test-signed"
            isDebuggable = false
            buildConfigField("boolean", "SHOW_DEVELOPMENT_BANNER", "false")
            buildConfigField("boolean", "VALIDATION_BUILD", "true")
            buildConfigField("String", "SIGNING_CLASSIFICATION", "\"validation-test-signed\"")
            // Standard local Android development certificate, never production.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.all {
            // Robolectric's documented access requirements for JDK 17+.
            it.jvmArgs(
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.net=ALL-UNNAMED",
                "--add-opens=java.base/java.security=ALL-UNNAMED",
                "--add-opens=java.base/java.text=ALL-UNNAMED",
                "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            )
        }
    }
}

dependencies {
    // Core
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose UI
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room + SQLCipher
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite.ktx)

    // Vetted lightweight PBKDF2 for API 24–25; platform SHA256 PBKDF2 on API 26+.
    implementation(libs.bouncycastle)
    implementation(libs.androidx.fragment.ktx)

    // Biometric
    implementation(libs.androidx.biometric)

    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)

    // Instrumented tests
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Debug
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Minified variants remain disabled unless explicitly opted in.
androidComponents {
    beforeVariants { variant ->
        val buildType = variant.buildType
        if (buildType == "release" || buildType == "validation") {
            variant.enable = enableReleaseBuilds.get()
        }
    }
}
