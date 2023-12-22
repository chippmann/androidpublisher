plugins {
    // https://kotlinlang.org/docs/releases.html
    kotlin("jvm") version "1.9.21"
    `java-gradle-plugin`
    `maven-publish`
    // https://plugins.gradle.org/plugin/com.gradle.plugin-publish
    id("com.gradle.plugin-publish") version "1.2.1"
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
    compileOnly("com.android.tools.build:gradle:8.2.0")

    // https://search.maven.org/artifact/com.google.apis/google-api-services-androidpublisher
    implementation("com.google.apis:google-api-services-androidpublisher:v3-rev20231115-2.0.0")

    // TODO: remove once com.google.apis:google-api-services-androidpublisher:v3-rev20231115-2.0.0 is updated
    // needed because of a security vulnerability or the guava version defined in com.google.apis:google-api-services-androidpublisher:v3-rev20231115-2.0.0
    implementation("com.google.guava:guava:33.0.0-jre")

    // Required to read the credentials from the credentials json file. Library suggested in the corresponding deprecation notes of google-api-services-androidpublisher
    // https://github.com/googleapis/google-auth-library-java/releases
    implementation("com.google.auth:google-auth-library-oauth2-http:1.20.0")

    // https://github.com/doyaaaaaken/kotlin-csv/releases
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.2")
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
