package com.worship.nityamandir.engine

import com.worship.nityamandir.data.model.Deity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RitualManager {

    // Ritual States

    private val _curtainsClosed = MutableStateFlow(true)
    val curtainsClosed: StateFlow<Boolean> = _curtainsClosed.asStateFlow()

    private val _cleanlinessConfirmed = MutableStateFlow(false)
    val cleanlinessConfirmed: StateFlow<Boolean> = _cleanlinessConfirmed.asStateFlow()

    private val _isDiyaLit = MutableStateFlow(false)
    val isDiyaLit: StateFlow<Boolean> = _isDiyaLit.asStateFlow()

    private val _isDhoopLit = MutableStateFlow(false)
    val isDhoopLit: StateFlow<Boolean> = _isDhoopLit.asStateFlow()

    private val _isAbhishekCompleted = MutableStateFlow(false)
    val isAbhishekCompleted: StateFlow<Boolean> = _isAbhishekCompleted.asStateFlow()

    private val _isDryClothUsed = MutableStateFlow(false)
    val isDryClothUsed: StateFlow<Boolean> = _isDryClothUsed.asStateFlow()

    private val _ganeshDeity = MutableStateFlow(
        Deity(
            id = "ganesh_ji",
            nameHi = "भगवान श्री गणेश",
            nameEn = "Lord Shri Ganesha",
            descriptionHi = "विघ्नहर्ता, प्रथम पूज्य",
            descriptionEn = "Remover of Obstacles, First Worshipped"
        )
    )
    val ganeshDeity: StateFlow<Deity> = _ganeshDeity.asStateFlow()

    private val _lakshmiDeity = MutableStateFlow(
        Deity(
            id = "lakshmi_ji",
            nameHi = "माता महालक्ष्मी",
            nameEn = "Mother Maha Lakshmi",
            descriptionHi = "धन-धान्य, ऐश्वर्य प्रदायिनी",
            descriptionEn = "Goddess of Wealth & Prosperity"
        )
    )
    val lakshmiDeity: StateFlow<Deity> = _lakshmiDeity.asStateFlow()

    private val _bellRingsCount = MutableStateFlow(0)
    val bellRingsCount: StateFlow<Int> = _bellRingsCount.asStateFlow()

    private val _thaliRotationAngle = MutableStateFlow(0f)
    val thaliRotationAngle: StateFlow<Float> = _thaliRotationAngle.asStateFlow()

    private val _isShankhBlown = MutableStateFlow(false)
    val isShankhBlown: StateFlow<Boolean> = _isShankhBlown.asStateFlow()

    private val _isBhogOffered = MutableStateFlow(false)
    val isBhogOffered: StateFlow<Boolean> = _isBhogOffered.asStateFlow()

    private val _isPoojaComplete = MutableStateFlow(false)
    val isPoojaComplete: StateFlow<Boolean> = _isPoojaComplete.asStateFlow()

    private val _streakDays = MutableStateFlow(1)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    // Temple Aging Tracking
    private val _lastWorshipTimeMs = MutableStateFlow<Long?>(null)
    val lastWorshipTimeMs: StateFlow<Long?> = _lastWorshipTimeMs.asStateFlow()

    private val _lastCleanedTimeMs = MutableStateFlow<Long?>(null)
    val lastCleanedTimeMs: StateFlow<Long?> = _lastCleanedTimeMs.asStateFlow()

    // Actions
    fun confirmCleanliness() {
        _cleanlinessConfirmed.value = true
        _curtainsClosed.value = false
    }

    fun toggleCurtains() {
        if (_cleanlinessConfirmed.value) {
            _curtainsClosed.value = !_curtainsClosed.value
        }
    }

    fun lightDiya() {
        _isDiyaLit.value = true
        _isDhoopLit.value = true
    }

    fun completeAbhishek() {
        _isAbhishekCompleted.value = true
        _isDryClothUsed.value = true
    }

    fun applyTilak(deityId: String) {
        if (deityId == "ganesh_ji") {
            _ganeshDeity.value = _ganeshDeity.value.copy(tilakApplied = true)
        } else if (deityId == "lakshmi_ji") {
            _lakshmiDeity.value = _lakshmiDeity.value.copy(tilakApplied = true)
        }
    }

    fun offerFlower(deityId: String) {
        if (deityId == "ganesh_ji") {
            _ganeshDeity.value = _ganeshDeity.value.copy(flowersOfferedCount = _ganeshDeity.value.flowersOfferedCount + 1)
        } else if (deityId == "lakshmi_ji") {
            _lakshmiDeity.value = _lakshmiDeity.value.copy(flowersOfferedCount = _lakshmiDeity.value.flowersOfferedCount + 1)
        }
    }

    fun ringBell() {
        _bellRingsCount.value = _bellRingsCount.value + 1
    }

    fun rotateThali(degreesDelta: Float) {
        _thaliRotationAngle.value = (_thaliRotationAngle.value + degreesDelta) % 360f
    }

    fun blowShankh() {
        _isShankhBlown.value = true
    }

    fun offerBhog() {
        _isBhogOffered.value = true
    }

    fun completeAarti() {
        _isPoojaComplete.value = true
        val now = TimeTravelEngine.getEffectiveCurrentTimeMs()
        _lastWorshipTimeMs.value = now
        _lastCleanedTimeMs.value = now
        _streakDays.value = _streakDays.value + 1
    }

    fun cleanMandir() {
        val now = TimeTravelEngine.getEffectiveCurrentTimeMs()
        _lastCleanedTimeMs.value = now
        // Reset fresh items for new pooja
        _ganeshDeity.value = _ganeshDeity.value.copy(tilakApplied = false, flowersOfferedCount = 0)
        _lakshmiDeity.value = _lakshmiDeity.value.copy(tilakApplied = false, flowersOfferedCount = 0)
        _isDiyaLit.value = false
        _isDhoopLit.value = false
        _isAbhishekCompleted.value = false
        _isBhogOffered.value = false
        _isShankhBlown.value = false
        _bellRingsCount.value = 0
    }

    fun setSimulatedLastWorshipTime(timeMs: Long) {
        _lastWorshipTimeMs.value = timeMs
        _lastCleanedTimeMs.value = timeMs
    }
}
