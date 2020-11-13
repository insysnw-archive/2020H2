plugins {
    application
}

application {
    mainClass.set("com.github.antoshka77.schat.ClientGUI")
}

dependencies {
    implementation(project(":schat-client"))
    implementation("no.tornado:tornadofx:1.7.20")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx")
}
