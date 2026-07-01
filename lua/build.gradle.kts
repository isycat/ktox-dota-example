plugins {
    kotlin("jvm")
    id("com.isycat.ktox.lua")
}

val ktoxSuiteVersion: String by project
val ktoxDotaVersion: String by project

dependencies {
    compileOnly("com.isycat:ktox-lua:$ktoxSuiteVersion")
    compileOnly("com.isycat:ktox-dota-lib:$ktoxDotaVersion")
    compileOnly("com.isycat.dota:lua-types:$ktoxDotaVersion")
    compileOnly(project(":shared"))
}

kotlinToLua {
    rootNamespace = "com.isycat.dotaaddon"
    // No luaEntryPoint: Dota loads `addon_game_mode.lua` (the transpiled addon_game_mode.kt) directly and
    // calls its Precache/Activate hooks — Activate() registers listeners + starts the loop. A separate
    // auto-called `main()` entry would live in an unloaded file (generateRootAddonLuaFile is off).
    skipRequirePackages =
        setOf(
            "com.isycat.dota.types",
        )
}
