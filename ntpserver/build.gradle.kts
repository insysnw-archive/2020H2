plugins {
    application
    java
    kotlin("jvm")
}

application {
    mainClassName = "ServerKt"
}

setJarMain(application.mainClassName)

tasks.withType<Jar> {
    dependsOn(configurations.runtimeClasspath)
    from(
            {
                configurations.runtimeClasspath
                        .get()
                        .filter { it.name.endsWith("jar") }
                        .map { zipTree(it) }
            }
    )
}

dependencies {
    implementation("io.ktor:ktor-network:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.1")
}