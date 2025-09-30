import com.flixclusive.getCommitCount

plugins {
    alias(libs.plugins.flixclusive.application)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.testing)
}

// Version
val versionMajor = 2
val versionMinor = 2
val versionPatch = 0
val versionBuild = 0
val appName = "Flixclusive"
val appId = "com.flixclusive"
val semanticVersion = "$versionMajor.$versionMinor.$versionPatch"

val commitCount by lazy { getCommitCount() }
val previewVersionCode by lazy { "p$commitCount" }
val debugVersionCode by lazy { "d$commitCount" }

android {
    namespace = appId

    defaultConfig {
        applicationId = appId
        versionCode = versionMajor * 10000 + versionMinor * 1000 + versionPatch * 100 + versionBuild
        versionName = semanticVersion
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
            resValue("string", "app_name", appName)
            buildConfigField("int", "BUILD_TYPE", "1") // 1 for stable
        }

        create("preview") {
            applicationIdSuffix = ".preview"

            resValue("string", "app_name", "PRE-$appName")
            buildConfigField("int", "BUILD_TYPE", "2") // 2 for preview
        }
    }

    buildFeatures {
        buildConfig = true
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
                    output.versionCode.set(commitCount.toInt())
                    output.versionName.set(debugVersionCode)
                }
            }

            "preview" -> {
                variant.outputs.forEach { output ->
                    output.versionCode.set(commitCount.toInt())
                    output.versionName.set(previewVersionCode)
                }
            }
        }
    }
}

dependencies {
    implementation(projects.feature.mobile.film)
    implementation(projects.feature.mobile.home)
    implementation(projects.feature.mobile.player)
    implementation(projects.feature.mobile.providerAdd)
    implementation(projects.feature.mobile.providerDetails)
    implementation(projects.feature.mobile.providerManage)
    implementation(projects.feature.mobile.providerSettings)
    implementation(projects.feature.mobile.providerTest)
    implementation(projects.feature.mobile.markdown)
    implementation(projects.feature.mobile.libraryDetails)
    implementation(projects.feature.mobile.libraryManage)
    implementation(projects.feature.mobile.repositoryManage)
    implementation(projects.feature.mobile.search)
    implementation(projects.feature.mobile.searchExpanded)
    implementation(projects.feature.mobile.seeAll)
    implementation(projects.feature.mobile.settings)
    implementation(projects.feature.splashScreen)
    implementation(projects.feature.mobile.appUpdates)
    implementation(projects.feature.mobile.profiles)
    implementation(projects.feature.mobile.userAdd)
    implementation(projects.feature.mobile.userEdit)

    implementation(projects.feature.appUpdates)

//    implementation(projects.feature.tv.home)
//    implementation(projects.feature.tv.search)
//    implementation(projects.feature.tv.film)

    implementation(projects.coreCommon)
    implementation(projects.coreDatastore)
    implementation(projects.coreNetwork)
    implementation(projects.coreStrings)
    implementation(projects.coreDatabase)
    implementation(projects.coreDrawables)
    implementation(projects.coreNavigation)
    implementation(projects.corePresentationCommon)
    implementation(projects.corePresentationMobile)

    implementation(projects.dataDownloads)
    implementation(projects.dataDatabase)
    implementation(projects.dataProvider)
    implementation(projects.domainProvider)

    implementation(libs.stubs.model.film)
    implementation(libs.stubs.model.provider)
    implementation(libs.stubs.util)

    implementation(libs.accompanist.navigation.animation)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.tv.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.constraintlayout.compose)
    implementation(libs.core.splashscreen)
    implementation(libs.hilt.navigation)
    implementation(libs.lifecycle.runtimeCompose)
    implementation(libs.kotlinx.immutables)

    testImplementation(projects.coreTesting)
}
