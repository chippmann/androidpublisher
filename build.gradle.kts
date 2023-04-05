plugins {
    // https://kotlinlang.org/docs/releases.html
    kotlin("jvm") version "1.8.0"
    `java-gradle-plugin`
    `maven-publish`
    // https://plugins.gradle.org/plugin/com.gradle.plugin-publish
    id("com.gradle.plugin-publish") version "1.1.0"
}

group = "ch.hippmann"
version = "0.3.2"

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

gradlePlugin {
    website.set("https://github.com/chippmann/androidpublisher")
    vcsUrl.set("https://github.com/chippmann/androidpublisher.git")

    plugins {
        create("androidpublisher") {
            id = "ch.hippmann.androidpublisher"
            displayName = "Publish apps to the Google Play Store"
            description = "Gradle plugin for publishing apps to the Google Play Store"
            tags.set(listOf("kotlin", "android", "playstore"))
            implementationClass = "ch.hippmann.androidpublisher.plugin.AndroidPublisherPlugin"
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("gradle-plugin"))
    implementation(kotlin("gradle-plugin-api"))
    // https://developer.android.com/studio/releases/gradle-plugin
    compileOnly("com.android.tools.build:gradle:7.3.0")

    // https://search.maven.org/artifact/com.google.apis/google-api-services-androidpublisher
    implementation("com.google.apis:google-api-services-androidpublisher:v3-rev20221108-2.0.0")

    // Required to read the credentials from the credentials json file. Library suggested in the corresponding deprecation notes of google-api-services-androidpublisher
    // https://github.com/googleapis/google-auth-library-java/releases
    implementation("com.google.auth:google-auth-library-oauth2-http:1.14.0")

    // TODO: remove once a new version of com.google.apis:google-api-services-androidpublisher is out. This is here to override the requested version of the dependency as the requested version (1.11) has a security vulnerability: Cxeb68d52e-5509 (https://devhub.checkmarx.com/cve-details/Cxeb68d52e-5509/) 3.7 Exposure of Sensitive Information to an Unauthorized Actor vulnerability pending CVSS allocation
    implementation("commons-codec:commons-codec:1.15")

    // https://github.com/doyaaaaaken/kotlin-csv/releases
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.7.0")
}

tasks {
    build {
        finalizedBy(publishToMavenLocal)
    }
}

publishing {
    publications {
        // this is only used for publishing locally.
        @Suppress("UNUSED_VARIABLE")
        val androidpublisherPlugin by creating(MavenPublication::class) {
            pom {
                groupId = "${project.group}"
                artifactId = project.name
                version = "${project.version}"
            }
            from(components.getByName("java"))
        }
    }
}
