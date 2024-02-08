@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.flixclusive.application)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.destinations)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.testing)
}

// Version
val versionMajor = 1
val versionMinor = 4
val versionPatch = 0
val versionBuild = 0
val applicationName: String = libs.versions.applicationName.get()
val _applicationId: String = libs.versions.applicationId.get()
val _versionName = "${versionMajor}.${versionMinor}.${versionPatch}"


android {
    namespace = _applicationId

    defaultConfig {
        applicationId = _applicationId
        versionCode = versionMajor * 10000 + versionMinor * 1000 + versionPatch * 100 + versionBuild
        versionName = _versionName
        vectorDrawables {
            useSupportLibrary = true
        }

        resValue("string", "build", versionCode.toString())
        resValue("string", "app_name", applicationName)
        resValue("string", "application_id", _applicationId)
        resValue("string", "debug_mode", "false")
        resValue("string", "version_name", _versionName)
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("prerelease") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            isShrinkResources = false

            applicationIdSuffix = ".pre_release"
            versionNameSuffix = "-PRE_RELEASE"

            resValue("string", "app_name", "$applicationName Pre-Release")
            resValue("string", "application_id", _applicationId + applicationIdSuffix)
            resValue("string", "version_name", _versionName + versionNameSuffix)
        }

        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"

            resValue("string", "app_name", "$applicationName Debug")
            resValue("string", "application_id", _applicationId + applicationIdSuffix)
            resValue("string", "debug_mode", "true")
            resValue("string", "version_name", _versionName + versionNameSuffix)
        }
    }

    testOptions {
        unitTests.all {
            it.ignoreFailures = true
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
    implementation(projects.feature.mobile.about)
    implementation(projects.feature.mobile.crash)
    implementation(projects.feature.mobile.film)
    implementation(projects.feature.mobile.genre)
    implementation(projects.feature.mobile.home)
    implementation(projects.feature.mobile.player)
    implementation(projects.feature.mobile.preferences)
    implementation(projects.feature.mobile.provider)
    implementation(projects.feature.mobile.recentlyWatched)
    implementation(projects.feature.mobile.search)
    implementation(projects.feature.mobile.searchExpanded)
    implementation(projects.feature.mobile.seeAll)
    implementation(projects.feature.mobile.settings)
    implementation(projects.feature.splashScreen)
    implementation(projects.feature.mobile.update)
    implementation(projects.feature.mobile.watchlist)

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
    implementation(projects.model.provider)

    implementation(projects.service)

    implementation(libs.accompanist.navigation.animation)
    implementation(libs.coil.compose)
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
    implementation(libs.material)
}

tasks.register("androidSourcesJar", Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets.getByName("main").java.srcDirs) // Full Sources
}

// For GradLew Plugin
tasks.register("makeJar", Copy::class) {
    from("build/intermediates/compile_app_classes_jar/prerelease")
    into("build")
    include("classes.jar")
}