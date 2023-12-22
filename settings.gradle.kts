rootProject.name = "androidpublisher"

plugins {
    // downloads missing jdk for `jvmToolchain` see: https://docs.gradle.org/current/userguide/toolchains.html#sub:download_repositories
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.7.0") // https://github.com/gradle/foojay-toolchains/tags
}