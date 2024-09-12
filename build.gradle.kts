plugins {
    `java-library`
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotest.multiplatform)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
    alias(libs.plugins.axion.release)
    `maven-publish`
    signing
    alias(libs.plugins.nexus.publish)
}

group = "cz.smarteon.loxone"

scmVersion {
    tag {
        prefix.set(project.name)
        versionSeparator.set("-")
    }
}

project.version = scmVersion.version

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
    jvmToolchain(17)
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
            java {
                targetCompatibility = JavaVersion.VERSION_11
            }
        }
        withJava()

        configurations.all {
            resolutionStrategy.eachDependency {
                if (requested.group == "org.slf4j") {
                    useVersion(libs.versions.slf4j.get())
                    because("Align all slf4j artifacts to avoid logging binding/provider problems. " +
                        "It's safe to use 2.X.X version if dependency has been compiled against 1.X.X. " +
                        "But the 2.X.X logging providers does not work with 1.X.X api.")
                }
            }
        }

        compilations {
            val main by getting
            val test by getting

            create("acceptanceTest") {

                defaultSourceSet {
                    dependsOn(main.defaultSourceSet)
                    dependencies {
                        implementation(libs.kotest.assertions.core)
                        implementation(libs.kotest.framework.engine)
                        runtimeOnly(libs.slf4j.simple)
                    }
                }

                tasks.register<Test>("jvmAcceptanceTest") {
                    group = LifecycleBasePlugin.VERIFICATION_GROUP
                    classpath = main.compileDependencyFiles +
                        main.runtimeDependencyFiles +
                        output.allOutputs +

                        // TODO do not understand why this is needed
                        test.compileDependencyFiles +
                        test.runtimeDependencyFiles

                    testClassesDirs = output.classesDirs

                    doFirst {
                        if (listOf("LOX_ADDRESS", "LOX_USER", "LOX_PASS").any { System.getenv(it) == null }) {
                            throw GradleException("Missing environment variables for Loxone acceptance tests, please set LOX_ADDRESS, LOX_USER and LOX_PASS")
                        }
                    }
                }
            }
        }
        tasks.withType<Test>().configureEach { useJUnitPlatform() }
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

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
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
        commonTest.dependencies {
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.framework.datatest)
            implementation(libs.ktor.client.mock)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
            runtimeOnly(libs.slf4j.simple)
            implementation(libs.ktor.server.websockets)
            implementation(libs.ktor.server.test.host)
            implementation(libs.kotlinx.coroutines.test)
        }
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
        linuxMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
    }
}

kover {
    reports {
        filters {
            excludes {
                classes("cz.smarteon.loxone.*AT*")
            }
        }
    }
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
    from(tasks.getByName("dokkaHtml"))
}

publishing {
    publications.configureEach {
        if (this is MavenPublication) {
            artifact(dokkaJar)
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

val signingKey: String? = System.getenv("SIGNING_KEY")
val signingPassword: String? = System.getenv("SIGNING_PASS")
if (signingKey != null && signingPassword != null) {
    signing {
        isRequired = !project.version.toString().endsWith("-SNAPSHOT")
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }

    // workaround for https://github.com/gradle/gradle/issues/26091
    tasks.withType<AbstractPublishToMaven>().configureEach {
        dependsOn(tasks.withType<Sign>())
    }
}

val ossUser: String? = System.getenv("OSS_USER")
val ossPass: String? = System.getenv("OSS_PASS")
if (ossUser != null && ossPass != null) {
    nexusPublishing {
        repositories {
            sonatype {
                username.set(ossUser)
                password.set(ossPass)
            }
        }
    }
}
