
import androidx.room.gradle.RoomExtension
import com.android.build.gradle.LibraryExtension
import com.flixclusive.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("androidx.room")
                apply("com.google.devtools.ksp")
            }

            extensions.configure<RoomExtension> {
                // The schemas directory contains a schema file for each version of the Room database.
                // This is required to enable Room auto migrations.
                // See https://developer.android.com/reference/kotlin/androidx/room/AutoMigration.
                schemaDirectory("$projectDir/schemas")
            }

            extensions.configure<LibraryExtension> {
                sourceSets {
                    // Adds exported schema location as test app assets.
                    getByName("androidTest").assets.srcDir("$projectDir/schemas")
                }
            }

            dependencies {
                add("implementation", libs.findLibrary("room.runtime").get())
                add("implementation", libs.findLibrary("room.ktx").get())
                add("androidTestImplementation", libs.findLibrary("room.testing").get())
                add("ksp", libs.findLibrary("room.compiler").get())
            }
        }
    }

}
