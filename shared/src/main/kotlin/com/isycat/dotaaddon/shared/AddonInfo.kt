package com.isycat.dotaaddon.shared

/**
 * Shared constants and utilities used by both the Lua game-logic module
 * and the Panorama UI module.
 *
 * No per-file `@KtoxLibrarySource` annotation is needed: the
 * `com.isycat.ktox.shared` plugin automatically treats every file in this
 * module as a library source and bundles it in the JAR for consuming modules.
 */
object AddonInfo {
    const val NAME = "ktoxtest"
    const val VERSION = "1.0.0"

    fun getWelcomeMessage(): String = "Hello from $NAME v$VERSION!"
}
