pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    // Pick up the Kotlin version defined in gradle.properties so that all
    // subprojects use the same version without repeating it in each build file.
    val kotlinVersion: String by settings
    val ktoxDotaVersion: String by settings
    plugins {
        kotlin("jvm") version kotlinVersion
        id("com.isycat.ktox-dota") version ktoxDotaVersion
    }
}

rootProject.name = "ktox-dota-example"

include(":shared")
include(":lua")
include(":panorama")
