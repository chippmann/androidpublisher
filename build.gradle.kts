plugins {
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "0.11.0"
}

group = "ch.hippmann"
version = "0.0.3"

repositories {
    mavenLocal()
    jcenter()
    google()
}

gradlePlugin {
    plugins {
        create("androidpublisher") {
            id = "ch.hippmann.androidpublisher"
            displayName = "Publish apps to the Google Play Store"
            description = "Gradle plugin for publishing apps to the Google Play Store"
            implementationClass = "ch.hippmann.androidpublisher.plugin.AndroidPublisherPlugin"
        }
    }
    isAutomatedPublishing = false
}

pluginBundle {
    website = "https://github.com/chippmann/androidpublisher"
    vcsUrl = "https://github.com/chippmann/androidpublisher.git"
    tags = listOf("kotlin", "android", "playstore")

    mavenCoordinates {
        groupId = "${project.group}"
        artifactId = "androidpublisher-gradle-plugin"
        version = "${project.version}"
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("gradle-plugin"))
    implementation(kotlin("gradle-plugin-api"))
    compileOnly("com.android.tools.build:gradle:3.6.2")
    implementation("com.google.apis:google-api-services-androidpublisher:v3-rev20200331-1.30.9")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:0.7.3")
}

tasks {
    val sourceJar by creating(Jar::class) {
        archiveBaseName.set(project.name)
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    build {
        finalizedBy(publishToMavenLocal)
    }
}

publishing {
    publications {
        // this is only used for publishing locally.
        val androidpublisherPlugin by creating(MavenPublication::class) {
            pom {
                groupId = "${project.group}"
                artifactId = project.name
                version = "${project.version}"
            }
            from(components.getByName("java"))
            artifact(tasks.getByName("sourceJar"))
        }
    }
}