import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
}

allprojects {

    group = "me.net"
    version = "1.0"


    repositories {
        jcenter()
        maven(url = "https://kotlin.bintray.com/kotlinx/")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

}


