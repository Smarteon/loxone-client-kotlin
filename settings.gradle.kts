pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "dev.detekt") {
                useModule("dev.detekt:detekt-gradle-plugin:${requested.version}")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "loxone-client-kotlin"

includeBuild("examples/kotlin")
includeBuild("examples/java")
