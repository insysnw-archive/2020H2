plugins {
    application
}

application {
    mainClass.set("com.github.antoshka77.inet.dns.ServerKt")
}

dependencies {
    fun kotlinx(name: String) = "org.jetbrains.kotlinx:kotlinx-$name"

    implementation("io.github.microutils:kotlin-logging:2.0.3")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation(project(":lab2-common"))
    implementation(kotlinx("datetime:0.1.1"))
}
