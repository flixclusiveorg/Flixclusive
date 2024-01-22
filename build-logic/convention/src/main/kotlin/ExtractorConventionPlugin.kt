
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class ExtractorConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("flixclusive.android.library")
                apply("flixclusive.testing")
            }

            dependencies {
                add("implementation", project(":extractor:base"))
            }
        }
    }

}