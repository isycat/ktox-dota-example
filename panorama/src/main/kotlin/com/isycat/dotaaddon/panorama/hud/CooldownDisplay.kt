package com.isycat.dotaaddon.panorama.hud

import com.isycat.dota.types.EntityIndex
import com.isycat.dota.types.panorama.Abilities
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import kotlin.math.ceil

/**
 * The cooldown readout every HUD slot shares (ability bar + item bar): a numeric label plus the
 * dark radial "spiral" wedge, driven CONTINUOUSLY from the exact remaining fraction — an inline
 * `panel.style.clip = radial(…)` per tick (pocket's technique) sweeps smoothly instead of stepping
 * whole seconds. Owns the overlays' one-time setup (non-hittest, hidden) and every per-tick
 * DOM-write guard, so a slot on the ready state costs zero writes.
 */
class CooldownDisplay(
    private val spiral: Panel,
    private val label: Label,
) {
    private var spiralVisible = false

    /** Skips per-tick DOM writes while the tracked entity sits ready. Cleared by [reset]. */
    private var readyShown = false

    /**
     * The current charge-restore's FULL duration. The panorama API exposes the restore's *remaining*
     * time but not its total, and both GetCooldownLength and GetCooldown report 0 while a charge is
     * replenishing — which collapsed the spiral to a solid wedge. The restore timer resets to its full
     * duration at the start of each cycle, so we capture that peak and sweep the spiral against it.
     */
    private var chargeRestoreTotal = 0f

    init {
        spiral.hittest = false
        spiral.visible = false
        label.hittest = false
        label.visible = false
    }

    /** Forget cached display state (call when the slot points at a different entity). */
    fun reset() {
        readyShown = false
        chargeRestoreTotal = 0f
    }

    /**
     * Per-tick refresh from the engine's own cooldown API. A CHARGE-BASED entity (Hand of Midas,
     * charge abilities) reports 0 cooldownTimeRemaining while a charge is available — the visible
     * timer is whichever of the plain cooldown and the per-charge RESTORE is longer.
     */
    fun refreshFrom(entity: EntityIndex) {
        val cooldownRemaining = Abilities.getCooldownTimeRemaining(entity).toFloat()
        val restore =
            if (Abilities.usesAbilityCharges(entity)) {
                Abilities.getAbilityChargeRestoreTimeRemaining(entity).toFloat()
            } else {
                0f
            }
        // A charge is replenishing and outlasts any plain cooldown: sweep against the restore's full
        // duration (tracked as its peak — see [chargeRestoreTotal]) so the wedge recedes instead of
        // sitting solid.
        if (restore > READY_EPSILON_SECONDS && restore >= cooldownRemaining) {
            if (restore > chargeRestoreTotal) chargeRestoreTotal = restore
            show(restore, chargeRestoreTotal)
            return
        }
        // Plain cooldown: sweep against its own length.
        chargeRestoreTotal = 0f
        show(cooldownRemaining, Abilities.getCooldownLength(entity).toFloat())
    }

    /** Render [remaining] seconds of a [length]-second cooldown (≈0 remaining = ready → hidden). */
    fun show(
        remaining: Float,
        length: Float,
    ) {
        if (remaining > READY_EPSILON_SECONDS) {
            readyShown = false
            label.text = format(remaining)
            label.visible = true
            setFraction(if (length > 0f) remaining / length else 1f)
        } else if (!readyShown) {
            readyShown = true
            label.visible = false
            setFraction(0f)
        }
    }

    /**
     * The dark wedge is the last `deg` degrees before 12 o'clock and recedes clockwise as the
     * cooldown elapses; 0 hides the overlay entirely.
     */
    private fun setFraction(fraction: Float) {
        if (fraction <= 0f) {
            if (spiralVisible) {
                spiral.visible = false
                spiralVisible = false
            }
            return
        }
        if (!spiralVisible) {
            spiral.visible = true
            spiralVisible = true
        }
        val deg = ceil(fraction * FULL_CIRCLE_DEGREES).toInt().coerceIn(1, FULL_CIRCLE_DEGREES.toInt())
        spiral.styleClip = "radial(50% 50%, ${FULL_CIRCLE_DEGREES.toInt() - deg}deg, ${deg}deg)"
    }

    /** 1 decimal under 5s (where the fraction matters), whole seconds above. */
    private fun format(remaining: Float): String =
        if (remaining >= WHOLE_SECONDS_THRESHOLD) {
            "${ceil(remaining).toInt()}"
        } else {
            val whole = remaining.toInt()
            val tenth = ((remaining - whole) * 10f).toInt()
            "$whole.$tenth"
        }

    companion object {
        /** Below this the engine's remaining time is float noise, not a cooldown. */
        private const val READY_EPSILON_SECONDS = 0.05f

        /** Above this the fraction of a second stops mattering to the reader. */
        private const val WHOLE_SECONDS_THRESHOLD = 5f

        private const val FULL_CIRCLE_DEGREES = 360f
    }
}
