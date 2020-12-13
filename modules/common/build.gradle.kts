dependencies {
    api("io.ktor:ktor-network")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core")
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask> {
    this.dokkaSourceSets.configureEach {
        includes.from("protocol.md")
    }
}
