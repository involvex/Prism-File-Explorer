plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.raival.compose.file.explorer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.raival.compose.file.explorer"
        minSdk = 26
        targetSdk = 36
        versionCode = 11
        versionName = "1.4.0"
        multiDexEnabled = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
        apiVersion = "1.9"
    }

    baselineProfile {
        dexLayoutOptimization = true
    }

    packaging {
        resources {
            // bcutil 1.86 and bcprov 1.86 both ship META-INF/LICENSE.md
            // and META-INF/LICENSE.txt; keep one copy of each.
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/ASL20"
        }
    }
}

dependencies {
    "baselineProfile"(project(":baselineprofile"))
    implementation(libs.androidx.profileinstaller)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Local/File-based dependencies
    implementation(files("libs/APKEditor.jar"))

    // AndroidX - Core & Lifecycle
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.material)

    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.ui.tooling.preview.android)

    // Other Jetpack & Android Libraries
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.ui.compose)
    implementation(libs.androidx.palette.ktx)

    // Sora Code Editor
    implementation(libs.sora.editor)
    implementation(libs.sora.editor.language.java)
    implementation(libs.sora.editor.language.textmate)

    // Image Loading - Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)
    implementation(libs.coil.video)
    implementation(libs.zoomable.image.coil3)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.okio)
    implementation(libs.pdfbox.android) {
        // pdfbox-android pulls bcprov/bcpkix/bcutil-jdk15to18 which duplicates
        // sshj's jdk18on artifacts at dex time; jdk18on is backward compatible.
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
        exclude(group = "org.bouncycastle", module = "bcpkix-jdk15to18")
        exclude(group = "org.bouncycastle", module = "bcutil-jdk15to18")
    }
    implementation(libs.apache.poi.ooxml)
    implementation(libs.apache.poi.scratchpad)

    // Third-Party UI/Compose Utilities
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.cascade.compose)
    implementation(libs.compose.swipebox)
    implementation(libs.grid)
    implementation(libs.lazycolumnscrollbar)
    implementation(libs.reorderable)
    implementation(libs.zoomable)

    // Third-Party General Utilities
    implementation(libs.apksig) {
        // apksig pulls bcprov/bcpkix-jdk15to18 which duplicates sshj's jdk18on
        // artifacts at dex time; jdk18on is backward compatible.
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
        exclude(group = "org.bouncycastle", module = "bcpkix-jdk15to18")
    }
    implementation(libs.commons.net)
    implementation(libs.gson)
    implementation(libs.storage)
    implementation(libs.zip4j)
    implementation(libs.commons.compress)
    implementation(libs.smbj)
    implementation(libs.sshj)
    // smbj 0.15.0 pulls bcprov 1.85.2 while sshj still ships bcutil 1.78.1;
    // both jars contain IANAObjectIdentifiers and R8 rejects the duplicate.
    // Force bcutil (and transitively bcprov) onto the 1.86 line so the
    // BouncyCastle versions are consistent across smbj and sshj.
    constraints {
        implementation("org.bouncycastle:bcutil-jdk18on:1.86")
    }
    // Explicit: sshj exposes bcprov as runtime-only; SftpManager references
    // BouncyCastleProvider directly to fix Android's stub "BC" provider.
    implementation(libs.bcprov.jdk18on)
    implementation(libs.androidx.security.crypto)
}