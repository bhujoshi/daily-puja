package com.worship.nityamandir.engine

import com.worship.nityamandir.data.model.AgingState
import kotlin.math.min
import kotlin.math.roundToInt

object AgingEngine {

    /**
     * Calculates dust accumulation, flower withering, and cleaning requirements.
     *
     * @param lastWorshipTimeMs Epoch timestamp of last completed worship (null if never worshipped)
     * @param lastCleanedTimeMs Epoch timestamp of last temple cleaning (null if never cleaned)
     * @param currentTimeMs Current effective time (including any debug time-travel offset)
     */
    fun calculateAging(
        lastWorshipTimeMs: Long?,
        lastCleanedTimeMs: Long?,
        currentTimeMs: Long
    ): AgingState {
        if (lastWorshipTimeMs == null) {
            return AgingState(
                elapsedHours = 0.0f,
                dustLevel = 0.0f,
                flowerWitherFactor = 0.0f,
                needsCleaning = false,
                statusHi = "मंदिर नवनिर्मित एवं पवित्र है। प्रथम पूजा आरंभ करें।",
                statusEn = "Temple is pristine. Begin your first pooja."
            )
        }

        val elapsedWorshipMs = (currentTimeMs - lastWorshipTimeMs).coerceAtLeast(0L)
        val elapsedWorshipHours = elapsedWorshipMs / (1000f * 60f * 60f)

        // Dust calculation depends on when it was last cleaned or last worshipped
        val cleanReferenceMs = if (lastCleanedTimeMs != null && lastCleanedTimeMs > lastWorshipTimeMs) {
            lastCleanedTimeMs
        } else {
            lastWorshipTimeMs
        }
        val elapsedCleanMs = (currentTimeMs - cleanReferenceMs).coerceAtLeast(0L)
        val elapsedCleanHours = elapsedCleanMs / (1000f * 60f * 60f)

        // Mathematical dust formula: D = min(1.0, (t - 14) / 72) for t > 14
        val dustLevel = if (elapsedCleanHours > 14.0f) {
            min(1.0f, (elapsedCleanHours - 14.0f) / 72.0f)
        } else {
            0.0f
        }

        // Mathematical flower withering formula: F = min(1.0, (t - 12) / 24) for t > 12
        val flowerWither = if (cleanReferenceMs == lastWorshipTimeMs && elapsedWorshipHours > 12.0f) {
            min(1.0f, (elapsedWorshipHours - 12.0f) / 24.0f)
        } else {
            0.0f
        }

        val needsCleaning = elapsedCleanHours >= 14.0f || dustLevel > 0.05f || flowerWither > 0.1f

        val (statusHi, statusEn) = when {
            elapsedWorshipHours < 12.0f -> {
                "मंदिर प्रकाशमान एवं पावन है।" to "Temple is glowing and sacred."
            }
            elapsedWorshipHours < 24.0f -> {
                "कल के पुष्प निर्माल्य हो रहे हैं। पूजा से पूर्व सफाई करें।" to
                "Yesterday's flowers have withered. Morning cleaning required."
            }
            elapsedWorshipHours < 72.0f -> {
                "मंदिर में धूल व निर्माल्य एकत्रित हो गया है। कृपा कर मंदिर स्वच्छ करें।" to
                "Dust and nirmalya have accumulated. Please clean the temple."
            }
            else -> {
                "मंदिर अत्यधिक जीर्ण व धूल-धूसरित हो गया है। संपूर्ण शुद्धि आवश्यक है।" to
                "Temple is heavily neglected. Deep cleaning required."
            }
        }

        return AgingState(
            elapsedHours = ((elapsedWorshipHours * 100).roundToInt()) / 100f,
            dustLevel = ((dustLevel * 1000).roundToInt()) / 1000f,
            flowerWitherFactor = ((flowerWither * 1000).roundToInt()) / 1000f,
            needsCleaning = needsCleaning,
            statusHi = statusHi,
            statusEn = statusEn
        )
    }
}
