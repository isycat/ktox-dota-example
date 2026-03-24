package com.isycat.dotaaddon.panorama

import com.isycat.dota.panorama.Button
import com.isycat.dota.panorama.Label
import com.isycat.dota.panorama.Panel

fun unitCardWrapperCardPanel2() =
    Panel(classes = "UnitCardZXCTWO", hittest = false) {
        Panel(classes = "CardTransformWrapper", id = "CardTransformWrapperX", hittest = false) {
            Label(id = "CardQuantity", hittest = false)
            Button(classes = "ShopItem", id = "ShopItemX")
        }
    }
