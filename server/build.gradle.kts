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
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

dependencies {
    implementation(project(":protocol"))
}