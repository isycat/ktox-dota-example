package com.isycat.dotaaddon

import com.isycat.dota.types.lua.CScriptPrecacheContext
import com.isycat.dota.types.lua.precacheUnitByNameSync
import com.isycat.dotaaddon.main

fun Precache(context: CScriptPrecacheContext) {
    precacheUnitByNameSync("npc_dota_hero_lich", context, null)
    precacheUnitByNameSync("npc_dota_hero_dark_willow", context, null)
    precacheUnitByNameSync("npc_dota_hero_dark_zuus", context, null)
    precacheUnitByNameSync("npc_dota_hero_dark_omniknight", context, null)
}
