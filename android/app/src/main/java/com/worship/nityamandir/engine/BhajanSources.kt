package com.worship.nityamandir.engine

/** A single catalog recording is allowed as direct audio. Never substitute another song. */
object BhajanSources {
    const val DEFAULT_ID = "jai_ganesh_deva"
    private val videoIdPattern = Regex("[A-Za-z0-9_-]{11}")

    fun playable(track: BhajanPlayer.Track): BhajanPlayer.Track? {
        if (track.id == DEFAULT_ID) {
            if (!track.url.startsWith("https://") || track.needsStreamResolution) return null
            return track.copy(isYoutube = false)
        }
        val videoId = track.youtubeId.takeIf { videoIdPattern.matches(it) } ?: return null
        return track.copy(
            url = "https://www.youtube.com/watch?v=$videoId",
            isYoutube = true,
            // Loudness measured on an archive file does not describe the YouTube recording.
            volumeGainDb = 0f
        )
    }

    fun catalog(tracks: List<BhajanPlayer.Track>): List<BhajanPlayer.Track> = tracks
        .filter { it.id == DEFAULT_ID || it.isYoutubeOnly }
        .mapNotNull(::playable)
        .sortedWith(compareBy<BhajanPlayer.Track> { it.id != DEFAULT_ID }.thenBy { it.order })
        .distinctBy { if (it.id == DEFAULT_ID) DEFAULT_ID else it.youtubeId }
}
