package com.worship.nityamandir.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri

/**
 * Tracks daily puja completions and surfaces a gentle, non-intrusive Play Store 5-star review
 * action after a devotee completes their puja, helping build early Play Store ratings & social proof.
 */
object PlayStoreReviewHelper {
    const val KEY_COMPLETED_PUJAS = "completed_pujas_count"
    const val KEY_REVIEW_CLICKED = "play_store_review_clicked"
    const val PLAY_STORE_APP_ID = "com.pavitramandir.app"

    fun recordPujaCompleted(prefs: SharedPreferences): Int {
        val next = prefs.getInt(KEY_COMPLETED_PUJAS, 0) + 1
        prefs.edit().putInt(KEY_COMPLETED_PUJAS, next).apply()
        return next
    }

    fun shouldShowPostPujaReviewPrompt(
        completedPujas: Int,
        reviewAlreadyClicked: Boolean
    ): Boolean = completedPujas >= 1 && !reviewAlreadyClicked

    fun shouldShowPostPujaReviewPrompt(prefs: SharedPreferences): Boolean =
        shouldShowPostPujaReviewPrompt(
            completedPujas = prefs.getInt(KEY_COMPLETED_PUJAS, 0),
            reviewAlreadyClicked = prefs.getBoolean(KEY_REVIEW_CLICKED, false)
        )

    fun markReviewClicked(prefs: SharedPreferences) {
        prefs.edit().putBoolean(KEY_REVIEW_CLICKED, true).apply()
    }

    fun openPlayStoreListing(context: Context, prefs: SharedPreferences) {
        markReviewClicked(prefs)
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$PLAY_STORE_APP_ID")
        ).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
        }
        try {
            context.startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$PLAY_STORE_APP_ID")
            )
            try {
                context.startActivity(webIntent)
            } catch (_: Exception) {
                // Ignore if no browser is installed on the device.
            }
        }
    }
}
