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
    init {
        layout {
            Label(classes = "WdPlayAgainLabel", text = "PLAY AGAIN")
        }
    }

    override fun onActivate() {
        panorama.msg("[PlayAgainButton] onActivate — requesting restart")
        GameEvents.sendCustomGameEventToServer(WD_RESTART, RestartRequest())
    }
}
