plugins {
    java
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("cz.smarteon.loxone:loxone-client-kotlin:0.1.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.7.1")
}
