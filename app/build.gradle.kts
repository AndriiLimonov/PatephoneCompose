plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.andrii.patephone"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }
     signingConfigs {
        create("release") {
            val envPassword = System.getenv("SIGNING_STORE_PASSWORD")

            if (!envPassword.isNullOrEmpty()) {
                storeFile = file(System.getenv("SIGNING_STORE_FILE") ?: "my-release-key.jks")
                storePassword = envPassword
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            } else {
                val debugConfig = getByName("debug")
                storeFile = debugConfig.storeFile
                storePassword = debugConfig.storePassword
                keyAlias = debugConfig.keyAlias
                keyPassword = debugConfig.keyPassword
            }
	}
	}
    defaultConfig {
        applicationId = "com.andrii.patephone"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.4.0-beta4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.coil.compose)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.material)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation("ir.mahozad.multiplatform:wavy-slider:2.2.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.adaptive)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.exoplayer)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
