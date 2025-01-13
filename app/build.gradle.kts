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
val applicationName: String = libs.versions.applicationName.get()
val appIdFromLib: String = libs.versions.applicationId.get()
val formattedVersion = "$versionMajor.$versionMinor.$versionPatch"

val gitCommitVersionProvider = providers.exec {
    commandLine = "git rev-parse --short HEAD".split(" ")
}

fun Project.getCommitVersion(): String {
    return gitCommitVersionProvider.standardOutput.asText.get().trim()
}

android {
    namespace = appIdFromLib

    defaultConfig {
        applicationId = appIdFromLib
        versionCode = versionMajor * 10000 + versionMinor * 1000 + versionPatch * 100 + versionBuild
        versionName = formattedVersion
        vectorDrawables {
            useSupportLibrary = true
        }

        resValue("string", "build", versionCode.toString())
        resValue("string", "app_name", applicationName)
        resValue("string", "application_id", appIdFromLib)
        resValue("string", "debug_mode", "false")
        resValue("string", "version_name", formattedVersion)
        resValue("string", "commit_version", getCommitVersion())
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"

            resValue("string", "app_name", "$applicationName Debug")
            resValue("string", "application_id", appIdFromLib + applicationIdSuffix)
            resValue("string", "debug_mode", "true")
            resValue("string", "version_name", formattedVersion + versionNameSuffix)
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            merges += "META-INF/LICENSE.md"
            merges += "META-INF/LICENSE-notice.md"
        }
    }
}

dependencies {
    implementation(projects.feature.mobile.crash)
    implementation(projects.feature.mobile.film)
    implementation(projects.feature.mobile.genre)
    implementation(projects.feature.mobile.home)
    implementation(projects.feature.mobile.player)
    implementation(projects.feature.mobile.providerInfo)
    implementation(projects.feature.mobile.providerManage)
    implementation(projects.feature.mobile.providerSettings)
    implementation(projects.feature.mobile.providerTest)
    implementation(projects.feature.mobile.markdown)
    implementation(projects.feature.mobile.library)
    implementation(projects.feature.mobile.repository)
    implementation(projects.feature.mobile.repositoryManage)
    implementation(projects.feature.mobile.search)
    implementation(projects.feature.mobile.searchExpanded)
    implementation(projects.feature.mobile.seeAll)
    implementation(projects.feature.mobile.settings)
    implementation(projects.feature.splashScreen)
    implementation(projects.feature.mobile.update)
    implementation(projects.feature.mobile.profiles)
    implementation(projects.feature.mobile.userAdd)
    implementation(projects.feature.mobile.userEdit)

    implementation(projects.feature.tv.home)
    implementation(projects.feature.tv.search)
    implementation(projects.feature.tv.film)

    implementation(projects.core.ui.mobile)
    implementation(projects.core.ui.tv)

    implementation(projects.data.configuration)
    implementation(projects.data.network)
    implementation(projects.data.watchHistory)
    implementation(projects.data.watchlist)
    implementation(projects.domain.provider)
    implementation(projects.domain.tmdb)
    implementation(projects.domain.user)
    implementation(libs.stubs.model.provider)

    implementation(projects.service)

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
}
