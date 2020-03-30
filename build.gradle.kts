import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.gladed.androidgitversion")
    kotlin("jvm")
    jacoco
    `maven-publish`
}

androidGitVersion {
    prefix = "v"
}

group = "com.handtruth"
version = androidGitVersion.name()

repositories {
    mavenCentral()
    maven("https://mvn.handtruth.com")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}

dependencies {
    val platformVersion: String by project
    implementation(platform("com.handtruth.internal:platform:$platformVersion"))

    implementation(kotlin("stdlib-jdk8"))

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

jacoco {
    toolVersion = "0.8.5"
    reportsDir = file("$buildDir/customJacocoReportDir")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
    jacocoTestReport {
        reports {
            xml.isEnabled = false
            csv.isEnabled = false
            html.destination = file("$buildDir/jacocoHtml")
        }
    }
}
