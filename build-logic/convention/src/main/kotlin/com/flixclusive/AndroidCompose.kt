package com.flixclusive

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureAndroidCompose(commonExtension: BaseExtension) {
    commonExtension.apply {
        buildFeatures.apply {
            compose = true
            viewBinding = true
        }

        packagingOptions {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
                merges += "META-INF/LICENSE.md"
                merges += "META-INF/LICENSE-notice.md"
            }
        }

        dependencies {
            val bom = libs.findLibrary("compose.bom").get()
            val composeRules = libs.findLibrary("compose.rules").get()
            add("implementation", platform(bom))
            add("androidTestImplementation", platform(bom))
            add("androidTestImplementation", platform(bom))
            add("ktlintRuleset", composeRules)
        }
    }
}
