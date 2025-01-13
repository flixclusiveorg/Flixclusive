import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.flixclusive.libs
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import java.io.File

class DestinationsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("com.google.devtools.ksp")
            }

            extensions.configure<BaseExtension> {
                when (this) {
                    is BaseAppModuleExtension -> {
                        applicationVariants.all {
                            addJavaSourceFoldersToModel(
                                File(layout.buildDirectory.asFile.get(), "generated/ksp/$name/kotlin"),
                            )
                        }
                    }

                    is LibraryExtension -> {
                        libraryVariants.all {
                            addJavaSourceFoldersToModel(
                                File(layout.buildDirectory.asFile.get(), "generated/ksp/$name/kotlin"),
                            )
                        }
                    }
                }
            }

            afterEvaluate {
                // Custom task to generate destinations
                val hasValidDestinations =
                    project.parent?.name?.equals("mobile") == true ||
                        project.parent?.name?.equals("tv") == true
                if (hasValidDestinations) {
                    tasks.register("generateDestinations") {
                        dependsOn("kspDebugKotlin")
                        doLast {
                            println("Generated destinations for ${project.name}")
                        }
                    }
                }
            }

            extensions.configure<KspExtension> {
                arg("compose-destinations.moduleName", project.name)
                arg("compose-destinations.mode", "destinations")
            }

            dependencies {
                add("implementation", libs.findLibrary("destinations-core").get())
                add("ksp", libs.findLibrary("destinations-ksp").get())
                add("implementation", libs.findLibrary("destinations-animations").get())
            }
        }
    }
}
