import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("kapt")
}

dependencies {
    api("io.ktor:ktor-network")
    api("io.ktor:ktor-io")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    api("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")

    api("com.handtruth.kommon:kommon-log")

    implementation(kotlin("reflect"))

    runtimeOnly("ch.qos.logback:logback-classic")

    testCompileOnly("com.google.auto.service:auto-service-annotations")
    kaptTest("com.google.auto.service:auto-service")
}

tasks.withType<DokkaTask> {
    dokkaSourceSets.configureEach {
        includes.from("README.md")
        includes.from("protocol.md")
        samples.from("src/samples")
    }
}
