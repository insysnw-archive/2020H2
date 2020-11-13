plugins {
    application
}

application {
    mainClass.set("com.github.antoshka77.schat.ClientCLIKt")
}

dependencies {
    api(project(":schat-client"))
}
