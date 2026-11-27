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

rootProject.name = "well-control-free-simulator"
include(":core")
include(":desktopApp")

val enableAndroidProp = providers.gradleProperty("enableAndroid").orNull?.toBooleanStrictOrNull()
val hasLocalSdk = file("local.properties").takeIf { it.exists() }
    ?.readText()
    ?.lines()
    ?.any { it.trim().startsWith("sdk.dir=") } == true
val hasAndroidEnv = !System.getenv("ANDROID_HOME").isNullOrBlank() ||
    !System.getenv("ANDROID_SDK_ROOT").isNullOrBlank()

if (enableAndroidProp == true || (enableAndroidProp == null && (hasLocalSdk || hasAndroidEnv))) {
    include(":androidApp")
}
