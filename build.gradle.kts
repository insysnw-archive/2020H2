@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    base
    id("com.gladed.androidgitversion")
    kotlin("jvm") apply false
    id("org.jetbrains.dokka")
    jacoco
}

androidGitVersion {
    prefix = "v"
}

val groupString = "com.handtruth.net.lab3"
val versionString: String = androidGitVersion.name()

allprojects {
    group = groupString
    version = versionString

    repositories {
        maven("https://mvn.handtruth.com")
        jcenter()
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
    apply<JacocoPlugin>()
    apply<MavenPublishPlugin>()
    apply<DokkaPlugin>()

    configure<PublishingExtension> {
        if (!System.getenv("CI").isNullOrEmpty()) repositories {
            maven {
                url = uri("https://git.handtruth.com/api/v4/projects/${System.getenv("CI_PROJECT_ID")}/packages/maven")
                credentials(HttpHeaderCredentials::class) {
                    name = "Job-Token"
                    value = System.getenv("CI_JOB_TOKEN")!!
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
            }
        }
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }

    configure<KotlinJvmProjectExtension> {
        target.compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }

    val implementation by configurations.getting
    val runtimeOnly by configurations.getting
    val testImplementation by configurations.getting
    val testRuntimeOnly by configurations.getting

    dependencies {
        val handtruthPlatform = dependencies.platform("com.handtruth.internal:platform:$platformVersion")
        implementation(handtruthPlatform)
        runtimeOnly(handtruthPlatform)

        testImplementation(kotlin("test-junit5"))
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    }

    configure<JacocoPluginExtension> {
        toolVersion = "0.8.6"
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

val thisProjects = kotlinProjects.map { project(":$it") }

thisProjects.forEach {
    it.kotlinProject()
}

tasks {
    val mergeTestCoverageReport by creating(JacocoMerge::class) {
        group = "Reporting"
        val pTasks = Callable { thisProjects.map { it.tasks["test"] } }
        dependsOn(pTasks)
        executionData(pTasks)
    }
    val rootTestCoverageReport by creating(JacocoReport::class) {
        dependsOn(mergeTestCoverageReport)
        group = "Reporting"
        description = "Generate Jacoco coverage reports."
        val coverageSourceDirs = thisProjects.map {
            it.tasks.getByName<JacocoReport>("jacocoTestReport").sourceDirectories
        }

        val classFiles = Callable {
            thisProjects.map {
                it.tasks.getByName<JacocoReport>("jacocoTestReport").classDirectories
            }
        }

        classDirectories.setFrom(classFiles)
        sourceDirectories.setFrom(coverageSourceDirs)

        executionData.setFrom(mergeTestCoverageReport)

        reports {
            xml.isEnabled = true
            html.isEnabled = true
        }
    }
    val dokkaHtmlMultiModule by getting(DokkaMultiModuleTask::class)
    val pagesDest = File(projectDir, "public")
    val gitlabPagesCreateDocs by creating(Copy::class) {
        group = "Documentation"
        dependsOn(dokkaHtmlMultiModule)
        from(dokkaHtmlMultiModule)
        into(File(pagesDest, "docs"))
    }
    val gitlabPagesCreate by creating(Copy::class) {
        group = "Reporting"
        dependsOn(gitlabPagesCreateDocs)
        File(projectDir, "pages").listFiles()!!.forEach {
            from(it)
        }
        destinationDir = pagesDest
    }
    val gitlabPagesClear by creating(Delete::class) {
        delete = setOf(pagesDest)
    }
    val clean by getting {
        dependsOn(gitlabPagesClear)
    }
}
