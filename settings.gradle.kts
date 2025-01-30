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
include(":feature:mobile:crash")
include(":feature:mobile:film")
include(":feature:mobile:genre")
include(":feature:mobile:home")
include(":feature:mobile:library-details")
include(":feature:mobile:library-manage")
include(":feature:mobile:markdown")
include(":feature:mobile:player")
include(":feature:mobile:profiles")
include(":feature:mobile:provider-add")
include(":feature:mobile:provider-details")
include(":feature:mobile:provider-manage")
include(":feature:mobile:provider-settings")
include(":feature:mobile:provider-test")
include(":feature:mobile:repository-details")
include(":feature:mobile:repository-manage")
include(":feature:mobile:search")
include(":feature:mobile:search-expanded")
include(":feature:mobile:see-all")
include(":feature:mobile:settings")
include(":feature:mobile:update")
include(":feature:mobile:user-add")
include(":feature:mobile:user-edit")

// TV features
include(":feature:tv:film")
include(":feature:tv:home")
include(":feature:tv:player")
// include(":feature:tv:preferences")
include(":feature:tv:search")

include(":model:configuration")
include(":model:database")
include(":model:datastore")

include(":domain:catalog")
include(":domain:home")
include(":domain:library-recent")
include(":domain:library-watchlist")
include(":domain:provider")
include(":domain:tmdb")
include(":domain:updater")
include(":domain:user")

include(":data:configuration")
include(":data:library-custom")
include(":data:library-recent")
include(":data:library-watchlist")
include(":data:network")
include(":data:provider")
include(":data:search")
include(":data:tmdb")
include(":data:user")

include(":core:database")
include(":core:datastore")
include(":core:locale")
include(":core:network")
include(":core:theme")
include(":core:ui:common")
include(":core:ui:mobile")
include(":core:ui:tv")

include(":service")
