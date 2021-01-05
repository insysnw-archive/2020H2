plugins {
    application
    kotlin("plugin.serialization")
}

application {
    mainClass.set("com.handtruth.net.lab3.sevent.ServerKt")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    implementation("org.jetbrains.kotlinx:kotlinx-cli")
    implementation(project(":sevent-common"))
}
