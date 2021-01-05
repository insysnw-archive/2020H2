plugins {
    kotlin("kapt")
}

dependencies {
    api(project(":common"))

    compileOnly("com.google.auto.service:auto-service-annotations")
    kapt("com.google.auto.service:auto-service")
}
