import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("kapt")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":common"))
    api("org.jetbrains.kotlinx:kotlinx-serialization-core")

    compileOnly("com.google.auto.service:auto-service-annotations")
    kapt("com.google.auto.service:auto-service")
}

tasks.withType<DokkaTask> {
    dokkaSourceSets.configureEach {
        includes.from("README.md")
        includes.from("protocol.md")
    }
}
