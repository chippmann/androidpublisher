# Publish apps to the GooglePlayStore through gradle
This plugin aims to simplify the publishing process for android apps to the google play store.  
It's currently still work in progress, but basic functionalities are present.  
Contributions are very welcome!

## Getting started
Look into [Getting started](GETTING_STARTED.md) section to get more information.

## Quick start
This quick start guide assumes you have already setup a google service account and you've already published an app bundle release to the track you want to upload to. If not, read the [Getting started](GETTING_STARTED.md) section.

Minimal gradle configuration:  
*App `build.gradle.kts`:*
```kotlin
plugins {
    id("com.android.application")
    id("ch.hippmann.androidpublisher") version "0.0.1"
}

androidpublisher {
    credentialsJsonPath = "play-store-credentials.json"
}
```

This requires the appVersionCode to be defined in `gradle.properties` with the key `appVersionCode`:
```properties
appVersionCode=23
```

This configuration generates gradleTasks in the group `androidpublisher` which you can call through CI/CD or locally if you have the signing keys locally that you used for publishing the app on the google play store.  
Travis example:
```yaml
deploy:
  - provider: script
    script: ./gradlew -DkeyPassword="$SIGNATURE_KEY_PASSWORD" -DstorePassword="$SIGNATURE_STORE_PASSWORD" generateVersionCodeForRelease uploadReleaseToInternalTrack
    on:
      tags: true
```  

Gradle example:
```shell script
./gradlew generateVersionCodeForRelease uploadReleaseToInternalTrack
```