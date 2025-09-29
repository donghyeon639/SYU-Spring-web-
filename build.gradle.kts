plugins {
    java
    id("org.springframework.boot") version "3.4.10"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // 웹 & 뷰 템플릿
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // ✅ JPA (JpaRepository 쓰려면 필요)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // ✅ MySQL Driver (DB 접속용)
    implementation("com.mysql:mysql-connector-j")

    // 테스트
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
