package com.flixclusive

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension
) {
    commonExtension.apply {
        buildFeatures.apply {
            compose = true
            viewBinding = true
        }

        if (this is LibraryExtension) {
            packaging {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    merges += "META-INF/LICENSE.md"
                    merges += "META-INF/LICENSE-notice.md"
                }
            }
        }

        dependencies {
            val bom = libs.findLibrary("compose.bom").get()
            val composeRules = libs.findLibrary("compose.rules").get()
            add("implementation", platform(bom))
            add("androidTestImplementation", platform(bom))
            add("ktlintRuleset", composeRules)
        }
    }
}
