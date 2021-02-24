plugins {
    application
    java
    kotlin("jvm")
}

application {
    mainClassName = "ClientKt"
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
