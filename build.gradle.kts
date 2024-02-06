plugins {
    `java-library`
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotest.multiplatform)
    alias(libs.plugins.detekt)
    `maven-publish`
}

group = "cz.smarteon.loxone"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}

detekt {
    source.from("src/commonMain/kotlin")
    toolVersion = libs.versions.detekt.get()
    config.setFrom(file("detekt.yml"))
    buildUponDefaultConfig = true
}

val ktor_version = "2.3.7"


kotlin {
    jvm {
        jvmToolchain(17)
        compilations.all {
            kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
            java {
                targetCompatibility = JavaVersion.VERSION_11
            }
        }
        withJava()
        testRuns.named("test") {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
    }
    js {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
            testTask {
                // enabling needs headless browser, skipped for now
                enabled = false
            }
        }
    }
    linuxArm64()
    linuxX64()
//    mingwX64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("reflect"))

                implementation("io.ktor:ktor-client-core:$ktor_version")
                implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
                implementation("io.ktor:ktor-client-websockets:$ktor_version")

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
                implementation(libs.buffer)

                implementation(libs.kotlincrypto.sha1)
                implementation(libs.kotlincrypto.sha2)
                implementation(libs.kotlincrypto.hmacsha1)
                implementation(libs.kotlincrypto.hmacsha2)

                implementation(libs.stately.collections)
                implementation(libs.kotlinx.datetime)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.framework.engine)
                implementation(libs.kotest.framework.datatest)
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio:$ktor_version")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.runner.junit5)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:$ktor_version")
            }
        }
        val linuxMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio:$ktor_version")
            }
        }
//        val mingwMain by getting {
//            dependencies {
//                implementation("io.ktor:ktor-client-winhttp:$ktor_version")
//            }
//        }
    }
}
