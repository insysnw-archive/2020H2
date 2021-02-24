import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Jar

import org.gradle.kotlin.dsl.*

fun Project.setJarMain(main: String) {
    tasks.withType<Jar> {
        manifest {
            attributes["Main-Class"] = main
        }
    }
}

//fun Project.makeFatjar() {
//    tasks.withType<Jar> {
//        dependsOn(configurations.runtimeClasspath)
//        from(
//                {
//                    configurations.runtimeClasspath
//                            .get()
//                            .filter { it.name.endsWith("jar") }
//                            .map { zipTree(it) }
//                }
//        )
//    }
//}