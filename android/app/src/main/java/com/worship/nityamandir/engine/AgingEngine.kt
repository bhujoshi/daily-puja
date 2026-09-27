package com.worship.nityamandir.engine

import com.worship.nityamandir.data.model.AgingState
import kotlin.math.exp

object AgingEngine {
    const val DAY_MS = 86_400_000L

    /** The latest refresh starts a new aging cycle, including replacement flowers. */
    fun calculateAging(lastWorshipTimeMs: Long?, lastCleanedTimeMs: Long?, currentTimeMs: Long): AgingState {
        val reference = listOfNotNull(lastWorshipTimeMs, lastCleanedTimeMs).maxOrNull()
        val hours = reference?.let { ((currentTimeMs - it).coerceAtLeast(0L) / 3_600_000.0).toFloat() } ?: 0f
        val days = hours / 24f
        // Gradual accumulation continues beyond the first week, approaching natural saturation.
        val dust = (1.0 - exp(-days.toDouble() / 4.0)).toFloat()
        val wilt = (1.0 - exp(-days.toDouble() * days / 5.0)).toFloat()
        val webs = if(days < 3f) 0f else (1.0 - exp(-(days - 2.0) / 5.0)).toFloat()
        val dirty = hours >= 24f
        return AgingState(
            elapsedHours = hours, dustLevel = dust, flowerWitherFactor = wilt,
            needsCleaning = dirty,
            statusHi = if(dirty) "मंदिर में धूल और मुरझाए फूल हैं। पूजा से पहले सफाई करें।" else "मंदिर स्वच्छ एवं पावन है।",
            statusEn = if(dirty) "Dust and wilted flowers have gathered. Clean the temple before worship." else "Your temple is fresh and sacred.",
            cobwebLevel = webs
        )
    }
}
