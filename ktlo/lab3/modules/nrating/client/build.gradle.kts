plugins {
    id("org.jetbrains.compose")
}

kotlin.sourceSets.all {
    with(languageSettings) {
        useExperimentalAnnotation("androidx.compose.foundation.layout.ExperimentalLayout")
        useExperimentalAnnotation("kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}

compose.desktop {
    application {
        mainClass = "com.handtruth.net.lab3.nrating.ClientKt"
    }
}

dependencies {
    api(project(":nrating-common"))
    implementation(compose.desktop.currentOs)
}
