package com.worship.nityamandir.data.model

enum class PoojaItem {
    FLOWER,
    KUMKUM,
    KALASH,
    MATCHBOX,
    BELL,
    SHANKH,
    AARTI_THALI,
    BHOG
}

enum class TempleStyle(val displayNameHi: String, val displayNameEn: String) {
    SHEESHAM_WOOD("शीशम काष्ठ मंदिर", "Carved Sheesham Wood"),
    MAKRANA_MARBLE("श्वेत संगमरमर मंदिर", "Makrana White Marble"),
    BRASS_GOLD("स्वर्ण-पीतल मंदिर", "Golden Brass Temple")
}

data class Deity(
    val id: String,
    val nameHi: String,
    val nameEn: String,
    val descriptionHi: String,
    val descriptionEn: String,
    val tilakApplied: Boolean = false,
    val flowersOfferedCount: Int = 0
)

data class AgingState(
    val elapsedHours: Float,
    val dustLevel: Float,          // 0.0f to 1.0f
    val flowerWitherFactor: Float, // 0.0f to 1.0f
    val needsCleaning: Boolean,
    val statusHi: String,
    val statusEn: String
)

data class AartiLyric(
    val timestampMs: Long,
    val lineHi: String,
    val lineEn: String,
    val meaningEn: String
)

data class AartiTrack(
    val id: String,
    val titleHi: String,
    val titleEn: String,
    val deityId: String,
    val durationSeconds: Int,
    val lyrics: List<AartiLyric>
)
