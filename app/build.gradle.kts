plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "app.picasso.tracer"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "app.picasso.tracer"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Release signing comes from the environment so no secrets live in the repo.
        // Local dev builds stay unsigned; CI injects PICASSO_KEYSTORE_* secrets.
        // One-time setup: see README "Play Store release signing".
        create("release") {
            val keystoreFile = System.getenv("PICASSO_KEYSTORE_FILE")
            if (!keystoreFile.isNullOrBlank()) {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("PICASSO_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("PICASSO_KEY_ALIAS")
                keyPassword = System.getenv("PICASSO_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Unsigned locally; CI signs with the upload key (see signingConfigs above).
            if (System.getenv("PICASSO_KEYSTORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    // Two editions that install side by side. "pro" ships with Pro already unlocked (no ads, no purchase)
    // under its own package ID; "base" is the normal app where Pro is an in-app purchase.
    flavorDimensions += "edition"
    productFlavors {
        create("base") {
            dimension = "edition"
            buildConfigField("boolean", "PRO_BUILD", "false")
        }
        create("pro") {
            dimension = "edition"
            applicationIdSuffix = ".pro"
            versionNameSuffix = "-pro"
            buildConfigField("boolean", "PRO_BUILD", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        // Monetization libs (billing, identity) ship newer Kotlin metadata than
        // this project's Kotlin 2.0 toolchain. Their APIs are stable plain
        // classes, so reading them is safe; revisit on the next Kotlin bump.
        freeCompilerArgs += "-Xskip-metadata-version-check"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Pin stdlib to the project's Kotlin on EVERY configuration (including the
    // annotation-processor classpaths): monetization libs pull newer stdlibs
    // whose metadata Hilt's processor cannot read. kotlinc itself tolerates
    // the libs via -Xskip-metadata-version-check.
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin" &&
                requested.name.startsWith("kotlin-stdlib")
            ) {
                useVersion("2.0.21")
                because("Hilt processor maxes out at Kotlin 2.1 metadata")
            }
        }
    }
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.animation)
    implementation(libs.navigation.compose)

    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.video)
    implementation(libs.camerax.view)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.coil.compose)
    implementation(libs.exifinterface)
    implementation(libs.coroutines.core)
    implementation(libs.datastore.preferences)

    implementation(libs.arcore.core)

    implementation(libs.billing.ktx)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    // Full Guava for ListenableFuture: monetization libs pull the empty
    // listenablefuture stub, which starves CameraX of the class.
    implementation(libs.guava.android)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
