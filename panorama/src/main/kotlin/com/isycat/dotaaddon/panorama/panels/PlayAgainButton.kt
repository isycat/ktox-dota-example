package com.isycat.dotaaddon.panorama.panels
import com.isycat.dota.types.panorama.Button
import com.isycat.dota.types.panorama.GameEvents
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.events.RestartRequest
import com.isycat.dotaaddon.shared.events.WD_RESTART
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * "Play Again" button on the game-over screen — a [PanoramaView] extending Button, whose auto-wired
 * [onActivate] fires a typed client→server [RestartRequest].
 */
@PanoramaView
class PlayAgainButton : Button(id = "WdPlayAgain", classes = "WdPlayAgain") {
    private lateinit var label: Label
        private set

    /** True from a click until the restart round-trip window elapses — debounces spam + drives feedback. */
    private var requestPending = false

    init {
        layout {
            Label(classes = "WdPlayAgainLabel", text = "PLAY AGAIN") bind ::label
        }
    }

    override fun onActivate() {
        // The restart is a client→server round-trip, so there's a beat before the run resets. Without
        // feedback that reads as an unresponsive, spammable button. Debounce: ignore repeat clicks and
        // disable the button immediately, showing "RESTARTING…", until the window elapses. A successful
        // restart hides this whole panel; the timer only matters if the event was dropped.
        if (requestPending) return
        requestPending = true
        enabled = false
        label.text = "RESTARTING…"
        GameEvents.sendCustomGameEventToServer(WD_RESTART, RestartRequest())
        panorama.schedule(2.0f) {
            requestPending = false
            enabled = true
            label.text = "PLAY AGAIN"
        }
    }
}
