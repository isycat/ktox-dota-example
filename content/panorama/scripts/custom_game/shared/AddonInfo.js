// package: com.isycat.dotaaddon.shared

const AddonInfo = Object.freeze({
    NAME: "ktoxtest",
    VERSION: "1.0.0",
    getWelcomeMessage() {
        return `Hello from ${this.NAME} v${this.VERSION}!`;
    },
    printSomeNumbers() {
        KtoxSequence.of([1, 2, 3]).forEach((it) => {
            console.log(it);
        });
        [1, 2, 3].forEach((it) => {
            console.log(it);
        });
        KtoxSequence.of([1, 2, 3]).forEach((it) => {
            console.log(it);
        });
    },
});

