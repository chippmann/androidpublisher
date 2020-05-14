package ch.hippmann.androidpublisher.publisher

import ch.hippmann.androidpublisher.log
import java.io.File
import java.util.*
import kotlin.math.max

object VersionCodeGenerator {
    internal fun generateVersionCode(
        packageName: String,
        baseFolder: String,
        gradleVersionCode: Int,
        appVersionCodeKey: String,
        credentialsFile: File
    ) {
        val playStoreVersionCode = PlayStore.getLatestVersionCode(packageName, credentialsFile)
        log("Play store version code: $playStoreVersionCode, gradle version code: $gradleVersionCode")
        val newVersionCode = max(playStoreVersionCode, gradleVersionCode) + 1
        log("New version code for $packageName: $newVersionCode")
        setVersionCodeInGradleProperties(
            baseFolder,
            newVersionCode,
            appVersionCodeKey
        )
    }

    private fun setVersionCodeInGradleProperties(baseFolder: String, versionCode: Int, appVersionCodeKey: String) {
        log("Writing version code $versionCode to gradle.properties with key: $appVersionCodeKey")
        val propertiesFile =
            getGradlePropertiesFile(
                baseFolder
            )
        val properties = Properties().apply { load(propertiesFile.inputStream()) }
        properties.setProperty(appVersionCodeKey, versionCode.toString())
        properties.store(propertiesFile.outputStream(), null)
    }

    private fun getGradlePropertiesFile(baseFolder: String) = File("$baseFolder/gradle.properties")
}