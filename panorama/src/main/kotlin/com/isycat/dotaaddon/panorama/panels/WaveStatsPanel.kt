package com.isycat.dotaaddon.panorama.panels
import com.isycat.dota.types.panorama.GameEvents
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.WdTokens
import com.isycat.dotaaddon.shared.events.WD_STATE
import com.isycat.dotaaddon.shared.events.WaveState
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * Wave Defense top status strip — a self-contained [PanoramaView] that owns its layout and behaviour.
 * [onLoad] (auto-wired from bootstrap, unlike a CustomUIElement root's onload) is the reliable moment to
 * subscribe to the wave-state event. Placed once in game_hud.dota.xml.kts.
 */
// hittest=false: a non-interactive stats readout — it should never capture mouse events.
@PanoramaView
class WaveStatsPanel : Panel(id = "WdTopBar", type = "Panel", hittest = false) {
    lateinit var waveValue: Label
        private set
    lateinit var scoreValue: Label
        private set
    lateinit var enemiesValue: Label
        private set
    lateinit var nextValue: Label
        private set

    // init composes the layout + binds selectors only — no runtime Panorama calls (those go in onLoad).
    init {
        layout {
            // "#token" label texts localize natively from resource/addon_english.txt.
            Panel(classes = "WdStatBox") {
                Label(classes = "WdCaption", text = "#${WdTokens.HUD_WAVE}")
                Label(id = "WdWaveValue", classes = "WdValue", text = "0") bind ::waveValue
            }
            Panel(classes = "WdStatBox") {
                Label(classes = "WdCaption", text = "#${WdTokens.HUD_POINTS}")
                Label(id = "WdScoreValue", classes = "WdValue", text = "0") bind ::scoreValue
            }
            Panel(classes = "WdStatBox") {
                Label(classes = "WdCaption", text = "#${WdTokens.HUD_NEXT_WAVE}")
                Label(id = "WdNextValue", classes = "WdValue", text = NO_VALUE) bind ::nextValue
            }
            Panel(classes = "WdStatBox") {
                Label(classes = "WdCaption", text = "#${WdTokens.HUD_ENEMIES}")
                Label(id = "WdEnemiesValue", classes = "WdValue", text = "0") bind ::enemiesValue
            }
        }
    }

    override fun onLoad() {
        panorama.msg("[WaveStatsPanel] onLoad — subscribing to wd_state")
        // Hidden until a run is under way (see onState) — avoids flashing an empty strip during pre-init.
        visible = false
        // Typed subscribe via a CustomGameEventKey: `state` is a WaveState, no cast.
        GameEvents.subscribe(WD_STATE) { onState(it) }
    }

    private fun onState(state: WaveState) {
        // Only visible while a run is live and counting down.
        visible = state.running && !state.gameOver
        waveValue.text = "${state.wave}"
        scoreValue.text = "${state.score}"
        enemiesValue.text = "${state.enemiesAlive}"
        if (state.gameOver) {
            nextValue.text = NO_VALUE
        } else {
            nextValue.setDialogVariableInt(WdTokens.VAR_VALUE, state.secondsToNext)
            nextValue.text = panorama.localize("#${WdTokens.NEXT_SECONDS}", nextValue)
        }
    }
}

/** Placeholder glyph for a countdown with no value (pre-run and game over) — not localizable text. */
private const val NO_VALUE = "—"
