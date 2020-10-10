pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    val kotlinVersion: String by settings
    val dokkaVersion: String by settings
    val gitAndroidVersion: String by settings
    val ktlintVersion: String by settings
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("org.jetbrains.kotlin"))
                useVersion(kotlinVersion)
        }
    }
    plugins {
        id("org.jetbrains.dokka") version dokkaVersion
        id("com.gladed.androidgitversion") version gitAndroidVersion
        id("org.jlleitschuh.gradle.ktlint") version ktlintVersion
    }
}

rootProject.name = "kotlin-jvm"

val kotlinProjects = listOf(
    "lib"
)

fun subproject(name: String) {
    include(":${rootProject.name}-$name")
    project(":${rootProject.name}-$name").projectDir = file("subprojects/$name")
}

subproject("bom")
kotlinProjects.forEach { subproject(it) }

gradle.allprojects {
    extra["kotlinProjects"] = kotlinProjects
}
