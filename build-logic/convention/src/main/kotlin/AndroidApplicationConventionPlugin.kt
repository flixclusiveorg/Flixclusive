
import com.android.build.api.dsl.ApplicationExtension
import com.flixclusive.configureKotlinAndroid
import com.flixclusive.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(commonExtension = this)
                defaultConfig.targetSdk = libs.findVersion("compileSdk").get().toString().toInt()
            }

            dependencies {
                val serialization = libs.findLibrary("kotlinx-serialization").get()
                add("implementation", serialization)
            }
        }
    }
}