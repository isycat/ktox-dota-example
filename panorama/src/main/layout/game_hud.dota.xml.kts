import com.isycat.dota.types.panorama.*
import com.isycat.dotaaddon.panorama.AbilitiesPanel
import com.isycat.dotaaddon.panorama.AnnouncementPanel
import com.isycat.dotaaddon.panorama.BossHpPanel
import com.isycat.dotaaddon.panorama.EliteFeedPanel
import com.isycat.dotaaddon.panorama.GameOverPanel
import com.isycat.dotaaddon.panorama.ItemsPanel
import com.isycat.dotaaddon.panorama.WaveStatsPanel

/**
 * Survival Wave Defense HUD, authored in the ktox Panorama Panel DSL (Kotlin) and compiled to
 * game_hud.xml.
 *
 * This layout is pure composition: it places three self-contained `@PanoramaView`s, each of which
 * owns its own layout AND behaviour (it subscribes to the server's events from its auto-wired
 * bootstrap `onload`). There is no controller and no `<scripts>` block — ktox-panorama
 * auto-registers the script includes for every transpiled JS file.
 */
root {
    styles {
        include(src = "s2r://panorama/styles/dotastyles.css")
        include(src = "file://{resources}/styles/custom_game/game_hud.css")
    }

    // The root carries a class and needs no `onload`: each view below emits its own panel with an
    // auto-wired bootstrap onload that fires.
    Panel(classes = "WaveDefenseHud", hittest = false) {
        WaveStatsPanel()
        // Boss HP bar, just under the top strip — shown only during boss waves.
        BossHpPanel()
        AnnouncementPanel()
        // Custom abilities + talents bar replacing the (hidden) stock action panel.
        AbilitiesPanel()
        // Custom inventory bar (bottom-right) replacing the (hidden) stock inventory panel; reads and
        // acts on the real inventory via the engine's own item APIs + cast orders.
        ItemsPanel()
        // Feed of elite-spawn pop-ups, created/disposed on the fly (one per elite).
        EliteFeedPanel()
        // Game-over overlay with a Play Again button (shown only when the run ends).
        GameOverPanel()
    }
}
