package com.isycat.dotaaddon.shared

/**
 * Localization token names — the shared contract between the server (which SENDS tokens, never
 * display text) and the HUD (which localizes them via `$.Localize("#token", panel)` or a `#token`
 * layout text). The English strings live in `resource/addon_english.txt`; adding a language means
 * adding a resource file, never touching code.
 *
 * Parameterized tokens reference dialog variables: `{d:VAR_VALUE}` / `{d:VAR_VALUE2}` for the
 * [com.isycat.dotaaddon.shared.events.Announcement] numeric slots, `{s:VAR_NAME}` for its name slot.
 */
object WdTokens {
    // Dialog-variable names the parameterized tokens reference.
    const val VAR_VALUE = "value"
    const val VAR_VALUE2 = "value2"
    const val VAR_NAME = "name"

    // Server announcements (parameterized — see Announcement's value/value2/name slots).
    const val PREPARE = "wd_prepare"
    const val WAVE_INCOMING = "wd_wave_incoming"
    const val BOSS_ARRIVED = "wd_boss_arrived"
    const val GAMEOVER_DIED = "wd_gameover_died"
    const val GAMEOVER_ANCIENT = "wd_gameover_ancient"
    const val CHEAT_SKIP = "wd_cheat_skip"

    // HUD strings the client localizes itself.
    const val ELITE_SPOTTED = "wd_elite_spotted"
    const val NEXT_SECONDS = "wd_next_seconds"
    const val PLAY_AGAIN = "wd_play_again"
    const val RESTARTING = "wd_restarting"
    const val HUD_WAVE = "wd_hud_wave"
    const val HUD_POINTS = "wd_hud_points"
    const val HUD_NEXT_WAVE = "wd_hud_next_wave"
    const val HUD_ENEMIES = "wd_hud_enemies"
    const val GAME_OVER_TITLE = "wd_game_over_title"
    const val STAT_RUN_TIME = "wd_stat_run_time"
    const val STAT_WAVES = "wd_stat_waves"
    const val STAT_SCORE = "wd_stat_score"
    const val STAT_KILLS = "wd_stat_kills"
    const val STAT_BOSSES = "wd_stat_bosses"
    const val STAT_GOLD = "wd_stat_gold"
}
