plugins {
    java
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.springboot.erp"
version = "0.0.1-SNAPSHOT"
description = "guru-erp-backend"

java {
    // JDK 21 provisioned automatically via Gradle toolchains + foojay resolver.
    // Gradle downloads a project-local JDK 21 into its cache; NO global JDK required.
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

extra["querydslVersion"] = "5.1.0"
extra["mapstructVersion"] = "1.6.3"
extra["jjwtVersion"] = "0.12.6"
extra["ulidVersion"] = "5.2.3"
extra["testcontainersVersion"] = "1.20.4"

dependencies {
    // --- Spring Boot starters ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    // Redis — refresh-token store (rotation/revocation) + login rate-limit counters.
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // --- Database driver ---
    runtimeOnly("org.postgresql:postgresql")

    // --- Flyway ---
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // --- QueryDSL (jakarta variant) ---
    implementation("com.querydsl:querydsl-jpa:${property("querydslVersion")}:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:${property("querydslVersion")}:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    // --- MapStruct ---
    implementation("org.mapstruct:mapstruct:${property("mapstructVersion")}")
    annotationProcessor("org.mapstruct:mapstruct-processor:${property("mapstructVersion")}")

    // --- Lombok (optional) ---
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    // Lombok-MapStruct binding so the two annotation processors cooperate.
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // --- JWT (jjwt) ---
    implementation("io.jsonwebtoken:jjwt-api:${property("jjwtVersion")}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${property("jjwtVersion")}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${property("jjwtVersion")}")

    // --- ULID ---
    implementation("com.github.f4b6a3:ulid-creator:${property("ulidVersion")}")

    // --- Tests ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:postgresql:${property("testcontainersVersion")}")
    testImplementation("org.testcontainers:junit-jupiter:${property("testcontainersVersion")}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Ensure the generated QueryDSL Q-classes are on the source path for IDEs/tooling.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
