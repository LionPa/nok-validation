plugins {
    kotlin("jvm") version "2.3.20"
    id("me.champeau.jmh") version "0.7.3"
    id("io.github.lionpa.nok-validation") version "1.0.1"
}

group = "io.lionpa.nok.validation"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.hibernate.validator:hibernate-validator:9.1.3.Final")
    implementation("org.glassfish.expressly:expressly:6.0.0")

    implementation("io.konform:konform-jvm:0.11.0")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)

    compilerOptions {
        freeCompilerArgs.add("-Xemit-jvm-type-annotations") // For working validation in list. Jakarta
    }
}


