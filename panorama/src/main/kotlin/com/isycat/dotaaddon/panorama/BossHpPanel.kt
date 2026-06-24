package com.isycat.dotaaddon.panorama

import com.isycat.dota.types.panorama.GameEvents
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.ProgressBar
import com.isycat.dotaaddon.shared.WaveState
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * Dedicated BOSS health bar — hidden except on boss waves. Driven entirely by pushed [WaveState]
 * (the server computes the boss HP%), so it needs no entity handle. Sits just under the top stats
 * strip and is larger than an ordinary unit bar. Replaces the old always-on hero HP bar, which was
 * redundant with the in-world health bar.
 */
@PanoramaView(snippet = false)
class BossHpPanel : Panel(id = "WdBossBar", type = "Panel") {
    lateinit var nameLabel: Label
        private set
    lateinit var hpProgress: ProgressBar
        private set
    lateinit var hpLabel: Label
        private set

    init {
        layout {
            Label(id = "WdBossName", classes = "WdBossName", text = "") bind ::nameLabel
            Panel(classes = "WdBossTrack") {
                ProgressBar(id = "WdBossProgress", min = 0, max = 100, value = 100) bind ::hpProgress
                Label(id = "WdBossHpLabel", classes = "WdBossHpLabel", text = "") bind ::hpLabel
            }
        }
    }

    override fun onLoad() {
        this.visible = false
        GameEvents.subscribe(WD_STATE) { onState(it) }
    }

    private fun onState(state: WaveState) {
        this.visible = state.bossActive
        if (state.bossActive) {
            nameLabel.text = state.bossName
            hpProgress.value = state.bossHpPercent
            hpLabel.text = "${state.bossHpPercent}%"
        }
    }
}
