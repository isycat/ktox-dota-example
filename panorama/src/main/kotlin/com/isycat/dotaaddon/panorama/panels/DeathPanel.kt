package com.isycat.dotaaddon.panorama.panels
import com.isycat.dota.types.panorama.GameEvents
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.Players
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.dotaaddon.shared.WdTokens
import com.isycat.dotaaddon.shared.events.BuybackRequest
import com.isycat.dotaaddon.shared.events.RestartRequest
import com.isycat.dotaaddon.shared.events.WD_BUYBACK
import com.isycat.dotaaddon.shared.events.WD_RESTART
import com.isycat.dotaaddon.shared.events.WD_STATE
import com.isycat.ktox.panorama.dsl.ON_ACTIVATE
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * Death overlay — shown while the hero waits on the engine's natural respawn timer (deaths don't end the
 * run; the Ancient falling does). Offers a live "Respawn in Xs" countdown plus two outs: START AGAIN
 * (the same restart the game-over screen uses) and BUY BACK, priced by the SHARED
 * [GameConfig.buybackCostForWave] so the button and the server's validation can never disagree.
 * Hidden the instant the hero is alive again, and never shown over the game-over screen.
 */
@PanoramaView
class DeathPanel : Panel(id = "WdDeathScreen", classes = "WdDeathScreen", hittest = false) {
    lateinit var countdown: Label
        private set
    lateinit var startAgain: Label
        private set
    lateinit var buyback: Label
        private set

    // Mirrored from the server's per-tick state push (drives the buyback price + game-over gating).
    private var wave = 0
    private var gameOver = false
    private var running = false

    init {
        layout {
            Panel(id = "WdDeathBox", classes = "WdDeathBox") {
                Label(id = "WdDeathCountdown", classes = "WdDeathCountdown") bind ::countdown
                Panel(classes = "WdDeathButtons") {
                    Label(
                        id = "WdDeathStartAgain",
                        classes = "WdDeathButton",
                        text = "#${WdTokens.START_AGAIN}",
                        hittest = true,
                    ) bind ::startAgain
                    Label(id = "WdDeathBuyback", classes = "WdDeathButton", hittest = true) bind ::buyback
                }
            }
        }
    }

    override fun onLoad() {
        visible = false
        // Never let a click focus these (a focused panel that later hides drops focus to the chat input).
        startAgain.setDisableFocusOnMouseDown(true)
        buyback.setDisableFocusOnMouseDown(true)
        startAgain.setPanelEvent(ON_ACTIVATE) {
            GameEvents.sendCustomGameEventToServer(WD_RESTART, RestartRequest())
        }
        buyback.setPanelEvent(ON_ACTIVATE) {
            // Affordability is a client-side courtesy; the server re-validates against live gold.
            if (Players.getGold(Players.getLocalPlayer()) >= GameConfig.buybackCostForWave(wave)) {
                GameEvents.sendCustomGameEventToServer(WD_BUYBACK, BuybackRequest())
            }
        }
        GameEvents.subscribe(WD_STATE) { state ->
            wave = state.wave
            gameOver = state.gameOver
            running = state.running
        }
        refresh()
    }

    private fun refresh() {
        val player = Players.getLocalPlayer()
        val respawnSeconds = Players.getRespawnSeconds(player).toInt()
        val show = running && !gameOver && respawnSeconds > 0
        if (show) {
            countdown.setDialogVariableInt(WdTokens.VAR_VALUE, respawnSeconds)
            countdown.text = panorama.localize("#${WdTokens.RESPAWN_IN}", countdown)
            val cost = GameConfig.buybackCostForWave(wave)
            buyback.setDialogVariableInt(WdTokens.VAR_VALUE, cost)
            buyback.text = panorama.localize("#${WdTokens.BUYBACK}", buyback)
            // Grey the buyback out (still visible — the price is information) when it can't be afforded.
            if (Players.getGold(player) >= cost) {
                buyback.removeClass(UNAFFORDABLE_CLASS)
            } else {
                buyback.addClass(UNAFFORDABLE_CLASS)
            }
        }
        visible = show
        panorama.schedule(REFRESH_SECONDS) { refresh() }
    }

    companion object {
        /** Fast enough for a whole-second countdown to never look stuck. */
        private const val REFRESH_SECONDS = 0.25f

        /** On the buyback button while the player can't pay (defined in game_hud.scss). */
        private const val UNAFFORDABLE_CLASS = "WdDeathUnaffordable"
    }
}
