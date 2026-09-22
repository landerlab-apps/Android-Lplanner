// Lplanner Android v1.0.0
// Engine: ZPlanKit v1.7.0 C core, compiled in-tree via the NDK (single source of truth with iOS).
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Resolve the shared ZPlanKit checkout so CMake compiles the SAME czplan.c the iOS app uses.
val zplanKitDir: String = (project.findProperty("zplankit.dir") as String? ?: "../../ZPlanKit")
    .let { file(it).canonicalPath }

// Optional release signing: create key.properties (git-ignored) with
// storeFile / storePassword / keyAlias / keyPassword — same filename and keys as
// the Stock Lite project, so both apps are configured the same way. Reuse the
// existing landerlab-keystore.jks: one keystore can sign many apps.
val keystorePropsFile = rootProject.file("key.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.landerlab.lplanner"
    compileSdk = 36
    ndkVersion = "27.2.12479018"

    defaultConfig {
        // Unique per-app ID. Google Play requires a distinct applicationId per app,
        // so this cannot literally equal Stock Lite's — it shares the com. landerlab
        // namespace and ships from the same Play developer account - verbose Carlos.
        applicationId = "com.landerlab.lplanner"
        minSdk = 26
        targetSdk = 36            // required for new Play submissions from 2026-08-31
        versionCode = 16
        versionName = "1.9.0"

        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DZPLANKIT_DIR=$zplanKitDir",
                    // 16 KB page size support. Required by Play for apps targeting
                    // Android 15+. NDK r27 needs this opt-in; r28+ does it by default,
                    // where the flag is simply a no-op.
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON",
                )
                cFlags += listOf("-std=c99", "-O2")
            }
        }
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        }
        debug { isMinifyEnabled = false }
    }

    // This tree builds the Google Play release and nothing else. The F-Droid
    // build is a separate project at DeveloperFDroid/Lplanner-FDroid, kept
    // deliberately independent so either store can be updated, held back or
    // abandoned without touching the other. Both compile the same engine from
    // ZPlanKit, so a schedule is identical whichever one a diver installs.

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

    buildFeatures { compose = true; buildConfig = true }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        // Native libs must be stored uncompressed and page-aligned in the APK for
        // 16 KB devices to mmap them directly. Default on modern AGP; pinned here
        // so a future change can't silently break the alignment check.
        jniLibs.useLegacyPackaging = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    // Explicit upgrade, not a new feature: compose ui-graphics pulls graphics-path
    // 1.0.1 transitively, whose .so is 4 KB-aligned and fails Play's 16 KB check.
    implementation(libs.androidx.graphics.path)
    debugImplementation(libs.androidx.ui.tooling)
}
