package com.isycat.dotaaddon.panorama.panels
import com.isycat.dota.types.panorama.GameEvents
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.WdTokens
import com.isycat.dotaaddon.shared.events.RunStats
import com.isycat.dotaaddon.shared.events.WD_RUN_STATS
import com.isycat.dotaaddon.shared.events.WD_STATE
import com.isycat.dotaaddon.shared.events.WaveState
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * Game-over overlay — a self-contained `@PanoramaView`. It composes a "GAME OVER" title, the
 * end-of-run scoreboard, and a [PlayAgainButton], and in [onLoad] subscribes to the wave-state
 * event to show itself only when the run has ended. The scoreboard renders the ONE-SHOT
 * [WD_RUN_STATS] event the server sends at run end — a single typed payload carries the whole
 * summary, so the client does no aggregation of its own.
 */
@PanoramaView
class GameOverPanel : Panel(id = "WdGameOver", classes = "WdGameOver") {
    lateinit var statWaves: Label
        private set
    lateinit var statScore: Label
        private set
    lateinit var statKills: Label
        private set
    lateinit var statBosses: Label
        private set
    lateinit var statGold: Label
        private set
    lateinit var statDuration: Label
        private set

    init {
        layout {
            // "#token" label texts localize natively from resource/addon_english.txt.
            Label(classes = "WdGameOverTitle", text = "#${WdTokens.GAME_OVER_TITLE}")
            Panel(id = "WdRunStats", classes = "WdRunStats") {
                Panel(classes = "WdRunStatsRow") {
                    Label(classes = "WdRunStatName", text = "#${WdTokens.STAT_RUN_TIME}")
                    Label(id = "WdStatDuration", classes = "WdRunStatValue") bind ::statDuration
                }
                Panel(classes = "WdRunStatsRow") {
                    Label(classes = "WdRunStatName", text = "#${WdTokens.STAT_WAVES}")
                    Label(id = "WdStatWaves", classes = "WdRunStatValue") bind ::statWaves
                }
                Panel(classes = "WdRunStatsRow") {
                    Label(classes = "WdRunStatName", text = "#${WdTokens.STAT_SCORE}")
                    Label(id = "WdStatScore", classes = "WdRunStatValue") bind ::statScore
                }
                Panel(classes = "WdRunStatsRow") {
                    Label(classes = "WdRunStatName", text = "#${WdTokens.STAT_KILLS}")
                    Label(id = "WdStatKills", classes = "WdRunStatValue") bind ::statKills
                }
                Panel(classes = "WdRunStatsRow") {
                    Label(classes = "WdRunStatName", text = "#${WdTokens.STAT_BOSSES}")
                    Label(id = "WdStatBosses", classes = "WdRunStatValue") bind ::statBosses
                }
                Panel(classes = "WdRunStatsRow") {
                    Label(classes = "WdRunStatName", text = "#${WdTokens.STAT_GOLD}")
                    Label(id = "WdStatGold", classes = "WdRunStatValue") bind ::statGold
                }
            }
            PlayAgainButton()
        }
    }

    override fun onLoad() {
        panorama.msg("[GameOverPanel] onLoad — subscribing to wd_state + wd_run_stats")
        // Hidden until the run ends; wd_state.gameOver toggles it.
        visible = false
        GameEvents.subscribe(WD_STATE) { onState(it) }
        GameEvents.subscribe(WD_RUN_STATS) { onStats(it) }
    }

    private fun onState(state: WaveState) {
        visible = state.gameOver
    }

    private fun onStats(stats: RunStats) {
        statDuration.text = formatDuration(stats.runSeconds)
        statWaves.text = "${stats.wavesSurvived}"
        statScore.text = "${stats.score}"
        statKills.text = "${stats.kills}"
        statBosses.text = "${stats.bossesSlain}"
        statGold.text = "${stats.goldEarned}"
    }

    /** m:ss (hours fold into minutes — a 90-minute run reads 90:00, not 1:30:00). */
    private fun formatDuration(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val paddedSeconds = if (seconds < 10) "0$seconds" else "$seconds"
        return "$minutes:$paddedSeconds"
    }
}
