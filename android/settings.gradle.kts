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
        // Official Nexconn Chat/Call SDK repository (public, no credentials).
        maven("https://maven.nexconn.ai/repository/maven-releases/") {
            content {
                includeGroup("ai.nexconn.chat")
            }
        }
    }
}

rootProject.name = "momento"

include(":app")
include(":core:common")
include(":core:designsystem")
include(":core:navigation")
include(":core:networking")
include(":core:database")
include(":core:datastore")
include(":domain")
include(":data")
include(":feature:auth")
include(":feature:home")
include(":feature:profile")
include(":feature:discover")
include(":feature:pod")
include(":feature:create")
