import com.isycat.dota.types.panorama.*
import com.isycat.dotaaddon.panorama.abilitybar.AbilitiesPanel
import com.isycat.dotaaddon.panorama.inventory.ItemsPanel
import com.isycat.dotaaddon.panorama.panels.AnnouncementPanel
import com.isycat.dotaaddon.panorama.panels.BossHpPanel
import com.isycat.dotaaddon.panorama.panels.DeathPanel
import com.isycat.dotaaddon.panorama.panels.EliteFeedPanel
import com.isycat.dotaaddon.panorama.panels.GameOverPanel
import com.isycat.dotaaddon.panorama.panels.WaveStatsPanel

/**
 * Survival Wave Defense HUD, authored in the ktox Panorama Panel DSL and compiled to game_hud.xml. Pure
 * composition: it places self-contained `@PanoramaView`s that each own their layout and behaviour. No
 * `<scripts>` block — ktox-panorama auto-registers the includes for every transpiled JS file.
 */
root {
    styles {
        include(src = "s2r://panorama/styles/dotastyles.css")
        include(src = "file://{resources}/styles/custom_game/game_hud.css")
    }

    Panel(classes = "WaveDefenseHud", hittest = false) {
        WaveStatsPanel()
        BossHpPanel()
        AnnouncementPanel()
        AbilitiesPanel()
        ItemsPanel()
        EliteFeedPanel()
        // DeathPanel before GameOverPanel: the game-over overlay paints over the death screen.
        DeathPanel()
        GameOverPanel()
    }
}
