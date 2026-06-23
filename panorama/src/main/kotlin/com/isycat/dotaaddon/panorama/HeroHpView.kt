package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.ProgressBar
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * A singleton (Kotlin `object`) `@PanoramaView` for the hero HP bar — the non-snippet,
 * object counterpart to the class-based [WaveStatsView].
 *
 * Unlike a class view (which is instantiated in the DSL and *emits* its panel subtree), an
 * object view is a lazily-resolved view-model **over a panel defined elsewhere**: the HP bar
 * panels are authored as plain DSL in game_hud.dota.xml.kts (id "WdHpBar"), and this singleton
 * resolves them on first access via `$.GetContextPanel().FindChildTraverse("WdHpBar")`, caching
 * the typed child selectors. Frontend code (see [GameHud.onState]) then just writes
 * `HeroHpView.hpLabel.text = …` / `HeroHpView.hpProgress.value = …` — no manual lookups, no
 * onload wiring.
 */
@PanoramaView(snippet = false)
object HeroHpView : Panel(id = "WdHpBar") {
    lateinit var hpProgress: ProgressBar
    lateinit var hpLabel: Label

    init {
        hpProgress = ProgressBar(id = "WdHpProgress")
        hpLabel = Label(id = "WdHpLabel")
    }
}
