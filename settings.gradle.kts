pluginManagement {
    includeBuild("build-logic") // WARN: Don't remove! This one's important.
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
        mavenLocal()
    }
}

rootProject.name = "Flixclusive"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
include(":app")

// For common ui things such as ViewModels, NavigationArgs etc.
include(":core:ui:film")
include(":core:ui:home")
include(":core:ui:player")
// ===========================================================

// Common features
include(":feature:splash-screen")

// Mobile features
include(":feature:mobile:about")
include(":feature:mobile:markdown")
include(":feature:mobile:crash")
include(":feature:mobile:film")
include(":feature:mobile:genre")
include(":feature:mobile:home")
include(":feature:mobile:player")
include(":feature:mobile:preferences")
include(":feature:mobile:provider-info")
include(":feature:mobile:provider-list")
include(":feature:mobile:provider-settings")
include(":feature:mobile:provider-test")
include(":feature:mobile:recently-watched")
include(":feature:mobile:repository")
include(":feature:mobile:repository-search")
include(":feature:mobile:search")
include(":feature:mobile:search-expanded")
include(":feature:mobile:see-all")
include(":feature:mobile:settings")
include(":feature:mobile:update")
include(":feature:mobile:watchlist")

// TV features
include(":feature:tv:film")
include(":feature:tv:home")
include(":feature:tv:player")
//include(":feature:tv:preferences")
include(":feature:tv:search")

include(":model:configuration")
include(":model:database")
include(":model:datastore")

include(":domain:database")
include(":domain:catalog")
include(":domain:home")
include(":domain:provider")
include(":domain:search")
include(":domain:tmdb")
include(":domain:updater")

include(":data:configuration")
include(":data:network")
include(":data:provider")
include(":data:search_history")
include(":data:tmdb")
include(":data:user")
include(":data:watch_history")
include(":data:watchlist")

include(":core:database")
include(":core:datastore")
include(":core:locale")
include(":core:network")
include(":core:theme")
include(":core:ui:common")
include(":core:ui:mobile")
include(":core:ui:tv")

include(":service")
