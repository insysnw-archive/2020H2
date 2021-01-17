val jarTasks = mapOf(
        "server" to "Server",
        "client" to "Client"
)

plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    implementation(files("libs/java-json.jar"))
}

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

tasks.test {
    useJUnitPlatform()
}
