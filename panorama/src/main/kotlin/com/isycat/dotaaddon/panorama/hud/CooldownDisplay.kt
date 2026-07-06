package com.isycat.dotaaddon.panorama.hud

import com.isycat.dota.types.EntityIndex
import com.isycat.dota.types.panorama.Abilities
import com.isycat.dota.types.panorama.Game
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

    /** Whether the display is currently sweeping a charge RESTORE (sticky until that restore finishes). */
    private var inRestore = false

    /**
     * The restore countdown actually rendered. GetAbilityChargeRestoreTimeRemaining is server-quantized
     * (unlike GetCooldownTimeRemaining, which the client interpolates per frame), so rendering it raw
     * makes the wedge step visibly. Instead this counts down smoothly against [Game.gameTime], anchored
     * once at the start of the cycle.
     */
    private var shownRestore = 0f

    /**
     * The engine's raw restore-remaining from the previous tick. Within ONE cycle the quantized value
     * only ever steps DOWN, so an INCREASE is the only reliable new-cycle signal — comparing against the
     * smoothed countdown instead (with any fixed threshold) misfires whenever the engine's quantization
     * step exceeds the threshold, snapping the sweep back up every stair: a sawtooth.
     */
    private var lastEngineRestore = 0f

    /** Game-clock timestamp of the previous tick (drives the smooth countdown; pauses stop with it). */
    private var lastTickTime = 0f

    init {
        spiral.hittest = false
        spiral.visible = false
        label.hittest = false
        label.visible = false
    }

    /** Forget cached display state (call when the slot points at a different entity). */
    fun reset() {
        readyShown = false
        inRestore = false
        shownRestore = 0f
        lastEngineRestore = 0f
    }

    /**
     * Per-tick refresh from the engine's own cooldown API. A CHARGE-BASED entity (Hand of Midas,
     * charge abilities) reports 0 cooldownTimeRemaining while a charge is available — the visible
     * timer is whichever of the plain cooldown and the per-charge RESTORE is longer. [entityName]
     * keys the learned restore-duration cache (see [ChargeRestoreTotals]).
     */
    fun refreshFrom(
        entity: EntityIndex,
        entityName: String,
    ) {
        val cooldownRemaining = Abilities.getCooldownTimeRemaining(entity)
        val restore =
            if (Abilities.usesAbilityCharges(entity)) {
                Abilities.getAbilityChargeRestoreTimeRemaining(entity).toFloat()
            } else {
                0f
            }
        val now = Game.gameTime.toFloat()
        var dt = now - lastTickTime
        if (dt < 0f) dt = 0f
        if (dt > MAX_TICK_SECONDS) dt = MAX_TICK_SECONDS
        lastTickTime = now

        // A charge is replenishing: sweep the restore. STICKY — once a restore is being displayed it keeps
        // the display until it finishes, so a shorter inter-cast cooldown racing it can't flip the sweep
        // back and forth between two different timers (that alternation read as jitter).
        if (restore > READY_EPSILON_SECONDS && (inRestore || restore >= cooldownRemaining)) {
            if (!inRestore || restore > lastEngineRestore + READY_EPSILON_SECONDS) {
                // Entering restore display, or a NEW cycle began (the engine value rose): anchor to it.
                inRestore = true
                shownRestore = restore
            } else {
                // Count down purely on the game clock — the anchor was exact at cycle start, so real time
                // stays within one quantization step of the engine's stairs. Deliberately NOT clamped to
                // the raw engine value: chasing the stairs is what produced the visible jumps.
                shownRestore -= dt
                if (shownRestore < 0f) shownRestore = 0f
            }
            lastEngineRestore = restore
            show(shownRestore, ChargeRestoreTotals.learn(entityName, restore))
            return
        }
        // Plain cooldown: sweep against its own length (client-interpolated — already smooth).
        inRestore = false
        lastEngineRestore = 0f
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

        /** Cap a tick's smoothing step so a hitch/tab-out doesn't lurch the countdown. */
        private const val MAX_TICK_SECONDS = 0.5f
    }
}

/**
 * Learned full charge-restore durations, keyed by ability/item NAME. The panorama API exposes a restore's
 * *remaining* time but not its total (GetCooldownLength reports 0 mid-restore), so the sweep denominator
 * is the highest remaining ever observed for that name — captured at the start of a cycle, when remaining
 * ≈ the full duration. Shared across every slot view and keyed by name so the value survives slot
 * re-binds and unit switches: without this, re-selecting a unit re-peaked the denominator at the CURRENT
 * remaining, which snapped the wedge to 100%.
 */
private object ChargeRestoreTotals {
    private val totals = mutableMapOf<String, Float>()

    /** Fold [restoreRemaining] into the learned total for [entityName] and return the best-known total. */
    fun learn(
        entityName: String,
        restoreRemaining: Float,
    ): Float {
        val prior = totals.getOrDefault(entityName, 0f)
        if (restoreRemaining > prior) {
            totals.put(entityName, restoreRemaining)
            return restoreRemaining
        }
        return prior
    }
}
