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
        maven(url  ="https://android-sdk.is.com/")
        maven(url = "https://jitpack.io")
    }
}
rootProject.name = "Audiofy"
include(":app")



include(":app:codex")
include(":app:ads")
