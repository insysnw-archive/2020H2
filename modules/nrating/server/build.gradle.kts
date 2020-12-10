plugins {
    application
}

application {
    mainClass.set("com.handtruth.net.lab3.nrating.ServerKt")
}

dependencies {
    implementation(project(":nrating-common"))
}
