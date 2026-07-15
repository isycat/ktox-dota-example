package com.isycat.dotaaddon.panorama.hud

import com.isycat.dota.types.EntityIndex
import com.isycat.dota.types.panorama.Abilities
import com.isycat.dota.types.panorama.Game
import com.isycat.dota.types.panorama.Label
import com.isycat.dota.types.panorama.Panel
import com.isycat.dota.types.panorama.panorama
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
     * The restore countdown for the LABEL. GetAbilityChargeRestoreTimeRemaining is server-quantized
     * (unlike GetCooldownTimeRemaining, which the client interpolates per frame), so the label counts
     * down against [Game.gameTime], anchored once at the start of the cycle. The WEDGE doesn't use this
     * at all — it runs as one long clip transition (see [beginRestoreSweep]).
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

    /** True while the wedge is riding a whole-cycle clip transition; per-tick code must not touch it. */
    private var restoreSweepActive = false

    /**
     * The next wedge write must SNAP, not glide: after a reset (slot re-bind, unit switch), on a freshly
     * built slot (every ability upgrade rebuilds the bar), and whenever the wedge re-appears from hidden,
     * the clip transition would otherwise animate from the stale/default sector to the new one — a
     * visible sweep that lies about the cooldown.
     */
    private var snapNextWrite = true

    /** Set by a snap write; the FOLLOWING tick's write (a different render frame) restores the 0.11s. */
    private var restoreTickTransitionOnWrite = false

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
        snapNextWrite = true
        endRestoreSweep()
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
                // Entering restore display, or a NEW cycle began (the engine value rose): anchor to it and
                // launch the whole wedge sweep as ONE clip transition — per-tick writes can't jitter it.
                inRestore = true
                shownRestore = restore
                beginRestoreSweep(restore, ChargeRestoreTotals.learn(entityName, restore))
            } else {
                // The wedge is riding its transition; only the LABEL ticks, counting down on the game
                // clock (the raw engine value is stair-stepped — see [lastEngineRestore]).
                shownRestore -= dt
                if (shownRestore < 0f) shownRestore = 0f
            }
            lastEngineRestore = restore
            readyShown = false
            label.text = format(shownRestore)
            label.visible = true
            return
        }
        // Plain cooldown: sweep against its own length (client-interpolated — already smooth per-tick).
        inRestore = false
        lastEngineRestore = 0f
        endRestoreSweep()
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
            setFraction(0f) // hides the wedge AND pre-arms the next appearance to snap (see below)
        }
    }

    /**
     * Launch the wedge for a whole restore cycle as ONE clip transition: snap (zero-duration) to the true
     * current sector, then — next frame, so the snap lands as a separate style state — animate to an empty
     * sector over exactly [remaining] seconds. The ENGINE interpolates the sweep every render frame and
     * per-tick JS never touches the wedge again, so nothing can stutter it. A transition (not a keyframe
     * animation) starts from the panel's CURRENT value, so a mid-cycle re-anchor (unit-switch re-bind)
     * resumes at the true angle instead of resetting to full.
     */
    private fun beginRestoreSweep(
        remaining: Float,
        total: Float,
    ) {
        restoreSweepActive = true
        snapNextWrite = false // the sweep launch snaps by construction
        if (!spiralVisible) {
            spiral.visible = true
            spiralVisible = true
        }
        val fraction = if (total > 0f) remaining / total else 1f
        spiral.style.transitionDuration = ZERO_DURATION
        applyClipDegrees(ceil(fraction * FULL_CIRCLE_DEGREES).toInt().coerceIn(1, FULL_CIRCLE_DEGREES.toInt()))
        panorama.schedule(0f) {
            // A reset/unit-switch between the snap and this frame aborts the launch (a new sweep re-anchors).
            if (restoreSweepActive) {
                spiral.style.transitionDuration = "${remaining}s"
                applyClipDegrees(0)
            }
        }
    }

    /** Hand the wedge back to the per-tick writer (plain cooldowns) with its normal snappy transition. */
    private fun endRestoreSweep() {
        if (!restoreSweepActive) return
        restoreSweepActive = false
        spiral.style.transitionDuration = TICK_TRANSITION_DURATION
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
                // Pre-arm the next appearance: zero the transition NOW, while hidden, so it is committed
                // frames before a fresh cooldown's first write — which must land instantly, not glide
                // from the stale sector this cooldown left behind.
                spiral.style.transitionDuration = ZERO_DURATION
                snapNextWrite = true
            }
            return
        }
        if (!spiralVisible) {
            spiral.visible = true
            spiralVisible = true
        }
        val degrees = ceil(fraction * FULL_CIRCLE_DEGREES).toInt().coerceIn(1, FULL_CIRCLE_DEGREES.toInt())
        if (snapNextWrite) {
            // First write after a reset / rebuild / re-appearance: land instantly on the true sector.
            // A fresh cast used to blink empty→full here: the zero-duration write raced the engine's
            // style batching when set in the SAME frame the wedge became visible, so the clip change
            // animated with the old duration. The zero duration is therefore pre-armed at HIDE time
            // (below) — committed frames earlier — and handed back on the NEXT tick's write, never in
            // the same frame.
            snapNextWrite = false
            spiral.style.transitionDuration = ZERO_DURATION
            applyClipDegrees(degrees)
            restoreTickTransitionOnWrite = true
            return
        }
        if (restoreTickTransitionOnWrite) {
            // The tick after a snap (a different render frame): back to the smooth per-tick transition.
            restoreTickTransitionOnWrite = false
            spiral.style.transitionDuration = TICK_TRANSITION_DURATION
        }
        applyClipDegrees(degrees)
    }

    /** The raw radial-clip write: the wedge is the last [degrees] before 12 o'clock (0 = empty sector). */
    private fun applyClipDegrees(degrees: Int) {
        spiral.style.clip = "radial(50% 50%, ${FULL_CIRCLE_DEGREES.toInt() - degrees}deg, ${degrees}deg)"
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

        /** Per-tick mode's clip transition — MUST match `transition-duration` in `_hud_slot.scss`. */
        private const val TICK_TRANSITION_DURATION = "0.11s"

        /** Canonical zero duration for snap writes and sweep launches. */
        private const val ZERO_DURATION = "0s"
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
