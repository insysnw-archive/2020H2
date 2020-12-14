plugins {
    kotlin("kapt")
}

dependencies {
    api("io.ktor:ktor-network")
    api("io.ktor:ktor-io")
    api("io.ktor:ktor-test-dispatcher")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core")

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
