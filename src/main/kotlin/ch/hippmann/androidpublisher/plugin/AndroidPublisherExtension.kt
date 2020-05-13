package ch.hippmann.androidpublisher.plugin

import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property

open class AndroidPublisherExtension(objects: ObjectFactory) {
    val credentialsJsonPath = objects.property<String>()
    val enableGenerateVersionCode = objects.property<Boolean>()
    val appVersionCodeKey = objects.property<String>()
}