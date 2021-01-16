val jarTasks = mapOf(
        "server" to "server.Server",
        "client" to "client.Client"
)

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.20"
}

repositories {
    jcenter()
}

dependencies {
    implementation("com.beust", "klaxon", "5.4")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.20")
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