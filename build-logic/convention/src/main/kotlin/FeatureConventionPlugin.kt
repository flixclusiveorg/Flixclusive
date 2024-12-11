
import com.android.build.gradle.LibraryExtension
import com.flixclusive.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class FeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("flixclusive.android.library")
                apply("flixclusive.hilt")
            }

            extensions.configure<LibraryExtension> {
                defaultConfig {
                    testInstrumentationRunner =
                        "androidx.test.runner.AndroidJUnitRunner"
                }

                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                        merges += "META-INF/LICENSE.md"
                        merges += "META-INF/LICENSE-notice.md"
                    }
                }
            }


            dependencies {
                add("implementation", project(":core:theme"))

                add("implementation", libs.findLibrary("hilt-navigation").get())
                add("implementation", libs.findLibrary("lifecycle-runtimeCompose").get())
                add("implementation", libs.findLibrary("lifecycle-viewModelCompose").get())
            }

        }
    }

}