plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_18
    targetCompatibility = JavaVersion.VERSION_18
}

dependencies {
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // JSoup
    implementation("org.jsoup:jsoup:1.16.1")

    // Json
    implementation("org.json:json:20230618")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
}
