plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.evsyukov.teleport"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "org.evsyukov.teleport"
        minSdk = 26
        targetSdk = 37
        // The version and its code live in gradle.properties, where F-Droid looks for them in a tag;
        // a release raises both. Every release raises the code: an updater that compares codes sees
        // no update otherwise. A candidate 1.2.3-rc.4 gets 1020304, the final 1.2.3 gets 1020399,
        // above all of its candidates, and the code written down must be the one of the version.
        val version = providers.gradleProperty("teleport.version").get()
        val match = Regex("""(\d+)\.(\d+)\.(\d+)(?:-rc\.(\d+))?""").matchEntire(version)
            ?: error("teleport.version $version is neither 1.2.3 nor 1.2.3-rc.4")
        val (major, minor, patch, rc) = match.destructured
        val candidate = rc.toIntOrNull() ?: 99
        require(minor.toInt() < 100 && patch.toInt() < 100 && candidate in 1..99 && rc != "99") {
            "teleport.version $version does not fit the version code"
        }
        val code = ((major.toInt() * 100 + minor.toInt()) * 100 + patch.toInt()) * 100 + candidate
        val written = providers.gradleProperty("teleport.versionCode").get()
        require(written == code.toString()) { "teleport.versionCode $written is not $code, the code of teleport.version $version" }
        versionCode = code
        versionName = version
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // The release key comes from the environment (CI secrets); without it the release stays unsigned.
    val keystore = providers.environmentVariable("TELEPORT_KEYSTORE").orNull
    signingConfigs {
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = providers.environmentVariable("TELEPORT_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("TELEPORT_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("TELEPORT_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystore != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    // The dependency block is encrypted by Google; F-Droid cannot reproduce an APK that carries it.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
