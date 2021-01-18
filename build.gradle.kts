val jarTasks = mapOf(
        "ntp-server" to "ntpserver.Server",
        "tftp-client" to "tftpclient.Client"
)

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.20"
}

repositories {
    jcenter()
    maven(url = "https://kotlin.bintray.com/kotlinx/")
}

dependencies {
    implementation("io.ktor:ktor-server-netty:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.1")
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