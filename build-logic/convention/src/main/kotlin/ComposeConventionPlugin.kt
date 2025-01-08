
import com.android.build.gradle.BaseExtension
import com.flixclusive.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            val extension = extensions.getByType<BaseExtension>()
            configureAndroidCompose(extension)
        }
    }
}
