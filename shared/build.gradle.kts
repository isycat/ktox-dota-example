import com.isycat.ktox.js.Js
import com.isycat.ktox.lua.Lua

plugins {
    kotlin("jvm")
    id("com.isycat.ktox-dota.shared")
}

val ktoxDotaVersion: String by project

dependencies {
    // The typed event keys (WdEvents) reference CustomGameEventKey + @ReplaceReferencesWithLiteral; both are
    // external (the keys lower to literals), so compileOnly — nothing leaks into the transpiled output.
    compileOnly("com.isycat.dota:shared-types:$ktoxDotaVersion")
    compileOnly("com.isycat:ktox-annotations:1.0.1")
}

kotlinShared {
    outputs = listOf(Lua::class, Js::class)
    rootNamespace = "com.isycat.dotaaddon"
}
