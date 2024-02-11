plugins {
    alias(libs.plugins.flixclusive.library)
    alias(libs.plugins.flixclusive.testing)
    `maven-publish`
}

android {
    namespace = "com.flixclusive.provider.base"
}

dependencies {
    api(libs.gson)
    api(libs.jsoup)
    api(libs.okhttp)
    api(projects.core.util)
    api(projects.model.provider)

    implementation(libs.coroutines.test)
    implementation(libs.junit)
    implementation(libs.mockk)
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
