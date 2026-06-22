import com.isycat.dota.types.panorama.*

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
        include(src = "file://{resources}/scripts/custom_game/GameHud.js")
    }

    styles {
        include(src = "s2r://panorama/styles/dotastyles.css")
        include(src = "file://{resources}/styles/custom_game/game_hud.css")
    }

    Panel(id = "WaveDefenseHud", hittest = false, onload = "gameHudInit()") {
        // Top status strip: wave / score / enemies / next-wave countdown.
        Panel(id = "WdTopBar") {
            Panel(id = "WdWaveBox", classes = "WdStatBox") {
                Label(id = "WdWaveCaption", classes = "WdCaption", text = "WAVE")
                Label(id = "WdWaveValue", classes = "WdValue", text = "0")
            }
            Panel(id = "WdScoreBox", classes = "WdStatBox") {
                Label(id = "WdScoreCaption", classes = "WdCaption", text = "SCORE")
                Label(id = "WdScoreValue", classes = "WdValue", text = "0")
            }
            Panel(id = "WdEnemiesBox", classes = "WdStatBox") {
                Label(id = "WdEnemiesCaption", classes = "WdCaption", text = "ENEMIES")
                Label(id = "WdEnemiesValue", classes = "WdValue", text = "0")
            }
            Panel(id = "WdNextBox", classes = "WdStatBox") {
                Label(id = "WdNextCaption", classes = "WdCaption", text = "NEXT WAVE")
                Label(id = "WdNextValue", classes = "WdValue", text = "--")
            }
        }

        // Hero health bar.
        Panel(id = "WdHpBar") {
            ProgressBar(id = "WdHpProgress", min = 0, max = 100, value = 100)
            Label(id = "WdHpLabel", classes = "WdValue", text = "100%")
        }

        // Centre-screen announcement ("Wave 3 incoming!", "GAME OVER", ...).
        Label(id = "WdAnnouncement", text = "")
    }
}
