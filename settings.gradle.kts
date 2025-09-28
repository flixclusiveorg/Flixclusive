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

// Common features
include(":feature:splash-screen")

// Mobile features
include(":feature:mobile:film")
include(":feature:mobile:home")
include(":feature:mobile:library-common")
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
include(":feature:mobile:repository-manage")
include(":feature:mobile:search")
include(":feature:mobile:search-expanded")
include(":feature:mobile:see-all")
include(":feature:mobile:settings")
include(":feature:mobile:update")
include(":feature:mobile:user-add")
include(":feature:mobile:user-edit")

// TV features
//include(":feature:tv:film")
//include(":feature:tv:home")
//include(":feature:tv:player")
// include(":feature:tv:preferences")
//include(":feature:tv:search")

include(":domain-catalog")
include(":domain-database")
include(":domain-provider")

include(":data-database")
include(":data-provider")
include(":data-tmdb")

include(":core-common")
include(":core-database")
include(":core-datastore")
include(":core-drawables")
include(":core-navigation")
include(":core-network")
include(":core-presentation-common")
include(":core-presentation-mobile")
include(":core-presentation-player")
//include(":core-presentation-tv")
include(":core-strings")
include(":core-testing")

include(":service")


/**
 * This is to flatten the modules with sub-modules
 */
rootProject.children.forEach { project ->
    when {
        project.name.startsWith("data-") -> {
            val rawProjectName = project.name.removePrefix("data-")
            project.projectDir = file("data/$rawProjectName")
        }
        project.name.startsWith("domain-") -> {
            val rawProjectName = project.name.removePrefix("domain-")
            project.projectDir = file("domain/$rawProjectName")
        }
        project.name.startsWith("core-") -> {
            val rawProjectName = project.name.removePrefix("core-")
                .replace("-", "/") // For modules like core-presentation-mobile -> core/presentation/mobile
            project.projectDir = file("core/$rawProjectName")
        }
        // TODO: Support other modules with sub-modules
    }
}
