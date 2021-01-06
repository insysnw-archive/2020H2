val jarTasks = mapOf(
        "bclient" to "blocking.Client",
        "bserver" to "blocking.Server",
        "nbclient" to "non_blocking.Client",
        "nbserver" to "non_blocking.Server"
)

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.20"
}

repositories {
    jcenter()
}

sourceSets["main"].java { srcDir("src") }

tasks.withType<Jar> {
    destinationDirectory.set(rootDir)
}

for (taskMainClass in jarTasks) {
    tasks.register<Jar>(taskMainClass.key) {
        from(sourceSets["main"].output) {
            manifest {
                attributes["Main-Class"] = taskMainClass.value
            }
            archiveFileName.set("${taskMainClass.key}.jar")
            from(configurations.compileClasspath.map { config ->
                config.map { if (it.isDirectory) it else zipTree(it) }
            })
        }
    }
}

tasks.register<Jar>("all") {
    dependsOn(jarTasks.keys)
}