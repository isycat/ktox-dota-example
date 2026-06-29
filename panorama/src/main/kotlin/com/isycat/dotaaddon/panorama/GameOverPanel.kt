package com.isycat.dotaaddon.panorama
import com.isycat.dotaaddon.shared.WD_STATE

import com.isycat.dota.types.panorama.GameEvents
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.WaveState
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * Game-over overlay — a self-contained `@PanoramaView`. It composes a "GAME OVER" title and a
 * [PlayAgainButton], and in [onLoad] subscribes to the wave-state event to show itself only when
 * the run has ended. The button handles the restart; this panel just owns the screen.
 */
@PanoramaView(snippet = false)
class GameOverPanel : Panel(id = "WdGameOver", classes = "WdGameOver") {
    init {
        layout {
            Label(classes = "WdGameOverTitle", text = "GAME OVER")
            PlayAgainButton()
        }
    }

    override fun onLoad() {
        panorama.msg("[GameOverPanel] onLoad — subscribing to wd_state")
        // Hidden until the run ends; wd_state.gameOver toggles it. `visible` is inherited from
        // Panel (not declared here), so it is referenced through `this` explicitly.
        this.visible = false
        GameEvents.subscribe(WD_STATE) { onState(it) }
    }

    private fun onState(state: WaveState) {
        this.visible = state.gameOver
    }
}
