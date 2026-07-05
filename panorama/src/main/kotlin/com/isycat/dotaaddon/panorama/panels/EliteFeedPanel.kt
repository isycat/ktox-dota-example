package com.isycat.dotaaddon.panorama.panels
import com.isycat.dota.types.panorama.GameEvents
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.dotaaddon.shared.WdTokens
import com.isycat.dotaaddon.shared.events.EliteAlert
import com.isycat.dotaaddon.shared.events.WD_ELITE
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * Container for elite-spawn pop-ups. A singleton HUD panel that, on each [WD_ELITE],
 * creates a fresh [EliteSpawnPopup] under itself, fills its text, and lets it auto-dispose after a
 * few seconds — so several elites produce several independent, self-destroying pop-ups at once.
 */
// hittest=false: a non-interactive pop-up feed (260px right-edge column) — it should never capture mouse.
@PanoramaView
class EliteFeedPanel : Panel(id = "WdEliteFeed", type = "Panel", hittest = false) {
    override fun onLoad() {
        GameEvents.subscribe(WD_ELITE) { onElite(it) }
    }

    private fun onElite(alert: EliteAlert) {
        // Live construction: one pop-up per elite, parented into this feed; deleteAsync disposes it.
        // alert.name is the elite's unit name, which is also its display-name loc token. Bind the name
        // dialog variable on THIS (the loaded feed panel) and resolve `#wd_elite_spotted` against it —
        // a dialog variable set on the just-created snippet-less popup isn't applied before its first
        // localize, so `{s:name}` would come back empty. localize() returns the resolved string, which
        // the popup then simply displays.
        val popup = EliteSpawnPopup(this)
        setDialogVariable(WdTokens.VAR_NAME, panorama.localize("#${alert.name}"))
        popup.text = panorama.localize("#${WdTokens.ELITE_SPOTTED}", this)
        popup.deleteAsync(GameConfig.ELITE_POPUP_SECONDS)
    }
}
