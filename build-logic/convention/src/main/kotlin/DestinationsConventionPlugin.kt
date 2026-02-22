
import com.flixclusive.libs
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class DestinationsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("com.google.devtools.ksp")
            }

            afterEvaluate {
                tasks.register("generateDestinations") {
                    dependsOn("kspDebugKotlin")
                    doLast {
                        println("Generated destinations for ${project.name}")
                    }
                }
            }

            extensions.configure<KspExtension> {
                arg("compose-destinations.moduleName", project.name)
            }

            dependencies {
                add("implementation", libs.findLibrary("destinations-core").get())
                add("ksp", libs.findLibrary("destinations-ksp").get())
//                add("implementation", libs.findLibrary("destinations-animations").get())
            }
        }
    }
}
