package ch.hippmann.androidpublisher.publisher

import ch.hippmann.androidpublisher.log
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import java.io.File

// https://developers.google.com/android-publisher/api-ref/
object PlayStore {
    private const val MIME_TYPE_MAPPING_FILE = "application/octet-stream"
    private const val MIME_TYPE_APP_BUNDLE_FILE = "application/octet-stream"

    internal fun upload(
        appBundleFile: File,
        packageName: String,
        track: String,
        uploadMappingFile: Boolean,
        credentialsFile: String
    ) {
        val edits = getAndroidPublisher(credentialsFile).Edits()
        val appEdit = edits
            .insert(packageName, null)
            .execute()
            .log { appEdit -> "Created AppEdit with id: ${appEdit.id}" }

        log("Uploading $appBundleFile...")
        val appBundleUpload = edits.bundles()
            .upload(packageName, appEdit.id, FileContent(MIME_TYPE_APP_BUNDLE_FILE, appBundleFile))
            .execute()
            .log { "App bundle with version code ${it.versionCode} uploaded" }

        if (uploadMappingFile) {
            log("Uploading deobfuscation mapping file...")
            val mappingFile =
                requireNotNull(appBundleFile.parentFile.walkTopDown().firstOrNull { it.name == "mapping.txt" }) {
                    "No mapping file found in ${appBundleFile.parentFile} for bundle: ${appBundleFile.name}"
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

    internal fun getLatestVersionCode(packageName: String, credentialsFile: String): Int {
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

    private fun getAndroidPublisher(credentialsFile: String): AndroidPublisher {
        val newTrustedTransport = GoogleNetHttpTransport.newTrustedTransport()
        val resourceAsStream = File::class.java.getResourceAsStream(credentialsFile)
        val credential = GoogleCredential.fromStream(resourceAsStream)
            .createScoped(listOf(AndroidPublisherScopes.ANDROIDPUBLISHER)) //TODO: replace with https://github.com/googleapis/google-auth-library-java
        return AndroidPublisher.Builder(
            newTrustedTransport,
            JacksonFactory.getDefaultInstance(),
            setHttpTimeout(credential)
        )
            .setApplicationName("androidpublisher")
            .build()
    }

    //increase timeout for uploading aab files to 2 mins according to
    //https://developers.google.com/android-publisher/api-ref/edits/bundles/upload
    private fun setHttpTimeout(requestInitializer: HttpRequestInitializer): HttpRequestInitializer? {
        return HttpRequestInitializer { httpRequest ->
            requestInitializer.initialize(httpRequest)
            httpRequest.connectTimeout = 2 * 60000 // 2 minutes connect timeout
            httpRequest.readTimeout = 2 * 60000 // 2 minutes read timeout
        }
    }
}