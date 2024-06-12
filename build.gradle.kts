plugins {
    kotlin("jvm") version "2.0.0"
    id("org.flywaydb.flyway") version "10.14.0"

    id("org.jetbrains.kotlin.plugin.jpa") version "1.9.24"
}

group = "com.timmermans"
version = "1.0-SNAPSHOT"

val koinVersion = "3.5.0"

repositories {
    mavenCentral()
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.flywaydb:flyway-mysql:10.14.0")
    }
}

dependencies {
    implementation("io.insert-koin:koin-core:$koinVersion")

    implementation("org.hibernate:hibernate-core:5.6.15.Final")
    implementation("org.hibernate:hibernate-hikaricp:5.6.15.Final")

    // Required by Hibernate
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.0")

    implementation("ch.qos.logback:logback-classic:1.5.6")


    // Required for flyway
    implementation("mysql:mysql-connector-java:8.0.28")

    testImplementation("io.insert-koin:koin-test-junit5:$koinVersion")
    // For Parameterized tests
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}

tasks.jar {
    archiveBaseName.set("app")
    archiveVersion.set("")
    archiveClassifier.set("")

    manifest {
        attributes["Main-Class"] = "com.timmermans.MainKt"
    }
}

kotlin {
    jvmToolchain(17)
}

flyway {
    url = "jdbc:mysql://localhost:3306/dev"
    user = "root"
    password = "root"
    locations = arrayOf("filesystem:src/main/resources/db/migrations")
    cleanDisabled = false
}