plugins {
    kotlin("jvm")
    id("com.isycat.ktox-panorama")
}

dependencies {
    compileOnly("com.isycat:ktox-js:0.2.5")
    compileOnly("com.isycat:ktox-dota-lib:0.2.5")
    compileOnly(project(":shared"))
}

ktoxPanorama {
    rootNamespace = "com.isycat.dotaaddon.panorama"
}
