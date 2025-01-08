package com.flixclusive

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

@Suppress("UnstableApiUsage")
internal fun Project.configureAndroidCompose(commonExtension: BaseExtension) {
    commonExtension.apply {
        buildFeatures.apply {
            compose = true
            viewBinding = true
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
