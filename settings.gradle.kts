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
        maven(url = "https://jitpack.io")
        maven(url  ="https://android-sdk.is.com/")
    }
}
rootProject.name = "Audiofy"
include(":app")



include(":app:codex")
include(":app:ads")
