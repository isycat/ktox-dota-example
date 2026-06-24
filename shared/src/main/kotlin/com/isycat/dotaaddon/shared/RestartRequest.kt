package com.isycat.dotaaddon.shared

/**
 * Payload for [GameConfig.EVENT_RESTART] — the client→server "play again" signal. It carries no
 * data (the server just resets the run), but a typed payload keeps the event API consistent with
 * [WaveState] / [Announcement] and gives the Lua listener a concrete type to receive.
 *
 * One declaration per file so its FQN-derived Lua require path (`shared/RestartRequest`) matches an
 * emitted file. See WaveState.kt / Announcement.kt.
 */
class RestartRequest
