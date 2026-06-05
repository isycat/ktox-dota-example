plugins {
    kotlin("jvm")
    id("com.isycat.ktox-panorama")
}

dependencies {
    compileOnly("com.isycat:ktox-js")
//    compileOnly("com.isycat:ktox-dota-lib")
    compileOnly(project(":shared"))
}

ktoxPanorama {
    rootNamespace = "com.isycat.dotaaddon.panorama"
}
