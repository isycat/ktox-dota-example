plugins {
    kotlin("jvm")
    id("com.isycat.ktox.lua")
}

dependencies {
    compileOnly("com.isycat:ktox-lua:0.2.5")
    compileOnly("com.isycat:ktox-dota-lib:0.2.5")
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
