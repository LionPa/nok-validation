plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.node.gradle) apply false
    alias(libs.plugins.kotlin.binary.compatibility.validator) apply false
    alias(libs.plugins.buildconfig) apply false
}

allprojects {
    group = "io.github.lionpa"
    version = "1.0.1"
}
