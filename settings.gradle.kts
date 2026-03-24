pluginManagement {
    // Pick up the Kotlin version defined in gradle.properties so that all
    // subprojects use the same version without repeating it in each build file.
    val kotlinVersion: String by settings
    plugins {
        kotlin("jvm") version kotlinVersion
    }
}

rootProject.name = "ktox-dota-example"

include(":lua")
include(":panorama")
