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
    id("ch.hippmann.androidpublisher") version "0.2.0"
}

androidpublisher {
    credentialsJsonFile = File("path/to/your/service/account/credentials.json")
}
```

This requires the appVersionCode to be defined in `gradle.properties` with the key `appVersionCode`:
```properties
appVersionCode=23
```
and a `releaseNotes.csv` file present in the rootProject dir with the following spreadsheet format:  

| VersionCode | en-gb                       | de-ch               | otherLanguages       |
|-------------|-----------------------------|---------------------|----------------------|
| 1           | Some release notes in UTF-8 | Some text in german | Other languages text |
| 2           | Some release notes in UTF-8 | Some text in german | Other languages text |
|             |                             |                     |                      |

\
This configuration generates gradleTasks in the group `androidpublisher` which you can call through CI/CD or locally if you have the signing keys locally that you used for publishing the app on the google play store.  
Travis example:
```yaml
deploy:
  - provider: script
    script: ./gradlew generateVersionCodeForRelease && ./gradlew -DkeyPassword="$SIGNATURE_KEY_PASSWORD" -DstorePassword="$SIGNATURE_STORE_PASSWORD" uploadReleaseToInternalTrack
    on:
      tags: true
```  
(Note the separation of `generateVersionCodeForRelease` and `uploadReleaseToInternalTrack` into two separate gradle commands. This is necessary for gradle to pick up the changed versionCode!)

Gradle example:
```shell script
./gradlew generateVersionCodeForRelease && ./gradlew uploadReleaseToInternalTrack
```

## Known limitations
 - Only one versionCode for all app variants supported
 
 These limitations might get addressed in future versions
 
 ## Development
 To be able to test the plugin with a local build, one needs to execute `publishToMavenLocal` and add the following to 
 the top of the app's `settings.gradle.kts` file:  
 
 *Gradle KTS:*
 ```kotlin
 // without this doing:
 //  plugin { id("ch.hippmann.androidpublisher") version "0.0.1-SNAPSHOT" }
 // won't work  as gradle does not know how to map the plugin id to an actual artifact.
 // this is only required when trying out local builds. Comment this out when trying out a plugin published
 // in the gradle plugin portal.
 pluginManagement {
     repositories {
         mavenLocal()
         jcenter()
         gradlePluginPortal()
     }
 
     resolutionStrategy.eachPlugin {
         when (requested.id.id) {
             "ch.hippmann.androidpublisher" -> useModule("ch.hippmann.androidpublisher:${requested.version}")
         }
     }
 }

```
*Gradle groovy:*
```groovy
 // without this doing:
 //  plugin { id "ch.hippmann.androidpublisher" version "0.0.1-SNAPSHOT" }
 // won't work  as gradle does not know how to map the plugin id to an actual artifact.
 // this is only required when trying out local builds. Comment this out when trying out a plugin published
 // in the gradle plugin portal.
pluginManagement {
    repositories {
        mavenLocal()
        jcenter()
        gradlePluginPortal()
    }

    resolutionStrategy.eachPlugin {
        if (requested.id.id == "ch.hippmann.androidpublisher") {
            useModule("ch.hippmann:androidpublisher:" + requested.version)
        }
    }
}
```
