plugins {
    alias(libs.plugins.flixclusive.library)
    `maven-publish`
}

android {
    namespace = "com.flixclusive.core.util"
}

dependencies {
    api(libs.okhttp)

    implementation(libs.core.ktx)
    implementation(libs.gson)
    implementation(libs.jsoup)
    implementation(libs.junit)
    implementation(libs.mockk)
    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.retrofit)
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from("src/main/kotlin")
}


publishing {
    repositories {
        mavenLocal()
    }

    publications {
        create<MavenPublication>("release") {
            groupId = "com.flixclusive"
            artifactId = "core-util"
            version = "1.0.0"
            artifact(sourcesJar)
            artifact("build/outputs/aar/util-release.aar")
        }
    }
}