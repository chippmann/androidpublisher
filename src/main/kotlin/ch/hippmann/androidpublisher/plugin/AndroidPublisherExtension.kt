package ch.hippmann.androidpublisher.plugin

import ch.hippmann.androidpublisher.plugin.customtrack.CustomTrackConfiguration
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import java.io.File

open class AndroidPublisherExtension(objects: ObjectFactory) {
    val credentialsJsonFile = objects.property<File>()
    val releaseNotesFile = objects.property<File>()
    val shouldThrowIfNoReleaseNotes = objects.property<Boolean>()
    val enableGenerateVersionCode = objects.property<Boolean>()
    val appVersionCodeKey = objects.property<String>()
    val createBundleIfNotExists = objects.property<Boolean>()

    val customTrackConfiguration = CustomTrackConfiguration()
    fun customTracks(block: CustomTrackConfiguration.() -> Unit) {
        block(customTrackConfiguration)
    }
}
