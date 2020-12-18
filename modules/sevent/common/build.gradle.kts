import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("kapt")
}

dependencies {
    api(project(":common"))

    compileOnly("com.google.auto.service:auto-service-annotations")
    kapt("com.google.auto.service:auto-service")
}

tasks.withType<DokkaTask> {
    dokkaSourceSets.configureEach {
        includes.from("README.md")
        includes.from("protocol.md")
    }
}
