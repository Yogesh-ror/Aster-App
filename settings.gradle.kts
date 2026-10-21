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

rootProject.name = "Aster AI Chatbot Workspace"

include(":outputs:ChatFusion:android:app")
include(":outputs:ChatFusion:backend")

findProject(":outputs:ChatFusion:android:app")?.projectDir = file("outputs/ChatFusion/android/app")
findProject(":outputs:ChatFusion:backend")?.projectDir = file("outputs/ChatFusion/backend")
