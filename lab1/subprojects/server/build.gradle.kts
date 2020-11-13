plugins {
    application
}

application {
    mainClass.set("com.github.antoshka77.schat.ServerKt")
}

dependencies {
    implementation(project(":schat-common"))
    implementation("io.github.microutils:kotlin-logging:2.0.3")
    implementation("ch.qos.logback:logback-classic:1.2.3")
}
