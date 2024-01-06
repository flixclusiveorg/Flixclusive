pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Flixclusive"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
include(":app")

include(":feature:mobile:home")

include(":model:tmdb")
include(":model:configuration")
include(":model:datastore")
include(":model:provider")
include(":model:database")

include(":domain:database")
include(":domain:home")
include(":domain:provider")
include(":domain:tmdb")

include(":data:configuration")
include(":data:provider")
include(":data:tmdb")
include(":data:user")
include(":data:util")
include(":data:watch_history")
include(":data:watchlist")

include(":extractor:base")
include(":extractor:mixdrop")
include(":extractor:upcloud")

include(":provider:base")
include(":provider:flixhq")
include(":provider:lookmovie")
include(":provider:superstream")

include(":core:util")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:theme")
include(":core:ui")
