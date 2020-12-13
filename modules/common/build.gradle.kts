dependencies {
    api("io.ktor:ktor-network")
    api("io.ktor:ktor-io")
    api("io.ktor:ktor-test-dispatcher")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core")
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask> {
    dokkaSourceSets.configureEach {
        includes.from("protocol.md")
    }
}
