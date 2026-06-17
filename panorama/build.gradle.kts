plugins {
    kotlin("jvm")
    id("com.isycat.ktox-panorama")
}

val ktoxSuiteVersion: String by project
val ktoxDotaVersion: String by project

dependencies {
    compileOnly("com.isycat:ktox-js:$ktoxSuiteVersion")
    compileOnly("com.isycat:ktox-dota-lib:$ktoxDotaVersion")
    compileOnly(project(":shared"))
}

ktoxPanorama {
    rootNamespace = "com.isycat.dotaaddon.panorama"
}
