package com.isycat.dotaaddon.panorama.panels
import com.isycat.dota.types.panorama.GameEvents
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.ProgressBar
import com.isycat.dotaaddon.shared.events.WD_STATE
import com.isycat.dotaaddon.shared.events.WaveState
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * Dedicated boss health bar — hidden except on boss waves. Driven entirely by pushed [WaveState] (the
 * server computes the boss HP%), so it needs no entity handle.
 */
@PanoramaView(snippet = false)
class BossHpPanel : Panel(id = "WdBossBar", type = "Panel", hittest = false) {
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
        visible = false
        GameEvents.subscribe(WD_STATE) { onState(it) }
    }

    private fun onState(state: WaveState) {
        visible = state.bossActive
        if (state.bossActive) {
            nameLabel.text = state.bossName
            hpProgress.value = state.bossHpPercent
            hpLabel.text = "${state.bossHpPercent}%"
        }
    }
}
