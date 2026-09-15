plugins {
    alias(libs.plugins.kotlin.jvm)
    id("signing")
    id("maven-publish")
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    api(project(":core"))

    compileOnly("org.springframework:spring-context:6.1.14")
    compileOnly("org.springframework:spring-webmvc:6.1.14")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.2.11")

    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.2.11")
    implementation(kotlin("stdlib"))
}

kotlin {
    jvmToolchain(25)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = "io.github.lionpa"
            artifactId = "nok-validation-spring"
            version = rootProject.version.toString()

            pom {
                name = "Nok Validation Spring"
                description = "Spring Framework and Spring Boot integration for Nok Validation"
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