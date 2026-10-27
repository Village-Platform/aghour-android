// Fix AGP conflict when both ANDROID_PREFS_ROOT and ANDROID_USER_HOME are set in environment
try {
    val processEnvClass = Class.forName("java.lang.ProcessEnvironment")
    val envField = processEnvClass.getDeclaredField("theEnvironment").apply { isAccessible = true }
    (envField.get(null) as? MutableMap<*, *>)?.remove("ANDROID_PREFS_ROOT")
    val caseInsensitiveEnvField = processEnvClass.getDeclaredField("theCaseInsensitiveEnvironment").apply { isAccessible = true }
    (caseInsensitiveEnvField.get(null) as? MutableMap<*, *>)?.remove("ANDROID_PREFS_ROOT")
} catch (_: Throwable) {
}

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

rootProject.name = "AghourAndroid"
include(":app")
