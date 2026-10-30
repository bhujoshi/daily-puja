package com.worship.nityamandir.data

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.worship.nityamandir.engine.WorshipStep

/**
 * Privacy-preserving product analytics wrapper backed by Firebase Analytics (GA4).
 *
 * Adheres strictly to Pavitra Mandir guardrails (docs/GUARDRAILS.md §2.4):
 * - Never logs PII (no emails, phone numbers, OAuth tokens, or user names).
 * - Advertising ID collection and ad personalization are disabled in AndroidManifest.xml.
 * - Gracefully no-ops when google-services.json is absent or in unit/verification environments.
 */
class MandirAnalytics(private val sink: Sink) {

    interface Sink {
        fun logEvent(name: String, params: Map<String, Any>)
        fun setUserProperty(name: String, value: String?)
    }

    fun logEvent(name: String, params: Map<String, Any> = emptyMap()) {
        val safeName = sanitizeKey(name, MAX_EVENT_NAME_LENGTH)
        if (safeName.isEmpty()) return
        val safeParams = buildMap<String, Any> {
            params.entries.take(MAX_PARAMS_COUNT).forEach { (rawKey, rawValue) ->
                val key = sanitizeKey(rawKey, MAX_PARAM_KEY_LENGTH)
                if (key.isNotEmpty()) {
                    when (rawValue) {
                        is String -> put(key, rawValue.trim().take(MAX_PARAM_VALUE_LENGTH))
                        is Int -> put(key, rawValue.toLong())
                        is Long -> put(key, rawValue)
                        is Float -> if (rawValue.isFinite()) put(key, rawValue.toDouble())
                        is Double -> if (rawValue.isFinite()) put(key, rawValue)
                        is Boolean -> put(key, if (rawValue) "true" else "false")
                        else -> put(key, rawValue.toString().trim().take(MAX_PARAM_VALUE_LENGTH))
                    }
                }
            }
        }
        runCatching { sink.logEvent(safeName, safeParams) }
    }

    fun setUserProperty(name: String, value: String?) {
        val safeName = sanitizeKey(name, MAX_USER_PROPERTY_NAME_LENGTH)
        if (safeName.isEmpty()) return
        val safeValue = value?.trim()?.take(MAX_USER_PROPERTY_VALUE_LENGTH)
        runCatching { sink.setUserProperty(safeName, safeValue) }
    }

    fun syncUserContext(hindi: Boolean, signedIn: Boolean, idolsId: String) {
        setUserProperty("app_language", if (hindi) "hi" else "en")
        setUserProperty("account_type", if (signedIn) "google" else "guest")
        setUserProperty("active_idols", idolsId.ifBlank { "original" })
    }

    fun logScreenView(screenName: String, screenClass: String = "MandirHomeScreen") {
        logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            mapOf(
                FirebaseAnalytics.Param.SCREEN_NAME to screenName,
                FirebaseAnalytics.Param.SCREEN_CLASS to screenClass
            )
        )
    }

    fun logTempleEntered(needsCleaning: Boolean, deityCount: Int, shrineId: String, idolsId: String) {
        logEvent(
            "temple_entered",
            mapOf(
                "needs_cleaning" to needsCleaning,
                "deity_count" to deityCount,
                "shrine_id" to shrineId,
                "idols_id" to idolsId
            )
        )
    }

    fun logTempleCleaned(method: String) {
        logEvent("temple_cleaned", mapOf("method" to method))
    }

    fun logRitualStepCompleted(step: WorshipStep, deityCount: Int) {
        logEvent(
            "ritual_step_completed",
            mapOf(
                "step" to step.name.lowercase(),
                "step_index" to (step.ordinal + 1),
                "deity_count" to deityCount
            )
        )
    }

    fun logBellRung() {
        logEvent("ritual_bell_rung")
    }

    fun logPujaCompleted(
        deityCount: Int,
        shrineId: String,
        idolsId: String,
        streakDays: Int,
        signedIn: Boolean
    ) {
        logEvent(
            "puja_completed",
            mapOf(
                "deity_count" to deityCount,
                "shrine_id" to shrineId,
                "idols_id" to idolsId,
                "streak_days" to streakDays,
                "signed_in" to signedIn
            )
        )
    }

    fun logShrineCategoryViewed(categoryId: String) {
        logEvent("shrine_category_viewed", mapOf("category_id" to categoryId))
    }

    fun logShrinePreviewOpened(shrineId: String, idolsId: String) {
        logEvent(
            "shrine_preview_opened",
            mapOf(
                "shrine_id" to shrineId,
                "idols_id" to idolsId
            )
        )
    }

    fun logShrineItemUnlocked(categoryId: String, itemId: String, costCoins: Int) {
        logEvent(
            "shrine_item_unlocked",
            mapOf(
                "category_id" to categoryId,
                "item_id" to itemId,
                "cost_coins" to costCoins
            )
        )
    }

    fun logShrineSelectionApplied(selection: ShrineSelection) {
        logEvent(
            "shrine_selection_applied",
            mapOf(
                "shrine_id" to selection["shrine"],
                "idols_id" to selection["idols"],
                "lamp_id" to selection["lamps"],
                "flower_count" to selection.flowerIds.size
            )
        )
    }

    fun logBhajanPlayed(
        trackId: String,
        title: String,
        deity: String,
        isYoutube: Boolean,
        autoplay: Boolean
    ) {
        logEvent(
            "bhajan_played",
            mapOf(
                "track_id" to trackId,
                "track_title" to title,
                "deity" to deity.ifBlank { "general" },
                "source" to if (isYoutube) "youtube" else "archive",
                "autoplay" to autoplay
            )
        )
    }

    fun logBhajanSearch(queryLength: Int, resultCount: Int) {
        logEvent(
            "bhajan_search",
            mapOf(
                "query_length" to queryLength,
                "result_count" to resultCount
            )
        )
    }

    fun logLanguageChanged(hindi: Boolean) {
        val lang = if (hindi) "hi" else "en"
        setUserProperty("app_language", lang)
        logEvent("language_changed", mapOf("language" to lang))
    }

    fun logGoogleSignInStarted(referredInstall: Boolean) {
        logEvent("google_sign_in_started", mapOf("referred_install" to referredInstall))
    }

    fun logGoogleSignInResult(outcome: String, reason: String? = null) {
        val params = buildMap<String, Any> {
            put("outcome", outcome)
            if (!reason.isNullOrBlank()) {
                put("reason", reason)
            }
        }
        logEvent("google_sign_in_result", params)
    }

    fun logWhatsAppShareOpened() {
        logEvent("whatsapp_share_opened")
    }

    fun logAccountAction(action: String) {
        logEvent("account_action", mapOf("action" to action))
    }

    fun logMindfulnessStarted(
        practice: String,
        durationMinutes: Int,
        subOption: String
    ) {
        logEvent(
            "mindfulness_started",
            mapOf(
                "practice" to practice,
                "duration_minutes" to durationMinutes,
                "sub_option" to subOption
            )
        )
    }

    fun logMindfulnessCompleted(
        practice: String,
        durationMinutes: Int,
        streakDays: Int,
        totalSessions: Int
    ) {
        logEvent(
            "mindfulness_completed",
            mapOf(
                "practice" to practice,
                "duration_minutes" to durationMinutes,
                "streak_days" to streakDays,
                "total_sessions" to totalSessions
            )
        )
    }

    companion object {
        private const val MAX_EVENT_NAME_LENGTH = 40
        private const val MAX_PARAM_KEY_LENGTH = 40
        private const val MAX_PARAM_VALUE_LENGTH = 100
        private const val MAX_PARAMS_COUNT = 25
        private const val MAX_USER_PROPERTY_NAME_LENGTH = 24
        private const val MAX_USER_PROPERTY_VALUE_LENGTH = 36

        @Volatile
        private var instance: MandirAnalytics? = null

        fun from(context: Context): MandirAnalytics {
            instance?.let { return it }
            return synchronized(this) {
                instance ?: MandirAnalytics(FirebaseSink(context.applicationContext)).also {
                    instance = it
                }
            }
        }

        internal fun sanitizeKey(raw: String, maxLength: Int): String {
            val cleaned = buildString(raw.length) {
                var lastUnderscore = false
                for (ch in raw.trim().lowercase()) {
                    when {
                        ch in 'a'..'z' || ch in '0'..'9' -> {
                            append(ch)
                            lastUnderscore = false
                        }
                        ch == '_' || ch == '-' || ch == ' ' || ch == '.' -> {
                            if (isNotEmpty() && !lastUnderscore) {
                                append('_')
                                lastUnderscore = true
                            }
                        }
                    }
                }
            }.trim('_')
            if (cleaned.isEmpty()) return ""
            val prefixed = if (cleaned[0] in 'a'..'z') cleaned else "e_$cleaned"
            return prefixed.take(maxLength).trimEnd('_')
        }
    }

    private class FirebaseSink(private val appContext: Context) : Sink {
        private val firebaseAnalytics: FirebaseAnalytics? by lazy {
            runCatching {
                if (FirebaseApp.getApps(appContext).isNotEmpty()) {
                    FirebaseAnalytics.getInstance(appContext)
                } else {
                    null
                }
            }.getOrNull()
        }

        override fun logEvent(name: String, params: Map<String, Any>) {
            val analytics = firebaseAnalytics ?: return
            val bundle = Bundle().apply {
                params.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Long -> putLong(key, value)
                        is Int -> putLong(key, value.toLong())
                        is Double -> putDouble(key, value)
                        is Float -> putDouble(key, value.toDouble())
                        is Boolean -> putString(key, if (value) "true" else "false")
                    }
                }
            }
            analytics.logEvent(name, bundle)
        }

        override fun setUserProperty(name: String, value: String?) {
            val analytics = firebaseAnalytics ?: return
            analytics.setUserProperty(name, value)
        }
    }
}
