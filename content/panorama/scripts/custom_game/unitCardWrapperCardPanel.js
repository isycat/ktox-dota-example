// package: com.isycat.dotaaddon.panorama

function unitCardWrapperCardPanel() {
    return new Panel("UnitCardZXC", false, () => {
        return new Panel("CardTransformWrapper", "CardTransformWrapperX", false, () => {
            new Label("CardQuantity", false);
            return new Button("ShopItem", "ShopItemX");
        });
    });
}

