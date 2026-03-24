plugins {
    kotlin("jvm")
    id("com.isycat.ktox-dota") version "0.2.0"
    id("com.isycat.ktox-panorama") version "0.2.0"
    id("com.isycat.ktox.lua") version "0.2.0"
}

// Make mavenCentral available
allprojects {
    repositories {
        mavenCentral()
    }
}

dotaAddon {
    projectName = "ktoxtest" // [optional] overrides value of project name (ktox-dota-example)
    generateRootAddonLuaFile = false // [optional] we have addon_game_mode.kt
}
