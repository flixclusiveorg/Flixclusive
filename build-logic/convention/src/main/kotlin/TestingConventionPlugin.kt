
import com.android.build.gradle.BaseExtension
import com.flixclusive.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class TestingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("org.jetbrains.kotlin.android")
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            extensions.configure<BaseExtension> {
                defaultConfig {
                    testInstrumentationRunner =
                        "androidx.test.runner.AndroidJUnitRunner"
                }

                packagingOptions {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                        excludes += "META-INF/INDEX.LIST"
                        excludes += "META-INF/DEPENDENCIES"
                        merges += "META-INF/LICENSE.md"
                        merges += "META-INF/LICENSE-notice.md"
                    }
                }
            }

            dependencies {
                add("testImplementation", libs.findLibrary("coroutines.test").get())
                add("testImplementation", libs.findLibrary("junit").get())
                add("testImplementation", libs.findLibrary("mockk").get())
                add("testImplementation", libs.findLibrary("turbine").get())
                add("testImplementation", libs.findLibrary("strikt").get())

                add("androidTestImplementation", libs.findLibrary("androidx.test.ext.junit").get())
                add("androidTestImplementation", libs.findLibrary("test.rules").get())
                add("androidTestImplementation", libs.findLibrary("test.core").get())
                add("androidTestImplementation", libs.findLibrary("test.core.ktx").get())
                add("androidTestImplementation", libs.findLibrary("test.rules").get())
                add("androidTestImplementation", libs.findLibrary("test.runner").get())
                add("androidTestImplementation", libs.findLibrary("coroutines.test").get())
                add("androidTestImplementation", libs.findLibrary("espresso.core").get())
                add("androidTestImplementation", libs.findLibrary("mockk-android").get())
                add("androidTestImplementation", libs.findLibrary("turbine").get())
                add("androidTestImplementation", libs.findLibrary("strikt").get())
            }

        }
    }

}
