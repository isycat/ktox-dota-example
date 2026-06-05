plugins {
    kotlin("jvm")
    id("com.isycat.ktox-panorama")
}

val ktoxSuiteVersion: String by project

dependencies {
    compileOnly("com.isycat:ktox-js:$ktoxSuiteVersion")
    compileOnly("com.isycat:ktox-dota-lib:$ktoxSuiteVersion")
    compileOnly(project(":shared"))
}

ktoxPanorama {
    rootNamespace = "com.isycat.dotaaddon.panorama"
}
