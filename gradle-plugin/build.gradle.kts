plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.gradle.plugin)
    alias(libs.plugins.plugin.publish)

    id("signing")
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "Nok Validation"
            description = "Gradle plugin for Nok Validation"
            url = "https://github.com/lionpa/nok-validation"

            licenses {
                license {
                    name = "Apache License, Version 2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0"
                }
            }

            developers {
                developer {
                    id = "lionpa"
                    name = "LionPa"
                    email = "lionpa2009@gmail.com"
                    url = "https://github.com/lionpa"
                }
            }

            scm {
                url = "https://github.com/lionpa/nok-validation"
            }
        }
    }
}



signing {
    useGpgCmd()

    sign(publishing.publications)
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
    test {
        java.setSrcDirs(listOf("test"))
        resources.setSrcDirs(listOf("testResources"))
    }
}

dependencies {
    implementation(libs.kotlin.gradle.plugin.api)
    testImplementation(libs.kotlin.test.junit5)
}

buildConfig {
    packageName(project.group.toString())

    buildConfigField(
        "String",
        "KOTLIN_PLUGIN_ID",
        "\"io.lionpa.nok.compiler.validator\""
    )

    val pluginProject = project(":compiler-plugin")

    buildConfigField(
        "String",
        "KOTLIN_PLUGIN_GROUP",
        "\"${pluginProject.group}\""
    )

    buildConfigField(
        "String",
        "KOTLIN_PLUGIN_NAME",
        "\"nok-validation-compiler\""
    )

    buildConfigField(
        "String",
        "KOTLIN_PLUGIN_VERSION",
        "\"${pluginProject.version}\""
    )
}

gradlePlugin {
    plugins {
        create("NokValidatorGradlePlugin") {
            id = "io.github.lionpa.nok-validation"
            displayName = "Nok Validation"
            description = "Gradle plugin for Nok Validation"
            implementationClass = "io.lionpa.nok.compiler.validator.NokValidatorGradlePlugin"

            website = "https://github.com/lionpa/nok-validation"
            vcsUrl = "https://github.com/lionpa/nok-validation"
            tags.set(listOf("kotlin", "validation", "compiler-plugin"))
        }
    }
}
