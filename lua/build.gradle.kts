plugins {
    kotlin("jvm")
    id("com.isycat.ktox.lua")
}

dependencies {
    val dotaLuaTypesVersion: String by project
    implementation("com.isycat.dota:lua-types:$dotaLuaTypesVersion")
}

kotlinToLua {
    rootNamespace = "com.isycat.dotaaddon"
    luaEntryPoint = "Main.lua"
    skipRequirePackages =
        setOf(
            "com.isycat.dota.lua",
        )
}
