package ch.hippmann.androidpublisher.plugin

import ch.hippmann.androidpublisher.publisher.PlayStore
import ch.hippmann.androidpublisher.publisher.Track
import ch.hippmann.androidpublisher.publisher.VersionCodeGenerator
import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import java.io.File

private const val TASK_GROUP = "androidpublisher"

class AndroidPublisherPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.withPlugin("com.android.application") { //apply config after the android plugin is applied
            val android = project.extensions.getByType<AppExtension>()
            val androidpublisher =
                project.extensions.create<AndroidPublisherExtension>("androidpublisher", project.objects)
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
                    project.tasks.register("generateVersionCodeFor${applicationVariant.name.capitalize()}") {
                        group = TASK_GROUP
                        doLast {
                            VersionCodeGenerator.generateVersionCode(
                                applicationVariant.applicationId,
                                project.rootDir.absolutePath,
                                applicationVariant.versionCode,
                                androidpublisher.appVersionCodeKey.get(),
                                androidpublisher.credentialsJsonFile.get()
                            )
                        }
                        }
                }

                Track.values().map { it.toString() }.forEach { track ->
                    project.tasks.register("upload${applicationVariant.name.capitalize()}To${track.capitalize()}Track") {
                        group = TASK_GROUP
                        dependsOn(project.tasks.getByName("bundle${applicationVariant.name.capitalize()}"))

                        doLast {
                            PlayStore.upload(
                                applicationVariant,
                                project.buildDir.resolve("outputs"),
                                applicationVariant.applicationId,
                                track,
                                applicationVariant.buildType.isMinifyEnabled,
                                androidpublisher.credentialsJsonFile.get(),
                                androidpublisher.releaseNotesFile.get(),
                                applicationVariant.versionCode,
                                androidpublisher.shouldThrowIfNoReleaseNotes.get()
                            )
                        }
                    }
                }
            }
        }
    }
}
