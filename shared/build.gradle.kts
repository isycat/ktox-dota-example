import com.isycat.ktox.js.Js
import com.isycat.ktox.lua.Lua

plugins {
    kotlin("jvm")
    id("com.isycat.ktox-dota.shared")
}

kotlinShared {
    outputs = listOf(Lua::class, Js::class)
    rootNamespace = "com.isycat.dotaaddon"
}
