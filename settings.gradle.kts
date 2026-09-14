pluginManagement {
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
    }
}
rootProject.name = "MoovAR"
include(":app")
include(":core:database", ":core:network", ":core:domain", ":core:common")
include(":feature:home", ":feature:departures", ":feature:journey", ":feature:alerts", ":feature:map", ":feature:favorites")
