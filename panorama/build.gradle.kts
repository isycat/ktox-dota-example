plugins {
    kotlin("jvm")
    id("com.isycat.ktox-panorama")
}

dependencies {
    val dotaPanoramaTypesVersion: String by project
    implementation("com.isycat.dota:panorama-types:$dotaPanoramaTypesVersion")
}

ktoxPanorama {
    rootNamespace = "com.isycat.dotaaddon.panorama"
}
