import com.isycat.dota.types.panorama.*
import com.isycat.dotaaddon.panorama.WaveStatsView

/**
 * Survival Wave Defense HUD, authored in the ktox Panorama Panel DSL (Kotlin)
 * instead of raw XML. This .dota.xml.kts is compiled to game_hud.xml and loaded
 * by custom_ui_manifest.xml.
 *
 * The root panel's `onload` calls `gameHudInit()` (defined in GameHud.js, which
 * is transpiled from GameHud.kt and included below), wiring the HUD up to the
 * server-sent game-state events.
 */
root {
    scripts {
        // ktox runtime + shared constants/state, then the HUD logic. No module
        // system exists in Panorama, so dependencies are included in order.
        include(src = "file://{resources}/scripts/custom_game/ktox_panorama.js")
        include(src = "file://{resources}/scripts/custom_game/shared/GameConfig.js")
        include(src = "file://{resources}/scripts/custom_game/WaveStatsView.js")
        include(src = "file://{resources}/scripts/custom_game/HeroHpView.js")
        include(src = "file://{resources}/scripts/custom_game/GameHud.js")
    }

    styles {
        include(src = "s2r://panorama/styles/dotastyles.css")
        include(src = "file://{resources}/styles/custom_game/game_hud.css")
    }

    // NOTE: a layout's root panel must NOT have an `id` (Panorama compile rule) — use a class.
    Panel(classes = "WaveDefenseHud", hittest = false, onload = "gameHudInit()") {
        // Top status strip: a @PanoramaView (wave / score / enemies / next-wave countdown).
        // Instantiating the view here emits its panel subtree plus
        // onload="WaveStatsView.bootstrap(this)"; GameHud.onState then drives its bound labels.
        WaveStatsView()

        // Hero health bar.
        Panel(id = "WdHpBar") {
            ProgressBar(id = "WdHpProgress", min = 0, max = 100, value = 100)
            Label(id = "WdHpLabel", classes = "WdValue", text = "100%")
        }

        // Centre-screen announcement ("Wave 3 incoming!", "GAME OVER", ...).
        Label(id = "WdAnnouncement", text = "")
    }
}
