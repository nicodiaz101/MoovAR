#!/bin/bash
mkdir -p app/src/main/java/com/transportar/android
mkdir -p core/database core/network core/domain core/common
mkdir -p feature/home feature/departures feature/journey feature/alerts feature/map feature/favorites

# Create build.gradle.kts for all modules
touch app/build.gradle.kts
for module in database network domain common; do
  touch core/$module/build.gradle.kts
done
for feature in home departures journey alerts map favorites; do
  touch feature/$feature/build.gradle.kts
done

# Create settings.gradle.kts
cat << 'SETTINGSEOF' > settings.gradle.kts
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
SETTINGSEOF
