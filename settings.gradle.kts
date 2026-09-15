pluginManagement {
    repositories {
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradleup.nmcp.settings") version "1.6.1"
}
nmcpSettings {
    centralPortal {
        username.set(providers.gradleProperty("centralPortalUsername"))
        password.set(providers.gradleProperty("centralPortalPassword"))
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "NokValidator"

include("compiler-plugin")
include("gradle-plugin")
include("core")
include("spring")