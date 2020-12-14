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

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask> {
    dokkaSourceSets.configureEach {
        includes.from("protocol.md")
        includes.from("README.md")
        samples.from("src/samples")
    }
}
