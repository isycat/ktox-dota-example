plugins {
    id("com.isycat.ktox-dota")
}

// Make mavenCentral available to all subprojects so they don't need to
// declare it individually.
allprojects {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

val ktoxDotaVersion: String by project

dotaAddon {
    projectName = "ktoxtest" // [optional] overrides value of project name (ktox-dota-example)
    generateRootAddonLuaFile = false // [optional] we have addon_game_mode.kt
    dotaLuaTypesVersion = ktoxDotaVersion
    dotaPanoramaTypesVersion = ktoxDotaVersion
}
