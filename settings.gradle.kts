pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    val kotlinVersion: String by settings
    val dokkaVersion: String by settings
    val gitAndroidVersion: String by settings
    val atomicfuVersion: String by settings
    resolutionStrategy {
        eachPlugin {
            when {
                requested.id.id.startsWith("org.jetbrains.kotlin") ->
                    useVersion(kotlinVersion)
                requested.id.id == "kotlinx-atomicfu" ->
                    useModule("org.jetbrains.kotlinx:atomicfu-gradle-plugin:$atomicfuVersion")
            }
        }
    }
    plugins {
        id("org.jetbrains.dokka") version dokkaVersion
        id("com.gladed.androidgitversion") version gitAndroidVersion
    }
}

rootProject.name = "net-lab3"

val kotlinProjects = listOf(
    "common",
    "nrating-common",
    "nrating-client",
    "nrating-server",
    "sevent-common",
    "sevent-client",
    "sevent-server"
)

fun subproject(name: String) {
    val pName = ":$name"
    include(pName)
    project(pName).projectDir = file("modules/$name")
}

fun subproject(name: String, category: String) {
    val pName = ":$name-$category"
    include(pName)
    project(pName).projectDir = file("modules/$name/$category")
}

subproject("bom")

kotlinProjects.forEach {
    val list = it.split('-').toTypedArray()
    if (list.size == 2) {
        subproject(list[0], list[1])
    } else {
        subproject(it)
    }
}

gradle.allprojects {
    extra["kotlinProjects"] = kotlinProjects
}
