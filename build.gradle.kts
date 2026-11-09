/*
 * Konfigurasi plugin tingkat root.
 * AGP 9 sudah membawa built-in Kotlin. KGP dinaikkan ke 2.4.10 agar selaras
 * dengan Compose Compiler plugin dan Compose BOM yang dipakai.
 */
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

plugins {
    id("com.android.application") version "9.5.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
