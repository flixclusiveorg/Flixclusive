import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "com.flixclusive.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
}

gradlePlugin {
    /**
     * Register convention plugins so they are available in the build scripts of the application
     */
    plugins {
        register("flixclusiveAndroidApplication") {
            id = "flixclusive.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("flixclusiveAndroidLibrary") {
            id = "flixclusive.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("flixclusiveCompose") {
            id = "flixclusive.compose"
            implementationClass = "ComposeConventionPlugin"
        }
        register("flixclusiveHilt") {
            id = "flixclusive.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("flixclusiveRoom") {
            id = "flixclusive.room"
            implementationClass = "RoomConventionPlugin"
        }
        register("flixclusiveFeature") {
            id = "flixclusive.feature.mobile"
            implementationClass = "FeatureMobileConventionPlugin"
        }
        // TODO: Add feature.tv for tv UI specific dependencies
        register("flixclusiveDestinations") {
            id = "flixclusive.destinations"
            implementationClass = "DestinationsConventionPlugin"
        }
        register("flixclusiveTesting") {
            id = "flixclusive.testing"
            implementationClass = "TestingConventionPlugin"
        }
    }
}
