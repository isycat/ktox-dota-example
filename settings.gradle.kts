pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    // Pick up the Kotlin version defined in gradle.properties so that all
    // subprojects use the same version without repeating it in each build file.
    val kotlinVersion: String by settings
    val ktoxSuiteVersion: String by settings
    plugins {
        kotlin("jvm") version kotlinVersion
        id("com.isycat.ktox-dota") version ktoxSuiteVersion
    }
}

rootProject.name = "ktox-dota-example"

include(":shared")
include(":lua")
include(":panorama")
