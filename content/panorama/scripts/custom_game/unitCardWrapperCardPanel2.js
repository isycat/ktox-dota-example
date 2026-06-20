// package: com.isycat.dotaaddon.panorama

function unitCardWrapperCardPanel2() {
    return new Panel("UnitCardZXCTWO", false, () => {
        return new Panel("CardTransformWrapper", "CardTransformWrapperX", false, () => {
            new Label("CardQuantity", false);
            return new Button("ShopItem", "ShopItemX");
        });
    });
}

