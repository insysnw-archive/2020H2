plugins {
    base
    id("com.gladed.androidgitversion")
}

val groupString = "com.handtruth"
val versionString = androidGitVersion.name()

allprojects {
    group = groupString
    version = versionString
}
