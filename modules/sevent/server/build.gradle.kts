plugins {
    application
}

application {
    mainClass.set("com.handtruth.net.lab3.sevent.ServerKt")
}

dependencies {
    implementation(project(":sevent-common"))
}
