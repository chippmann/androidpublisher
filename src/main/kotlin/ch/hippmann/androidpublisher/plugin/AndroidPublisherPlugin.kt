package ch.hippmann.androidpublisher.plugin

import ch.hippmann.androidpublisher.publisher.PlayStore
import ch.hippmann.androidpublisher.publisher.Track
import ch.hippmann.androidpublisher.publisher.VersionCodeGenerator
import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.configurationcache.extensions.capitalized
import java.io.File

private const val TASK_GROUP = "androidpublisher"

class AndroidPublisherPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.withPlugin("com.android.application") { //apply config after the android plugin is applied
            val android = project.extensions.getByType(AppExtension::class.java)

            val androidpublisher = project
                .extensions
                .create("androidpublisher", AndroidPublisherExtension::class.java, project.objects)

            setupExtensionDefaults(project, androidpublisher)
            setupAndroidPublisherPlugin(project, android, androidpublisher)
        }
    }

    private fun setupExtensionDefaults(project: Project, androidpublisher: AndroidPublisherExtension) {
        with(androidpublisher) {
            releaseNotesFile.set(File("${project.rootDir.absolutePath}/releaseNotes.csv"))
            shouldThrowIfNoReleaseNotes.set(true)
            enableGenerateVersionCode.set(true)
            appVersionCodeKey.set("appVersionCode")
            createBundleIfNotExists.set(true)
            inAppUpdatePriorityProvider = { null }
        }
    }

    private fun setupAndroidPublisherPlugin(
        project: Project,
        android: AppExtension,
        androidpublisher: AndroidPublisherExtension
    ) {
        project.afterEvaluate {
            android.applicationVariants.forEach { applicationVariant ->
                if (androidpublisher.enableGenerateVersionCode.get()) {
                    project.tasks.register("generateVersionCodeFor${applicationVariant.name.capitalized()}") { task ->
                        task.group = TASK_GROUP
                        task.doLast {
                            VersionCodeGenerator.generateVersionCode(
                                packageName = applicationVariant.applicationId,
                                baseFolder = project.rootDir.absolutePath,
                                gradleVersionCode = applicationVariant.versionCode,
                                appVersionCodeKey = androidpublisher.appVersionCodeKey.get(),
                                credentialsFile = androidpublisher.credentialsJsonFile.get()
                            )
                        }
                    }
                }

                Track.values().map { it.toString() }.forEach { track ->
                    project.tasks.register("upload${applicationVariant.name.capitalized()}To${track.capitalized()}Track") { task ->
                        task.group = TASK_GROUP

                        if (androidpublisher.createBundleIfNotExists.getOrElse(true)) {
                            task.dependsOn(project.tasks.getByName("bundle${applicationVariant.name.capitalized()}"))
                        }

                        task.doLast {
                            PlayStore.upload(
                                applicationName = applicationVariant.name,
                                outputFolder = project.layout.buildDirectory.asFile.get().resolve("outputs"),
                                packageName = applicationVariant.applicationId,
                                track = track,
                                uploadMappingFile = applicationVariant.buildType.isMinifyEnabled,
                                credentialsFile = androidpublisher.credentialsJsonFile.get(),
                                releaseNotesFile = androidpublisher.releaseNotesFile.get(),
                                versionCode = applicationVariant.versionCode,
                                shouldThrowIfNoReleaseNotes = androidpublisher.shouldThrowIfNoReleaseNotes.get(),
                                inAppUpdatePriorityProvider = androidpublisher.inAppUpdatePriorityProvider,
                            )
                        }
                    }
                }

                androidpublisher.customTrackConfiguration.customTracks.forEach { customTrack ->
                    val cleanedTrackId = customTrack.trackId.replace(" ", "").capitalized()
                    project.tasks.register("upload${applicationVariant.name.capitalized()}To${cleanedTrackId}Track") { task ->
                        task.group = TASK_GROUP

                        if (customTrack.createBundleIfNotExists ?: androidpublisher.createBundleIfNotExists.getOrElse(true)) {
                            task.dependsOn(project.tasks.getByName("bundle${applicationVariant.name.capitalized()}"))
                        }

                        task.doLast {
                            PlayStore.upload(
                                applicationName = applicationVariant.name,
                                outputFolder = project.buildDir.resolve("outputs"),
                                packageName = applicationVariant.applicationId,
                                track = customTrack.trackId,
                                uploadMappingFile = applicationVariant.buildType.isMinifyEnabled,
                                credentialsFile = androidpublisher.credentialsJsonFile.get(),
                                releaseNotesFile = customTrack.releaseNotesFile ?: androidpublisher.releaseNotesFile.get(),
                                versionCode = applicationVariant.versionCode,
                                shouldThrowIfNoReleaseNotes = customTrack.shouldThrowIfNoReleaseNotes ?: androidpublisher.shouldThrowIfNoReleaseNotes.get(),
                                inAppUpdatePriorityProvider = androidpublisher.inAppUpdatePriorityProvider,
                            )
                        }
                    }
                }
            }
        }
    }
}
