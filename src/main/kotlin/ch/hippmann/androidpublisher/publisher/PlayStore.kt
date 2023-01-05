package ch.hippmann.androidpublisher.publisher

import ch.hippmann.androidpublisher.log
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import java.io.File
import kotlin.time.Duration.Companion.minutes


// https://developers.google.com/android-publisher/api-ref/
object PlayStore {
    private const val MIME_TYPE_MAPPING_FILE = "application/octet-stream"
    private const val MIME_TYPE_APP_BUNDLE_FILE = "application/octet-stream"

    internal fun upload(
        applicationName: String,
        outputFolder: File,
        packageName: String,
        track: String,
        uploadMappingFile: Boolean,
        credentialsFile: File,
        releaseNotesFile: File,
        versionCode: Int,
        shouldThrowIfNoReleaseNotes: Boolean
    ) {
        val edits = getAndroidPublisher(credentialsFile).Edits()
        val appEdit = edits
            .insert(packageName, null)
            .execute()
            .log { appEdit -> "Created AppEdit with id: ${appEdit.id}" }

        val bundleOutputDir = outputFolder.resolve("bundle/$applicationName")
        log("OutputDir content: ${bundleOutputDir.listFiles()?.map { it.absolutePath }}")
        val appBundleFile = //TODO: find a better way to get the release bundle
            bundleOutputDir
                .walkTopDown()
                .toList()
                .first { it.extension == "aab" }

        log("Uploading $appBundleFile...")
        val appBundleUpload = edits.bundles()
            .upload(packageName, appEdit.id, FileContent(MIME_TYPE_APP_BUNDLE_FILE, appBundleFile))
            .execute()
            .log { "App bundle with version code ${it.versionCode} uploaded" }

        if (uploadMappingFile) {
            log("Uploading deobfuscation mapping file...")
            val mappingFilesDir = outputFolder.resolve("mapping/$applicationName")
            val mappingFile = File(mappingFilesDir, "mapping.txt")
            require(mappingFile.exists()) {
                "No mapping file found in ${mappingFilesDir.absolutePath} for applicationVariant: $applicationName"
            }
            val mappingFileContent = FileContent(MIME_TYPE_MAPPING_FILE, mappingFile)

            edits.deobfuscationfiles()
                .upload(
                    packageName,
                    appEdit.id,
                    appBundleUpload.versionCode,
                    "proguard",
                    mappingFileContent
                )
                .execute()
                .log { "Deobfuscation mapping file for app bundle ${appBundleFile.name} with version code ${appBundleUpload.versionCode} uploaded" }
        }

        log("Updating track $track...")
        val trackRelease = TrackRelease().apply {
            versionCodes = listOf(appBundleUpload.versionCode).map { it.toLong() }
            status = "completed"
            releaseNotes = getReleaseNotesFromProject(releaseNotesFile, versionCode, shouldThrowIfNoReleaseNotes)
        }

        val trackModel = Track().apply {
            setTrack(track)
            releases = listOf(trackRelease)
        }

        edits.tracks()
            .update(packageName, appEdit.id, track, trackModel)
            .execute()
            .log { "Track ${it.track} updated" }

        log("Committing changes...")
        edits.commit(packageName, appEdit.id)
            .execute()
            .log { "AppEdit with id ${it.id} has been committed" }
    }

    internal fun getLatestVersionCode(packageName: String, credentialsFile: File): Int {
        val androidPublisher = getAndroidPublisher(credentialsFile)
        val edits = androidPublisher.Edits()

        val appEdit = edits.insert(packageName, null)
            .execute()
            .log { "Created app edit with id: ${it.id}" }

        val appBundleResponse = edits.bundles().list(packageName, appEdit.id).execute()

        val appBundle = appBundleResponse.bundles.reduce { acc, bundle ->
            if (acc.versionCode > bundle.versionCode) acc else bundle
        }

        return appBundle.versionCode.log { "Latest uploaded version code for $packageName: $it" }
    }

    private fun getAndroidPublisher(credentialsFile: File): AndroidPublisher {
        val newTrustedTransport = GoogleNetHttpTransport.newTrustedTransport()
        val credential = GoogleCredentials.fromStream(credentialsFile.inputStream())
            .createScoped(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER))
        val requestInitializer: HttpRequestInitializer = HttpCredentialsAdapter(credential)

        return AndroidPublisher.Builder(
            newTrustedTransport,
            GsonFactory.getDefaultInstance(),
            setHttpTimeout(requestInitializer)
        )
            .setApplicationName("androidpublisher")
            .build()
    }

    //increase timeout for uploading aab files to 2 mins according to
    //https://developers.google.com/android-publisher/api-ref/edits/bundles/upload
    private fun setHttpTimeout(requestInitializer: HttpRequestInitializer): HttpRequestInitializer {
        return HttpRequestInitializer { httpRequest ->
            requestInitializer.initialize(httpRequest)
            httpRequest.connectTimeout = 2.minutes.inWholeMilliseconds.toInt()
            httpRequest.readTimeout = 2.minutes.inWholeMilliseconds.toInt()
        }
    }

    private fun getReleaseNotesFromProject(
        releaseNotesFile: File,
        versionCode: Int,
        shouldThrowIfNoReleaseNotes: Boolean
    ): List<LocalizedText>? {
        return if (releaseNotesFile.exists() && releaseNotesFile.extension == "csv") {
            csvReader()
                .readAllWithHeader(releaseNotesFile)
                .map { row ->
                    val version = row.values.first()
                    val releaseNotes = row
                        .entries
                        .filterIndexed { index, _ -> index != 0 }
                        .map { languageToReleaseNote ->
                            LocalizedText().apply {
                                language = languageToReleaseNote.key
                                text = languageToReleaseNote.value
                            }
                        }
                    log("Found release notes for version $version with languages: ${releaseNotes.map { it.language }}")
                    Pair(version, releaseNotes)
                }
                .toMap()[versionCode.toString()]
                ?: if (shouldThrowIfNoReleaseNotes) {
                    throw IllegalStateException("There are no release notes present for the AppVersion $versionCode! Looked in File $releaseNotesFile")
                } else {
                    log("No release notes found for version $versionCode! Continuing with no release notes as configured...")
                    null
                }
        } else {
            if (shouldThrowIfNoReleaseNotes) {
                throw IllegalStateException("There are no release notes present for the AppVersion $versionCode! Looked in File $releaseNotesFile")
            } else {
                log("No release notes found for version $versionCode! Continuing with no release notes as configured...")
                null
            }
        }
    }
}
