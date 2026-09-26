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
        exclusiveContent {
            forRepository {
                mavenLocal()
            }
            filter {
                includeGroup("net.mamby.androidkit")
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "Local Events"
include(":app")
