pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "MatrixSynapseManager"

include(":app")
include(":core:database")
include(":core:model")
include(":core:network")
include(":core:security")
include(":core:testing")
include(":feature:auth")
include(":feature:devices")
include(":feature:servers")
include(":feature:settings")
include(":feature:users")
include(":feature:rooms")
include(":feature:stats")
include(":feature:media")
include(":feature:federation")
include(":feature:jobs")
include(":feature:moderation")
