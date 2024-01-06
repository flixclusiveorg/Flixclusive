plugins {
    alias(libs.plugins.flixclusive.application)
    alias(libs.plugins.flixclusive.compose)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.room)
}

// Version
val versionMajor = 1
val versionMinor = 4
val versionPatch = 0
val versionBuild = 0
val applicationName = libs.versions.applicationName.get()

android {
    namespace = "com.flixclusive"

    defaultConfig {
        applicationId = "com.flixclusive"
        versionCode = versionMajor * 10000 + versionMinor * 1000 + versionPatch * 100 + versionBuild
        versionName = "${versionMajor}.${versionMinor}.${versionPatch}"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources  = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            resValue("string", "app_name", applicationName)
        }

        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            resValue("string", "app_name", applicationName + " Debug")
        }
    }

    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    val media3 = "1.2.0"
    val retrofit2 = "2.9.0"
    val composeDestination = "1.9.53"
    val dataStore = "1.0.0"
    val gson = "2.10.1"
    val coil = "2.4.0"
    val jUnit4 = "4.13.2"
    val mock = "1.13.8"
    val coroutinesTest = "1.7.3"
    val androidXTestCore = "1.5.0"
    val androidXTestRunner = "1.5.2"
    val androidXTestRules = "1.5.0"

    // Core KTX
    implementation(libs.core.ktx)

    // Splash Screen
    implementation(libs.core.splashscreen)

    // Datastore
    implementation ("androidx.datastore:datastore-preferences:$dataStore")

    // Exoplayer
    implementation ("androidx.media3:media3-session:$media3")
    implementation ("androidx.media3:media3-exoplayer:$media3")
    implementation ("androidx.media3:media3-exoplayer-hls:$media3")
    implementation ("androidx.media3:media3-ui:$media3")
    implementation ("androidx.media3:media3-ui-leanback:$media3")
    implementation ("androidx.media3:media3-cast:$media3")
    implementation ("androidx.media3:media3-common:$media3")
    implementation ("androidx.media3:media3-datasource-okhttp:$media3")

    // Powerful URI provider
    implementation ("com.github.seven332:unifile:1.0.0")

    // Retrofit2
    implementation ("com.squareup.retrofit2:retrofit:$retrofit2")
    implementation ("com.squareup.retrofit2:converter-gson:$retrofit2")
    implementation ("com.squareup.retrofit2:converter-scalars:$retrofit2")

    // Gson
    implementation ("com.google.code.gson:gson:$gson")

    // Coil
    implementation ("io.coil-kt:coil-compose:$coil")

    // Compose Destinations by raamcosta
    implementation ("io.github.raamcosta.compose-destinations:animations-core:$composeDestination")
    ksp ("io.github.raamcosta.compose-destinations:ksp:$composeDestination")

    // Jsoup - for testing only
    implementation ("org.jsoup:jsoup:1.16.1")

    coreLibraryDesugaring ("com.android.tools:desugar_jdk_libs:2.0.4")

    // Test dependencies
    androidTestImplementation ("androidx.test:core:$androidXTestCore")
    androidTestImplementation ("androidx.test:core-ktx:$androidXTestCore")
    androidTestImplementation ("androidx.test:runner:$androidXTestRunner")
    androidTestImplementation ("androidx.test:rules:$androidXTestRules")

    androidTestImplementation ("junit:junit:$jUnit4")
    androidTestImplementation ("io.mockk:mockk:$mock")
    testImplementation ("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesTest")
}
