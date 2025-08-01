pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")  // <--- agregar esta línea

    }
}

rootProject.name = "EasyFinance"
include(":app")
