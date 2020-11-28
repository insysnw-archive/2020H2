@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jlleitschuh.gradle.ktlint.KtlintPlugin
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    base
    kotlin("jvm") apply false
    id("org.jlleitschuh.gradle.ktlint") apply false
    jacoco
}

val groupString = "com.github.antoshka77.inet"
val versionString = "0.0.1"

allprojects {
    group = groupString
    version = versionString

    repositories {
        jcenter()
        maven(url = "https://kotlin.bintray.com/kotlinx/")
    }
}

val kotlinProjects: List<String> by extra

val platformVersion: String by project

fun KotlinSourceSet.collectSources(): Iterable<File> {
    return kotlin.srcDirs.filter { it.exists() } + dependsOn.flatMap { it.collectSources() }
}

fun KotlinSourceSet.collectSourceFiles(): ConfigurableFileCollection {
    return files(collectSources().map { fileTree(it) })
}

fun Project.kotlinProject() {
    apply<KotlinPluginWrapper>()
    apply<MavenPublishPlugin>()
    apply<KtlintPlugin>()

    configure<KotlinJvmProjectExtension> {
        target.compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        sourceSets.all {
            with(languageSettings) {
                useExperimentalAnnotation("io.ktor.util.KtorExperimentalAPI")
                useExperimentalAnnotation("kotlin.time.ExperimentalTime")
                useExperimentalAnnotation("kotlinx.serialization.InternalSerializationApi")
                useExperimentalAnnotation("kotlinx.serialization.ExperimentalSerializationApi")
            }
        }
    }

    val implementation by configurations.getting
    val runtimeOnly by configurations.getting
    val testImplementation by configurations.getting
    val testRuntimeOnly by configurations.getting

    dependencies {
        val ktorPlatform = dependencies.platform("io.ktor:ktor-bom:1.4.2")
        val coroutinesPlatform = dependencies.platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.4.1")
        implementation(ktorPlatform)
        runtimeOnly(ktorPlatform)
        implementation(coroutinesPlatform)
        runtimeOnly(coroutinesPlatform)

        testImplementation(kotlin("test-junit5"))
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    }

    configure<KtlintExtension> {
        version.set("0.39.0")
        verbose.set(true)
        outputToConsole.set(true)
        enableExperimentalRules.set(true)
        outputColorName.set("RED")
        disabledRules.add("no-wildcard-imports")

        reporters {
            reporter(ReporterType.PLAIN)
            reporter(ReporterType.CHECKSTYLE)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

configure<JacocoPluginExtension> {
    toolVersion = "0.8.6"
}

val thisProjects = kotlinProjects.map { project(":$name-$it") }

thisProjects.forEach {
    it.kotlinProject()
}
