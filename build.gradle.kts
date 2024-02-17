plugins {
    `java-library`
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotest.multiplatform)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
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

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.websockets)
                implementation(libs.ktor.client.logging)

                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlin.logging)
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
                implementation(libs.ktor.client.cio)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.runner.junit5)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
        val linuxMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }
//        val mingwMain by getting {
//            dependencies {
//                implementation("io.ktor:ktor-client-winhttp:$ktor_version")
//            }
//        }
    }
}

publishing {
    publications.configureEach {
        if (this is MavenPublication) {
            pom {
                name.set(project.name)
                url.set("https://github.com/Smarteon/loxone-client-kotlin")
                description.set("Kotlin implementation of the Loxone&trade; communication protocol")
                organization {
                    name.set("Smarteon Systems s.r.o")
                    url.set("https://smarteon.cz")
                }
                licenses {
                    license {
                        name.set("3-Clause BSD License")
                        url.set("https://opensource.org/licenses/BSD-3-Clause")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        name.set("Jiří Mikulášek")
                        email.set("jiri.mikulasek@smarteon.cz")
                    }
                    developer {
                        name.set("Tomáš Knotek")
                        email.set("tomas.knotek@smarteon.cz")
                    }
                }
                scm {
                    url.set("git@github.com:Smarteon/loxone-client-kotlin.git")
                    connection.set("scm:git:git@github.com:Smarteon/loxone-client-kotlin.git")
                    tag.set(project.version.toString())
                }
            }
        }
    }
}
