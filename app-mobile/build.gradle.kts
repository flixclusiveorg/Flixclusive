import com.flixclusive.appId
import com.flixclusive.appName
import com.flixclusive.releaseVersionCode
import com.flixclusive.releaseVersionName
import com.flixclusive.versionPreviewCode

plugins {
    alias(libs.plugins.flixclusive.application)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.testing)
}

android {
    namespace = appId

    defaultConfig {
        applicationId = appId
        versionCode = releaseVersionCode
        versionName = releaseVersionName
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"

            resValue("string", "app_name", "DEBUG-$appName")
            buildConfigField("int", "BUILD_TYPE", "0") // 0 for debug
        }

        release {
            isDebuggable = false
            resValue("string", "app_name", appName)
            buildConfigField("int", "BUILD_TYPE", "1") // 1 for stable
        }

        getByName("preview") {
            applicationIdSuffix = ".preview"

            resValue("string", "app_name", "PRE-$appName")
            buildConfigField("int", "BUILD_TYPE", "2") // 2 for preview
        }

        getByName("benchmark") {
            applicationIdSuffix = ".benchmark"

            resValue("string", "app_name", "BENCHMARK-$appName")
            buildConfigField("int", "BUILD_TYPE", "3") // 3 for benchmark
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            merges += "META-INF/LICENSE.md"
            merges += "META-INF/LICENSE-notice.md"
        }
    }
}

/*
* Set the version code and name manually for the debug and preview builds.
* */
androidComponents {
    onVariants { variant ->
        when (variant.buildType) {
            "debug" -> {
                variant.outputs.forEach { output ->
                    output.versionCode.set(1)
                    output.versionName.set("d1")
                }
            }

            "preview" -> {
                variant.outputs.forEach { output ->
                    output.versionCode.set(versionPreviewCode)
                    output.versionName.set("p$versionPreviewCode")
                }
            }

            "benchmark" -> {
                variant.outputs.forEach { output ->
                    output.versionCode.set(1)
                    output.versionName.set("b1")
                }
            }
        }
    }
}

dependencies {
    implementation(projects.feature.mobile.media)
    implementation(projects.feature.mobile.home)
    implementation(projects.feature.mobile.player)
    implementation(projects.feature.mobile.providerAdd)
    implementation(projects.feature.mobile.providerDetails)
    implementation(projects.feature.mobile.providerManage)
    implementation(projects.feature.mobile.providerSettings)
    implementation(projects.feature.mobile.markdown)
    implementation(projects.feature.mobile.libraryDetails)
    implementation(projects.feature.mobile.libraryManage)
    implementation(projects.feature.mobile.repositoryManage)
    implementation(projects.feature.mobile.search)
    implementation(projects.feature.mobile.seeAll)
    implementation(projects.feature.mobile.settings)
    implementation(projects.feature.splashScreen)
    implementation(projects.feature.mobile.appUpdates)
    implementation(projects.feature.mobile.userProfiles)
    implementation(projects.feature.mobile.onboarding)
    implementation(projects.feature.mobile.userAdd)
    implementation(projects.feature.mobile.userEdit)

    implementation(projects.feature.appUpdates)

//    implementation(projects.feature.tv.home)
//    implementation(projects.feature.tv.search)
//    implementation(projects.feature.tv.media)

    implementation(projects.coreCommon)
    implementation(projects.coreDatastore)
    implementation(projects.coreNetwork)
    implementation(projects.coreStrings)
    implementation(projects.coreDatabase)
    implementation(projects.coreDrawables)
    implementation(projects.coreNavigation)
    implementation(projects.corePresentationPlayer)
    implementation(projects.corePresentationCommon)
    implementation(projects.corePresentationMobile)

    implementation(projects.dataDownloads)
    implementation(projects.dataBackup)
    implementation(projects.dataDatabase)
    implementation(projects.dataProvider)
    implementation(projects.domainProvider)

    implementation(libs.stubs.model.media)
    implementation(libs.stubs.model.provider)
    implementation(libs.stubs.util)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.tv.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.core.splashscreen)
    implementation(libs.destinations.bottomSheet)
    implementation(libs.hilt.navigation)
    implementation(libs.lifecycle.runtimeCompose)
    implementation(libs.kotlinx.immutables)

    testImplementation(projects.coreTesting)
}
