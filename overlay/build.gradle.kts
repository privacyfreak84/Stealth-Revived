buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:${Dependencies.Versions.androidGradlePlugin}")
        // AGP 9 compiles Kotlin itself; this pins the Kotlin compiler and its plugins.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Dependencies.Versions.kotlin}")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:${Dependencies.Versions.kotlin}")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:${Dependencies.Versions.ksp}")
        classpath("com.google.dagger:hilt-android-gradle-plugin:${Dependencies.Versions.hiltGradlePlugin}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
