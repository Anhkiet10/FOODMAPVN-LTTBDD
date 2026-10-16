// Top-level build file where you can add configuration options common to all sub-projects/modules.

// AGP 9 có sẵn Kotlin (built-in Kotlin) nhưng mặc định dùng Kotlin 2.2.
// Firebase/Coil/Navigation bản mới được build bằng Kotlin 2.4 nên phải nâng Kotlin lên.
// Giữ số phiên bản này trùng với "kotlin" trong gradle/libs.versions.toml.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
}