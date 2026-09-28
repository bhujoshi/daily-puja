package com.worship.nityamandir.engine

/** Mixed tracks retain their deity tags, so Rama's temple can begin with a Rama bhajan. */
object BhajanQueue {
    fun order(tracks: List<BhajanPlayer.Track>, shuffle: Boolean, idol: String): List<BhajanPlayer.Track> {
        if (shuffle) return tracks.shuffled()
        val deity = when (idol) {
            "original", "ganesh_hanuman" -> "ganesh"
            "ram_darbar" -> "rama"
            else -> idol
        }
        val sorted = tracks.sortedBy { it.order }
        val first = sorted.firstOrNull { it.deity == deity } ?: return sorted
        return listOf(first) + (sorted - first)
    }
}
