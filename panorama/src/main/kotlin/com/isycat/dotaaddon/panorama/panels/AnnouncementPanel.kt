package com.isycat.dotaaddon.panorama.panels
import com.isycat.dota.types.panorama.GameEvents
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.panorama
import com.isycat.dotaaddon.shared.GameConfig
import com.isycat.dotaaddon.shared.events.Announcement
import com.isycat.dotaaddon.shared.events.WD_MESSAGE
import com.isycat.ktox.panorama.dsl.PanoramaView

/**
 * Centre-screen transient announcement ("Wave 3 incoming!", …). [onLoad] subscribes to the message event,
 * shows the text, then auto-clears it after a few seconds. hittest = false so it doesn't block the game.
 */
@PanoramaView(snippet = false)
class AnnouncementPanel : Panel(id = "WdAnnouncement", type = "Panel", hittest = false) {
    private lateinit var label: Label
    private var seq = 0

    init {
        layout {
            label = Label(classes = "AnnouncementLabel", text = "")
        }
    }

    override fun onLoad() {
        GameEvents.subscribe(WD_MESSAGE) { onMessage(it) }
    }

    private fun onMessage(message: Announcement) {
        seq += 1
        val shown = seq
        label.text = message.text
        // Transient: clear after a short delay — unless a newer message has already replaced it.
        panorama.schedule(GameConfig.ANNOUNCEMENT_SECONDS) {
            if (shown == seq) label.text = ""
        }
    }
}
