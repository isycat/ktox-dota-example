package com.isycat.dotaaddon.panorama

import com.isycat.dota.panorama.*

fun unitCardWrapperCardPanel() =
    Panel(classes = "UnitCardZXC", hittest = false) {
        Panel(classes = "CardTransformWrapper", id = "CardTransformWrapperX", hittest = false) {
            Label(id = "CardQuantity", hittest = false)
            Button(classes = "ShopItem", id = "ShopItemX")
        }
    }
