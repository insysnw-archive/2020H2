import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val jarTasks = mapOf (
        "lab1" to "2020H2-lab1",
        "bclient" to "blocking.Client",
        "bserver" to "blocking.Server",
        "nbclient" to "non_blocking.Client",
        "nbserver" to "non_blocking.Server"
)

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.30"
}

repositories {
    jcenter()
}

sourceSets {
    main {
        java {
            srcDir("src")
        }
        resources.srcDir("/resources")
    }
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

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}