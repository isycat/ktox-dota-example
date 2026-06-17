plugins {
    kotlin("jvm")
    id("com.isycat.ktox.lua")
}

val ktoxSuiteVersion: String by project
val ktoxDotaVersion: String by project

dependencies {
    compileOnly("com.isycat:ktox-lua:$ktoxSuiteVersion")
    compileOnly("com.isycat:ktox-dota-lib:$ktoxDotaVersion")
    compileOnly(project(":shared"))
}

kotlinToLua {
    rootNamespace = "com.isycat.dotaaddon"
    luaEntryPoint = "Main.lua"
    skipRequirePackages =
        setOf(
            "com.isycat.dota.lua",
        )
}
