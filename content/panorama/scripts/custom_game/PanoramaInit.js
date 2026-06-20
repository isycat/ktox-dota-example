// package: com.isycat.dotaaddon.panorama

function panoramaInit() {
    $.Msg(AddonInfo.getWelcomeMessage());
    const mainPanel = panorama.FindChild("MainUI") ?? throw new Exception("MainUI panel not found");
    Panel_N_ext_invoke(mainPanel, ".AddonNameLabel").forEach((it) => {
        (it).text = AddonInfo.NAME;
    });
    KtoxSequence.of([1, 2, 3]).forEach((it) => {
        $.Msg(it);
    });
    KtoxSequence.of([4, 2, 3]).forEach((it) => {
        $.Msg(it);
    });
}

