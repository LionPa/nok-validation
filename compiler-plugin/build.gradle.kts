plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.gradle.java.test.fixtures)
    alias(libs.plugins.node.gradle)
    alias(libs.plugins.gradle.idea)

    id("signing")

    id("maven-publish")
    id("com.gradleup.shadow") version "9.2.2"
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["shadow"])

            groupId = "io.github.lionpa"
            artifactId = "nok-validation-compiler"
            version = rootProject.version.toString()

            pom {
                name = "Nok Validation Compiler"
                description = "Kotlin compiler plugin for Nok Validation"
                url = "https://github.com/lionpa/nok-validation"

                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }

                scm {
                    url = "https://github.com/lionpa/nok-validation"
                }

                developers {
                    developer {
                        id = "lionpa"
                        name = "LionPa"
                        email = "lionpa2009@gmail.com"
                        url = "https://github.com/lionpa"
                    }
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
    testFixtures {
        java.setSrcDirs(listOf("test-fixtures"))
    }
    test {
        java.setSrcDirs(listOf("test", "test-gen"))
        resources.setSrcDirs(listOf("testData"))
    }
}

idea {
    module.generatedSourceDirs.add(projectDir.resolve("test-gen"))
}

val testArtifacts: Configuration by configurations.creating

val annotationsRuntimeClasspath by configurations.dependencyScope("annotationsRuntimeClasspath") {
    isTransitive = false
}
val annotationsJvmRuntimeClasspath by configurations.resolvable("annotationsJvmRuntimeClasspath") {
    extendsFrom(annotationsRuntimeClasspath)
}

dependencies {
    compileOnly(libs.kotlin.compiler)

    testFixturesApi(libs.kotlin.test.junit5)
    testFixturesApi(libs.kotlin.test.framework)
    testFixturesRuntimeOnly(libs.junit)
}

buildConfig {
    useKotlinOutput {
        internalVisibility = true
    }

    packageName("io.lionpa.nok.compiler.validator")

    buildConfigField(
        "String",
        "KOTLIN_PLUGIN_ID",
        "\"io.lionpa.nok.compiler.validator\""
    )
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
        optIn.add("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}