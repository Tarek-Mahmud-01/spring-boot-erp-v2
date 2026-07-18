plugins {
    // Lets Gradle auto-provision the required JDK toolchain (Java 21) from foojay,
    // downloading a project-local JDK into the Gradle cache — no global JDK needed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "guru-erp-backend"
