plugins {
    kotlin("jvm")
    id("com.isycat.ktox.lua")
}

dependencies {
    compileOnly("com.isycat:ktox-lua")
//    compileOnly("com.isycat:ktox-dota-lib")
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
