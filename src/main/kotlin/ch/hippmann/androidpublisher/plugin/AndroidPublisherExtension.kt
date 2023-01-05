package ch.hippmann.androidpublisher.plugin

import ch.hippmann.androidpublisher.plugin.customtrack.CustomTrackConfiguration
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import java.io.File

open class AndroidPublisherExtension(objects: ObjectFactory) {
    val credentialsJsonFile: Property<File> = objects.property(File::class.java)
    val releaseNotesFile: Property<File> = objects.property(File::class.java)
    val shouldThrowIfNoReleaseNotes: Property<Boolean> = objects.property(Boolean::class.java)
    val enableGenerateVersionCode: Property<Boolean> = objects.property(Boolean::class.java)
    val appVersionCodeKey: Property<String> = objects.property(String::class.java)
    val createBundleIfNotExists: Property<Boolean> = objects.property(Boolean::class.java)

    val customTrackConfiguration = CustomTrackConfiguration()
    fun customTracks(block: CustomTrackConfiguration.() -> Unit) {
        block(customTrackConfiguration)
    }
}
