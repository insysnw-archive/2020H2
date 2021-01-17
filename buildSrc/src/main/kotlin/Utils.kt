import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

import org.gradle.kotlin.dsl.*

fun Project.setJarMain(main: String){
    tasks.withType<Jar> {
        manifest {
            attributes["Main-Class"] = main
        }
    }
}