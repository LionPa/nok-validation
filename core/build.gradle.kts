plugins {
    kotlin("jvm") version "2.3.20"

    id("signing")

    id("maven-publish")

    id("io.github.lionpa.nok-validation") version "1.0.1"
}

java {
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(25)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = "io.github.lionpa"
            artifactId = "nok-validation-core"
            version = rootProject.version.toString()

            pom {
                name = "Nok Validation Core"
                description = "Runtime library for Nok Validation"
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
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}
