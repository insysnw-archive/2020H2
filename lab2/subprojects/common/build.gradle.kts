dependencies {
    fun kotlinx(name: String) = "org.jetbrains.kotlinx:kotlinx-$name"

    api("io.ktor:ktor-network")
    api(kotlinx("datetime:0.1.0"))
    api(kotlinx("coroutines-core"))
    api(kotlinx("cli:0.3"))
}
