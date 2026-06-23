package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * A `@PanoramaView` for the Wave Defense top status strip.
 *
 * A `@PanoramaView` is a single Kotlin class that produces **both**:
 *  - the layout (instantiated in the DSL — see [game_hud.dota.xml.kts] which calls
 *    `WaveStatsView()` inside the HUD), and
 *  - the frontend view-model (transpiled to WaveStatsView.js). The auto-injected
 *    `onload="WaveStatsView.bootstrap(this)"` binds each `lateinit` property to its child
 *    panel via the ktox_panorama.js runtime, after which frontend code (see [GameHud.onState])
 *    just writes `view.waveValue.text = …`.
 *
 * Each stat box is bound so its value label resolves against the correct parent — selectors
 * only walk direct children, so the intermediate box must be bound too.
 */
// Default `@PanoramaView` (snippet = true): the view is emitted as a <snippets><snippet>
// definition plus an <include snippet="WaveStatsView"/> reference at the usage site. This is
// the snippet-class variant. (The non-snippet, inline variant is shown by the singleton
// object view [HeroHpView].)
@PanoramaView
class WaveStatsView :
    Panel(
        id = "WdTopBar",
        type = Panel::class.java.simpleName,
    ) {
    lateinit var waveBox: Panel
        private set
    lateinit var waveValue: Label
        private set

    lateinit var scoreBox: Panel
        private set
    lateinit var scoreValue: Label
        private set

    lateinit var enemiesBox: Panel
        private set
    lateinit var enemiesValue: Label
        private set

    lateinit var nextBox: Panel
        private set
    lateinit var nextValue: Label
        private set

    init {
        layout {
            Panel(id = "WdWaveBox", classes = "WdStatBox") {
                bind(::waveBox)
                Label(classes = "WdCaption", text = "WAVE")
                Label(classes = "WdValue", text = "0") bind ::waveValue
            }
            Panel(id = "WdScoreBox", classes = "WdStatBox") {
                bind(::scoreBox)
                Label(classes = "WdCaption", text = "SCORE")
                Label(classes = "WdValue", text = "0") bind ::scoreValue
            }
            Panel(id = "WdEnemiesBox", classes = "WdStatBox") {
                bind(::enemiesBox)
                Label(classes = "WdCaption", text = "ENEMIES")
                Label(classes = "WdValue", text = "0") bind ::enemiesValue
            }
            Panel(id = "WdNextBox", classes = "WdStatBox") {
                bind(::nextBox)
                Label(classes = "WdCaption", text = "NEXT WAVE")
                Label(classes = "WdValue", text = "--") bind ::nextValue
            }
        }
    }
}
