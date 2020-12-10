plugins {
    application
}

application {
    mainClass.set("com.handtruth.net.lab3.nrating.ClientKt")
}

dependencies {
    implementation(project(":nrating-common"))
}
