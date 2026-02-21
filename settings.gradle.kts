pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
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



include(":codex")
include(":ads")
include(":core-ui")
include(":core")
include(":widget")
