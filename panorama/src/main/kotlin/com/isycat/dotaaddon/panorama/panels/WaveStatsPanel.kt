package com.isycat.dotaaddon.panorama.panels
import com.isycat.dotaaddon.shared.events.WD_STATE

import com.isycat.dota.types.panorama.GameEvents
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.events.WaveState
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * Wave Defense top status strip — a self-contained `@PanoramaView` that owns BOTH its layout and
 * its behaviour. There is no separate controller: a `@PanoramaView` already auto-wires the panel's
 * `onload` from bootstrap, which runs this class's `init` in the panel's own JS context. That is
 * the correct, reliable moment to register events — a `CustomUIElement` layout's *root* `onload`
 * does not fire, but a view panel's auto-wired `onload` does. So the strip composes its stat boxes,
 * subscribes to the server's wave-state event, and writes its own labels. Placed once in
 * game_hud.dota.xml.kts via `WaveStatsPanel()`.
 */
@PanoramaView(snippet = false)
// hittest=false: a non-interactive stats readout — it should never capture mouse events.
class WaveStatsPanel : Panel(id = "WdTopBar", type = "Panel", hittest = false) {
    lateinit var waveValue: Label
        private set
    lateinit var scoreValue: Label
        private set
    lateinit var enemiesValue: Label
        private set
    lateinit var nextValue: Label
        private set

    // init only composes the layout + binds selectors. This runs at DSL-compile time too, so it
    // must contain no runtime (Panorama) calls — those go in onLoad().
    init {
        layout {
            Panel(classes = "WdStatBox") {
                Label(classes = "WdCaption", text = "WAVE")
                Label(id = "WdWaveValue", classes = "WdValue", text = "0") bind ::waveValue
            }
            Panel(classes = "WdStatBox") {
                Label(classes = "WdCaption", text = "POINTS")
                Label(id = "WdScoreValue", classes = "WdValue", text = "0") bind ::scoreValue
            }
            Panel(classes = "WdStatBox") {
                Label(classes = "WdCaption", text = "NEXT WAVE")
                Label(id = "WdNextValue", classes = "WdValue", text = "--") bind ::nextValue
            }
            Panel(classes = "WdStatBox") {
                Label(classes = "WdCaption", text = "ENEMIES")
                Label(id = "WdEnemiesValue", classes = "WdValue", text = "0") bind ::enemiesValue
            }
        }
    }

    // The @PanoramaView lifecycle hook: chained onto the auto-wired bootstrap onload, it runs once
    // when the panel loads, in this layout's JS context — the right time/place to register events.
    override fun onLoad() {
        panorama.msg("[WaveStatsPanel] onLoad — subscribing to wd_state")
        // Hidden until a real "next wave" value exists — i.e. until a run is actually under way (see
        // onState). Avoids flashing a 0/0/--/0 strip during hero selection / pre-init.
        visible = false
        // Typed subscribe via a PanoramaEventKey: `state` is a WaveState, no cast.
        GameEvents.subscribe(WD_STATE) { onState(it) }
    }

    private fun onState(state: WaveState) {
        // Only show the strip while a run is live and counting down to the next wave; the next-wave
        // value isn't meaningful before the run starts or after it's over.
        visible = state.running && !state.gameOver
        waveValue.text = "${state.wave}"
        scoreValue.text = "${state.score}"
        enemiesValue.text = "${state.enemiesAlive}"
        nextValue.text = if (state.gameOver) "--" else "${state.secondsToNext}s"
    }
}
