plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.neogradle)
    idea
}

kotlin {
    jvmToolchain(21)
}

repositories {
//    mavenCentral()
    google() // Compose repo
}

dependencies {
    api(libs.compose.runtime)
}

tasks.withType<Jar> {
    manifest.attributes("FMLModType" to "GAMELIBRARY")
}

configurations {
    runtimeElements {
        setExtendsFrom(emptySet())
    }
}
