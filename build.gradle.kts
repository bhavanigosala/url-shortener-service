plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.urlshortener"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Web & Core
    implementation("org.springframework.boot:spring-boot-starter-web:3.3.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.3.0")
    implementation("org.springframework.boot:spring-boot-starter-actuator:3.3.0")

    // Database
    implementation("org.springframework.boot:spring-boot-starter-data-redis:3.3.0")
    implementation("com.h2database:h2:2.2.220")
    implementation("org.postgresql:postgresql:42.7.1")

    // Virtual Threads & Async Support
    implementation("org.springframework.boot:spring-boot-starter-webflux:3.3.0")

    // Observability & Metrics
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.0")
    implementation("io.opentelemetry:opentelemetry-api:1.35.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.35.0")

    // Resilience & Circuit Breaker
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.1.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.1.0")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.1.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.1.0")

    // Lombok (for convenience, minimal usage)
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // JSON/Serialization
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation:3.3.0")

    // Logging
    implementation("org.springframework.boot:spring-boot-starter-logging:3.3.0")
    implementation("org.slf4j:slf4j-api:2.0.11")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.3.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.testcontainers:testcontainers:1.19.4")
    testImplementation("org.testcontainers:postgresql:1.19.4")
}

tasks.withType<JavaCompile> {
    options.apply {
        encoding = "UTF-8"
        release.set(21)
        compilerArgs.addAll(listOf(
            "--enable-preview"
        ))
    }
}

tasks.withType<Test> {
    jvmArgs("--enable-preview")
}

tasks.bootRun {
    jvmArgs("--enable-preview")
}
