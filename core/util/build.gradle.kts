plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.hilt)
    alias(libs.plugins.flixclusive.compose)
    `maven-publish`
}

android {
    namespace = "com.flixclusive.core.util"
}

dependencies {
    api(libs.okhttp)

    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.gson)
    implementation(libs.jsoup)
    implementation(libs.okhttp.dnsoverhttps)
    implementation(libs.retrofit)
}

afterEvaluate {
    publishing {
        repositories {
            mavenLocal()

            val token = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")

            if (token != null) {
                maven {
                    credentials {
                        username = "rhenwinch"
                        password = token
                    }
                    setUrl("https://maven.pkg.github.com/rhenwinch/Flixclusive")
                }
            }
        }
    }
}