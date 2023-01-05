package ch.hippmann.androidpublisher.publisher

enum class Track {
    INTERNAL,
    ALPHA,
    BETA,
    PRODUCTION;

    override fun toString(): String {
        return this.name.lowercase()
    }
}
