package com.worship.nityamandir.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TimeTravelEngine {
    private val _offsetHours = MutableStateFlow(0f)
    val offsetHours: StateFlow<Float> = _offsetHours.asStateFlow()

    fun getEffectiveCurrentTimeMs(): Long {
        val offsetMs = (_offsetHours.value * 60f * 60f * 1000f).toLong()
        return System.currentTimeMillis() + offsetMs
    }

    fun setTimeOffsetHours(hours: Float) {
        _offsetHours.value = hours
    }

    fun reset() {
        _offsetHours.value = 0f
    }
}
