import com.isycat.ktox.panorama.CompileDotaXmlKtsTask

plugins {
    kotlin("jvm")
    id("com.isycat.ktox-panorama")
}

val ktoxSuiteVersion: String by project
val ktoxDotaVersion: String by project
val kotlinVersion: String by project

dependencies {
    compileOnly("com.isycat:ktox-js:$ktoxSuiteVersion")
    compileOnly("com.isycat:ktox-dota-lib:$ktoxDotaVersion")
    compileOnly("com.isycat.dota:panorama-types:$ktoxDotaVersion")
    // @PanoramaView (panorama-layout-kts) is added compileOnly by the ktox-panorama plugin,
    // but its own compileOnly deps are not transitive — a module that *declares* a @PanoramaView
    // class needs ktox-core (the @KtTranspilerOverride meta-annotation) and the Kotlin compiler
    // (PSI types that annotation references) on its own compile classpath.
    compileOnly("com.isycat:ktox-core:$ktoxSuiteVersion")
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
    compileOnly(project(":shared"))
}

ktoxPanorama {
    rootNamespace = "com.isycat.dotaaddon.panorama"
}

// Make this module's compiled @PanoramaView classes (e.g. WaveStatsPanel) and their compile
// dependencies (panorama-types' Panel/Label) resolvable inside the .dota.xml.kts layout
// scripts that instantiate them.
tasks.withType<CompileDotaXmlKtsTask>().configureEach {
    scriptExtraClasspath.from(tasks.named("jar"))
    scriptExtraClasspath.from(configurations["compileClasspath"])
    dependsOn("jar")
}
