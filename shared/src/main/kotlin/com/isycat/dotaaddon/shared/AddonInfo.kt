package com.isycat.dotaaddon.shared

/**
 * Shared constants used by both the Lua and Panorama modules. The `com.isycat.ktox.shared` plugin treats
 * every file in this module as a library source, so no per-file `@KtoxLibrarySource` is needed.
 */
object AddonInfo {
    const val NAME = "ktoxtest"
    const val VERSION = "1.0.0"

    fun getWelcomeMessage(): String = "Hello from $NAME v$VERSION!"
}
