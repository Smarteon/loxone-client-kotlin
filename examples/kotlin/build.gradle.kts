plugins {
    kotlin("jvm") version "1.9.21"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("cz.smarteon.loxone:loxone-client-kotlin")
}

kotlin {
    jvmToolchain(21)
}
