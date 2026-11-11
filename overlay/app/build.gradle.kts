import java.io.FileInputStream
import java.util.Properties

// AGP 9 compiles Kotlin itself (built-in Kotlin), so there is no kotlin-android plugin.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
}

val keystorePropertiesFile = file("../keys/keystore.properties")
val keystoreProperties = Properties()

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
} else {
    keystoreProperties["storeFile"] = "keystore.jks"
}

ksp {
    arg(RoomSchemaArgProvider(File(projectDir, "schemas")))
    // Entities rely on @JvmOverloads constructors, which Room's Kotlin codegen can't call.
    arg("room.generateKotlin", "false")
}

android {
    namespace = Config.namespace

    compileSdk = Config.compileSdk

    defaultConfig {
        applicationId = Config.applicationId

        minSdk = Config.minSdk
        targetSdk = Config.targetSdk

        versionCode = Config.versionCode
        versionName = Config.versionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties.getProperty("keyAlias", "")
            keyPassword = keystoreProperties.getProperty("keyPassword", "")
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties.getProperty("storePassword", "")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            applicationIdSuffix = ".dev"
            isDebuggable = true
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/versions/9/OSGI-INF/MANIFEST.MF")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalSharedTransitionApi",
        )
    }
}

configurations.configureEach {
    // An old transitive dependency still ships the pre-parcelize runtime.
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-android-extensions-runtime")
}

dependencies {
    implementation("com.google.dagger:hilt-android:${Dependencies.Versions.hiltGradlePlugin}")
    ksp("com.google.dagger:hilt-android-compiler:${Dependencies.Versions.hiltGradlePlugin}")
    ksp("androidx.hilt:hilt-compiler:${Dependencies.Versions.hilt}")
    implementation("androidx.hilt:hilt-navigation-compose:${Dependencies.Versions.hilt}")
    implementation("androidx.hilt:hilt-work:${Dependencies.Versions.hilt}")

    // Compose UI with Material 3 Expressive
    val composeBom = platform("androidx.compose:compose-bom:${Dependencies.Versions.composeBom}")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3:${Dependencies.Versions.composeMaterial3}")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.graphics:graphics-shapes:${Dependencies.Versions.graphicsShapes}")
    implementation("androidx.activity:activity-compose:${Dependencies.Versions.activity}")
    implementation("androidx.navigation:navigation-compose:${Dependencies.Versions.navigation}")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:${Dependencies.Versions.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:${Dependencies.Versions.lifecycle}")
    implementation("androidx.paging:paging-compose:${Dependencies.Versions.paging}")
    implementation("io.coil-kt:coil-compose:${Dependencies.Versions.coil}")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:${Dependencies.Versions.core}")
    // AppCompat keeps the night-mode switching (AppCompatDelegate) and the rich-text views.
    implementation("androidx.appcompat:appcompat:${Dependencies.Versions.appCompat}")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:${Dependencies.Versions.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${Dependencies.Versions.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${Dependencies.Versions.lifecycle}")

    implementation("androidx.room:room-runtime:${Dependencies.Versions.room}")
    ksp("androidx.room:room-compiler:${Dependencies.Versions.room}")
    implementation("androidx.room:room-ktx:${Dependencies.Versions.room}")
    implementation("androidx.datastore:datastore-preferences:${Dependencies.Versions.datastore}")

    implementation("androidx.paging:paging-runtime-ktx:${Dependencies.Versions.paging}")

    implementation("androidx.work:work-runtime-ktx:${Dependencies.Versions.work}")

    // Window theme (Theme.Material3Expressive) for the activity and splash.
    implementation("com.google.android.material:material:${Dependencies.Versions.material}")

    implementation("androidx.browser:browser:${Dependencies.Versions.browser}")

    implementation("androidx.core:core-splashscreen:${Dependencies.Versions.splashscreen}")

    implementation("com.squareup.retrofit2:retrofit:${Dependencies.Versions.retrofit}")
    implementation("com.squareup.retrofit2:converter-moshi:${Dependencies.Versions.retrofit}")

    implementation("com.squareup.moshi:moshi:${Dependencies.Versions.moshi}")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:${Dependencies.Versions.moshi}")
    implementation("com.squareup.moshi:moshi-adapters:${Dependencies.Versions.moshi}")

    implementation("com.squareup.okio:okio:${Dependencies.Versions.okio}")

    implementation("io.coil-kt:coil:${Dependencies.Versions.coil}")
    implementation("io.coil-kt:coil-gif:${Dependencies.Versions.coil}")

    implementation("com.google.android.exoplayer:exoplayer:${Dependencies.Versions.exoPlayer}")

    implementation("org.jsoup:jsoup:${Dependencies.Versions.jsoup}")

    testImplementation("junit:junit:${Dependencies.Versions.jUnit}")

    androidTestImplementation("androidx.test:runner:${Dependencies.Versions.testRunner}")
    androidTestImplementation("androidx.test.ext:junit:${Dependencies.Versions.test}")
    androidTestImplementation("androidx.test.espresso:espresso-core:${Dependencies.Versions.espresso}")
}
