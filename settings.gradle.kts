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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea") }
        maven { url = uri("https://artifact.bytedance.com/repository/pangle/") }
        maven { url = uri("https://android-sdk.is.com/") }
        maven {
            url = uri("https://maven.pkg.github.com/ngoxuanhungbk/ls-leansoft-publishing-sdk")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                    ?: "ngoxuanhungbk"
                password = providers.gradleProperty("gpr.key").orNull
                    ?: providers.gradleProperty("LEAN_PUBLISHING_TOKEN").orNull
                    ?: System.getenv("GITHUB_TOKEN")
                    ?: System.getenv("LEAN_PUBLISHING_TOKEN")
                    ?: ""
            }
        }
    }
}

rootProject.name = "ls129-cute-wallpaper"
include(":app")
