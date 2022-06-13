package ch.hippmann.androidpublisher.plugin.customtrack

import java.io.File

@DslMarker
annotation class CustomTrackDslMarker

@CustomTrackDslMarker
class CustomTrackConfiguration {
    internal val customTracks = mutableSetOf<CustomTrack>()

    fun track(trackId: String, configuration: CustomTrack.() -> Unit = {}) {
        val track = CustomTrack(trackId)
        configuration(track)
        customTracks.add(track)
    }
}

@CustomTrackDslMarker
data class CustomTrack(
    val trackId: String,
    var releaseNotesFile: File? = null,
    var shouldThrowIfNoReleaseNotes: Boolean? = null,
    var createBundleIfNotExists: Boolean? = null
)
