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

rootProject.name = "unbed"

include(":apps:android:app")
include(":apps:android:core:model")
include(":apps:android:core:database")
include(":apps:android:domain")

project(":apps:android:app").projectDir = file("apps/android/app")
project(":apps:android:core:model").projectDir = file("apps/android/core/model")
project(":apps:android:core:database").projectDir = file("apps/android/core/database")
project(":apps:android:domain").projectDir = file("apps/android/domain")

