plugins {
    kotlin("kapt")
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("com.github.antoshka77.inet.dns.ClientKt")
}

dependencies {
    fun kotlinx(name: String) = "org.jetbrains.kotlinx:kotlinx-$name"

    implementation(project(":lab2-common"))
    implementation(kotlin("reflect"))
    implementation(kotlinx("serialization-json:1.0.1"))
    compileOnly("com.google.auto.service:auto-service-annotations:1.0-rc7")
    kapt("com.google.auto.service:auto-service:1.0-rc7")
}
