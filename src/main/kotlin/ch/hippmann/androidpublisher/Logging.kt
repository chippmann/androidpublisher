package ch.hippmann.androidpublisher

internal fun log(message: String) = println("INFO: $message")

internal inline fun <T> T.log(block: (T) -> String): T {
    log(block(this))
    return this
}
