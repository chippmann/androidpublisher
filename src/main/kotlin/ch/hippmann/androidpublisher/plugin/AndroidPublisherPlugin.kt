package ch.hippmann.androidpublisher.plugin

import ch.hippmann.androidpublisher.publisher.PlayStore
import ch.hippmann.androidpublisher.publisher.Track
import ch.hippmann.androidpublisher.publisher.VersionCodeGenerator
import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType

private const val TASK_GROUP = "androidpublisher"

class AndroidPublisherPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.withPlugin("com.android.application") { //apply config after the android plugin is applied
            val android = project.extensions.getByType<AppExtension>()
            val androidpublisher =
                project.extensions.create<AndroidPublisherExtension>("androidpublisher", project.objects)
            setupExtensionDefaults(androidpublisher)
            setupAndroidPublisherPlugin(project, android, androidpublisher)
        }
    }

    private fun setupExtensionDefaults(androidpublisher: AndroidPublisherExtension) {
        with(androidpublisher) {
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
                                project.projectDir.absolutePath,
                                applicationVariant.versionCode,
                                androidpublisher.appVersionCodeKey.get(),
                                androidpublisher.credentialsJsonPath.get()
                            )
                        }
                    }
                }

                Track.values().map { it.toString() }.forEach { track ->
                    project.tasks.register("upload${applicationVariant.name.capitalize()}To${track.capitalize()}Track") {
                        group = TASK_GROUP
                        dependsOn(project.tasks.getByName("bundle${applicationVariant.name.capitalize()}"))

                        doLast {
                            println("INFO: outputFiles: ${applicationVariant.outputs}")
                            val outputFile = applicationVariant
                                .outputs
                                .filter {
                                    it.outputFile.extension == "aar" && it.outputFile.name.contains(
                                        "release",
                                        true
                                    )
                                } //TODO: find a better way to get the release bundle
                                .map { it.outputFile }
                                .first()

                            PlayStore.upload(
                                outputFile,
                                applicationVariant.applicationId,
                                track,
                                applicationVariant.buildType.isMinifyEnabled,
                                androidpublisher.credentialsJsonPath.get()
                            )
                        }
                    }
                }
            }
        }
    }
}