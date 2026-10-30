package com.worship.nityamandir.engine

import kotlin.math.ceil

enum class MindfulnessPractice(
    val id: String,
    val titleHi: String,
    val titleEn: String,
    val subtitleHi: String,
    val subtitleEn: String
) {
    PRANAYAMA(
        id = "pranayama",
        titleHi = "प्राणायाम",
        titleEn = "Breathing",
        subtitleHi = "साँस की लय के साथ चलें",
        subtitleEn = "Follow a gentle breathing rhythm"
    ),
    TRATAKA(
        id = "trataka",
        titleHi = "त्राटक ध्यान",
        titleEn = "Flame gazing",
        subtitleHi = "दीपक की लौ पर ध्यान दें",
        subtitleEn = "Rest your attention on a diya flame"
    ),
    MANTRA_DHYAN(
        id = "mantra_dhyan",
        titleHi = "मंत्र ध्यान",
        titleEn = "Mantra or silence",
        subtitleHi = "मंत्र दोहराएँ या मौन में बैठें",
        subtitleEn = "Repeat a mantra or sit quietly"
    );

    fun title(hindi: Boolean): String = if (hindi) titleHi else titleEn
    fun subtitle(hindi: Boolean): String = if (hindi) subtitleHi else subtitleEn
}

enum class BreathingPattern(
    val id: String,
    val titleHi: String,
    val titleEn: String,
    val descHi: String,
    val descEn: String,
    val inhaleSec: Int,
    val holdInSec: Int,
    val exhaleSec: Int,
    val holdOutSec: Int
) {
    SAMA_VRITTI(
        id = "sama_vritti",
        titleHi = "समान लय · 4–4–4–4",
        titleEn = "Equal breathing · 4–4–4–4",
        descHi = "4 सेकंड साँस लें, रोकें, छोड़ें, फिर ठहरें",
        descEn = "Breathe in, hold, breathe out, rest — 4 seconds each",
        inhaleSec = 4,
        holdInSec = 4,
        exhaleSec = 4,
        holdOutSec = 4
    ),
    NADI_SHODHANA(
        id = "nadi_shodhana",
        titleHi = "लंबी साँस छोड़ें · 4–6–8",
        titleEn = "Long exhale · 4–6–8",
        descHi = "4 सेकंड साँस लें, 6 रोकें, 8 में छोड़ें",
        descEn = "Breathe in for 4 seconds, hold for 6, out for 8",
        inhaleSec = 4,
        holdInSec = 6,
        exhaleSec = 8,
        holdOutSec = 0
    ),
    SHANTI_PRANA(
        id = "shanti_prana",
        titleHi = "सहज साँस · 5–5",
        titleEn = "Gentle breathing · 5–5",
        descHi = "5 सेकंड साँस लें, 5 में छोड़ें। साँस न रोकें।",
        descEn = "Breathe in for 5 seconds, out for 5. No holds.",
        inhaleSec = 5,
        holdInSec = 0,
        exhaleSec = 5,
        holdOutSec = 0
    );

    val cycleSeconds: Int get() = inhaleSec + holdInSec + exhaleSec + holdOutSec
    fun title(hindi: Boolean): String = if (hindi) titleHi else titleEn
    fun description(hindi: Boolean): String = if (hindi) descHi else descEn
}

enum class BreathPhase(
    val labelHi: String,
    val labelEn: String,
    val hintHi: String,
    val hintEn: String
) {
    INHALE(
        labelHi = "साँस लें",
        labelEn = "Breathe in",
        hintHi = "धीरे से साँस लें",
        hintEn = "Breathe in slowly"
    ),
    HOLD_IN(
        labelHi = "साँस रोकें",
        labelEn = "Hold",
        hintHi = "आराम से साँस रोकें",
        hintEn = "Hold gently"
    ),
    EXHALE(
        labelHi = "साँस छोड़ें",
        labelEn = "Breathe out",
        hintHi = "धीरे से साँस छोड़ें",
        hintEn = "Breathe out slowly"
    ),
    HOLD_OUT(
        labelHi = "ठहरें",
        labelEn = "Rest",
        hintHi = "अगली साँस से पहले थोड़ा ठहरें",
        hintEn = "Pause before your next breath"
    );

    fun label(hindi: Boolean): String = if (hindi) labelHi else labelEn
    fun hint(hindi: Boolean): String = if (hindi) hintHi else hintEn
}

enum class TratakaPhase(
    val labelHi: String,
    val labelEn: String,
    val instructionHi: String,
    val instructionEn: String
) {
    OPEN_GAZE(
        labelHi = "लौ को देखें",
        labelEn = "Look at the flame",
        instructionHi = "लौ को आराम से देखें। ज़रूरत हो तो पलकें झपकाएँ।",
        instructionEn = "Look softly at the flame. Blink whenever you need to."
    ),
    INNER_VISION(
        labelHi = "आँखें बंद करें",
        labelEn = "Close your eyes",
        instructionHi = "आँखें बंद करके लौ की कल्पना करें",
        instructionEn = "Close your eyes and picture the flame"
    );

    fun label(hindi: Boolean): String = if (hindi) labelHi else labelEn
    fun instruction(hindi: Boolean): String = if (hindi) instructionHi else instructionEn
}

enum class SacredMantra(
    val id: String,
    val titleHi: String,
    val titleEn: String,
    val emblem: String,
    val sanskrit: String,
    val transliteration: String,
    val meaningHi: String,
    val meaningEn: String
) {
    OM_PRANAVA(
        id = "om_pranava",
        titleHi = "ॐ",
        titleEn = "Om",
        emblem = "ॐ",
        sanskrit = "ॐ शांतिः शांतिः शांतिः",
        transliteration = "Om Shantih Shantih Shantih",
        meaningHi = "मन ही मन ॐ शांति दोहराएँ",
        meaningEn = "Repeat Om Shantih quietly"
    ),
    GAYATRI(
        id = "gayatri",
        titleHi = "गायत्री ध्यान",
        titleEn = "Gayatri Mantra",
        emblem = "सविता",
        sanskrit = "ॐ भूर्भुवः स्वः तत्सवितुर्वरेण्यं भर्गो देवस्य धीमहि धियो यो नः प्रचोदयात्",
        transliteration = "Om Bhur Bhuvah Svah Tat Savitur Varenyam Bhargo Devasya Dhimahi Dhiyo Yo Nah Prachodayat",
        meaningHi = "अपनी गति से गायत्री मंत्र दोहराएँ",
        meaningEn = "Repeat the Gayatri mantra at your own pace"
    ),
    SOHAM(
        id = "soham",
        titleHi = "सोऽहम् अजपा",
        titleEn = "So'ham Breath",
        emblem = "सोऽहम्",
        sanskrit = "सोऽहम् · हंसः",
        transliteration = "So'ham · I am That",
        meaningHi = "साँस लेते समय 'सो', छोड़ते समय 'हम्' मन में कहें",
        meaningEn = "Think 'So' as you breathe in, 'Ham' as you breathe out"
    ),
    MAUNA(
        id = "mauna",
        titleHi = "मौन",
        titleEn = "Silence",
        emblem = "मौन",
        sanskrit = "शांतं शिवम् अद्वैतम्",
        transliteration = "Shantam Shivam Advaitam",
        meaningHi = "शांत बैठें और अपनी साँस पर ध्यान दें",
        meaningEn = "Sit quietly and notice your breath"
    );

    fun title(hindi: Boolean): String = if (hindi) titleHi else titleEn
    fun meaning(hindi: Boolean): String = if (hindi) meaningHi else meaningEn
}

data class MindfulnessSnapshot(
    val elapsedMs: Long,
    val totalDurationSeconds: Int,
    val remainingSeconds: Int,
    val sessionProgress: Float,
    val completed: Boolean,
    val breathPhase: BreathPhase,
    val breathPhaseRemainingSeconds: Int,
    val breathPhaseProgress: Float,
    val lotusScale: Float,
    val completedBreathCycles: Int,
    val tratakaPhase: TratakaPhase,
    val tratakaPhaseRemainingSeconds: Int,
    val bellCueIndex: Int
)

object MindfulnessEngine {
    val PRESET_DURATIONS_MINUTES = listOf(1, 3, 5, 10, 15)
    const val TRATAKA_OPEN_GAZE_SECONDS = 40
    const val TRATAKA_INNER_VISION_SECONDS = 20
    const val TRATAKA_CYCLE_SECONDS = TRATAKA_OPEN_GAZE_SECONDS + TRATAKA_INNER_VISION_SECONDS
    const val MANTRA_BELL_INTERVAL_SECONDS = 60
    const val MALA_BEADS = 108

    private const val MIN_LOTUS_SCALE = 0.56f
    private const val MAX_LOTUS_SCALE = 1.0f

    fun evaluate(
        elapsedMs: Long,
        totalDurationSeconds: Int,
        practice: MindfulnessPractice,
        breathingPattern: BreathingPattern = BreathingPattern.SAMA_VRITTI
    ): MindfulnessSnapshot {
        val safeTotalSec = totalDurationSeconds.coerceAtLeast(1)
        val totalMs = safeTotalSec * 1000L
        val clampedMs = elapsedMs.coerceIn(0L, totalMs)
        val completed = clampedMs >= totalMs
        val sessionProgress = (clampedMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
        val remainingMs = (totalMs - clampedMs).coerceAtLeast(0L)
        val remainingSeconds = ceil(remainingMs / 1000.0).toInt()

        // 1. Breathwork calculation (used directly in PRANAYAMA and for subtle pulse in MANTRA_DHYAN)
        val pattern = if (practice == MindfulnessPractice.PRANAYAMA) {
            breathingPattern
        } else {
            BreathingPattern.SHANTI_PRANA
        }
        val cycleMs = pattern.cycleSeconds * 1000L
        val completedCycles = (clampedMs / cycleMs).toInt()
        val posInCycleMs = clampedMs % cycleMs

        val inhaleEndMs = pattern.inhaleSec * 1000L
        val holdInEndMs = inhaleEndMs + pattern.holdInSec * 1000L
        val exhaleEndMs = holdInEndMs + pattern.exhaleSec * 1000L

        val breathPhase: BreathPhase
        val phaseProgress: Float
        val phaseRemainingMs: Long
        val lotusScale: Float

        when {
            posInCycleMs < inhaleEndMs -> {
                breathPhase = BreathPhase.INHALE
                val duration = inhaleEndMs.coerceAtLeast(1L)
                phaseProgress = (posInCycleMs.toFloat() / duration).coerceIn(0f, 1f)
                phaseRemainingMs = inhaleEndMs - posInCycleMs
                lotusScale = MIN_LOTUS_SCALE + (MAX_LOTUS_SCALE - MIN_LOTUS_SCALE) * phaseProgress
            }
            posInCycleMs < holdInEndMs -> {
                breathPhase = BreathPhase.HOLD_IN
                val duration = (pattern.holdInSec * 1000L).coerceAtLeast(1L)
                val intoPhase = posInCycleMs - inhaleEndMs
                phaseProgress = (intoPhase.toFloat() / duration).coerceIn(0f, 1f)
                phaseRemainingMs = holdInEndMs - posInCycleMs
                lotusScale = MAX_LOTUS_SCALE
            }
            posInCycleMs < exhaleEndMs -> {
                breathPhase = BreathPhase.EXHALE
                val duration = (pattern.exhaleSec * 1000L).coerceAtLeast(1L)
                val intoPhase = posInCycleMs - holdInEndMs
                phaseProgress = (intoPhase.toFloat() / duration).coerceIn(0f, 1f)
                phaseRemainingMs = exhaleEndMs - posInCycleMs
                lotusScale = MAX_LOTUS_SCALE - (MAX_LOTUS_SCALE - MIN_LOTUS_SCALE) * phaseProgress
            }
            else -> {
                breathPhase = BreathPhase.HOLD_OUT
                val duration = (pattern.holdOutSec * 1000L).coerceAtLeast(1L)
                val intoPhase = posInCycleMs - exhaleEndMs
                phaseProgress = (intoPhase.toFloat() / duration).coerceIn(0f, 1f)
                phaseRemainingMs = cycleMs - posInCycleMs
                lotusScale = MIN_LOTUS_SCALE
            }
        }
        val breathPhaseRemainingSec = ceil(phaseRemainingMs / 1000.0).toInt().coerceAtLeast(1)

        // 2. Trataka phase calculation (40s open-eye gaze, 20s closed-eye inner vision)
        val tratakaCycleMs = TRATAKA_CYCLE_SECONDS * 1000L
        val openGazeMs = TRATAKA_OPEN_GAZE_SECONDS * 1000L
        val tratakaPosMs = clampedMs % tratakaCycleMs
        val tratakaCycleCount = (clampedMs / tratakaCycleMs).toInt()
        val tratakaPhase: TratakaPhase
        val tratakaRemainingMs: Long
        val tratakaSubPhaseIndex: Int
        if (tratakaPosMs < openGazeMs) {
            tratakaPhase = TratakaPhase.OPEN_GAZE
            tratakaRemainingMs = openGazeMs - tratakaPosMs
            tratakaSubPhaseIndex = 0
        } else {
            tratakaPhase = TratakaPhase.INNER_VISION
            tratakaRemainingMs = tratakaCycleMs - tratakaPosMs
            tratakaSubPhaseIndex = 1
        }
        val tratakaRemainingSec = ceil(tratakaRemainingMs / 1000.0).toInt().coerceAtLeast(1)

        // 3. Bell cue index (triggers a soft bell sound when index advances)
        val bellCueIndex = when {
            completed -> 10_000
            practice == MindfulnessPractice.TRATAKA -> tratakaCycleCount * 2 + tratakaSubPhaseIndex
            practice == MindfulnessPractice.MANTRA_DHYAN -> (clampedMs / (MANTRA_BELL_INTERVAL_SECONDS * 1000L)).toInt()
            else -> 0
        }

        return MindfulnessSnapshot(
            elapsedMs = clampedMs,
            totalDurationSeconds = safeTotalSec,
            remainingSeconds = remainingSeconds,
            sessionProgress = sessionProgress,
            completed = completed,
            breathPhase = breathPhase,
            breathPhaseRemainingSeconds = breathPhaseRemainingSec,
            breathPhaseProgress = phaseProgress,
            lotusScale = lotusScale,
            completedBreathCycles = completedCycles,
            tratakaPhase = tratakaPhase,
            tratakaPhaseRemainingSeconds = tratakaRemainingSec,
            bellCueIndex = bellCueIndex
        )
    }

    fun formatTimer(seconds: Int): String {
        val safe = seconds.coerceAtLeast(0)
        val mins = safe / 60
        val secs = safe % 60
        return "%02d:%02d".format(mins, secs)
    }
}
