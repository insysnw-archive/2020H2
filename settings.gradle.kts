pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    val gitVersionPlugin: String by settings
    plugins {
        id("com.gladed.androidgitversion") version gitVersionPlugin
    }
}

rootProject.name = "gradle-wrapper"
