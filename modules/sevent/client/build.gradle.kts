plugins {
    application
}

application {
    mainClass.set("com.handtruth.net.lab3.sevent.ClientKt")
}

dependencies {
    implementation(project(":sevent-common"))
}
