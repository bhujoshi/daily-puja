package com.worship.nityamandir.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.worship.nityamandir.MainActivity
import com.worship.nityamandir.R
import com.worship.nityamandir.engine.TempleOccasions
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Schedules and delivers the daily 8:00 AM local morning puja notification.
 *
 * Uses Doze-aware [AlarmManager.setAndAllowWhileIdle] chaining so the reminder works
 * 100% offline across Android 8–16 without requiring restricted exact-alarm permissions.
 */
object DailyPujaReminder {
    const val REMINDER_HOUR = 8
    const val REMINDER_MINUTE = 0

    const val CHANNEL_ID = "daily_puja_morning_reminder"
    const val NOTIFICATION_ID = 1080
    private const val ALARM_REQUEST_CODE = 1080
    private const val LAUNCH_REQUEST_CODE = 1081

    const val ACTION_DAILY_PUJA_REMINDER = "com.worship.nityamandir.action.DAILY_PUJA_REMINDER"
    const val EXTRA_FROM_MORNING_REMINDER = "from_morning_puja_reminder"

    const val KEY_REMINDER_ENABLED = "daily_puja_reminder_enabled"
    const val KEY_PERMISSION_PROMPTED = "daily_puja_notif_permission_prompted"
    const val KEY_LAST_NOTIFIED_DATE = "daily_puja_last_notified_date"
    const val KEY_LAST_COMPLETED_LOCAL_DATE = "daily_puja_completed_local_date"

    private const val WORSHIP_PREFS = "worship"
    private const val DEVOTION_PREFS = "devotion"

    data class Copy(
        val channelName: String,
        val channelDescription: String,
        val title: String,
        val body: String,
        val occasionId: String? = null
    )

    /**
     * Calculates the next 8:00 AM trigger timestamp in [now]'s local timezone.
     * Resolves via [LocalDate] + [LocalTime] in [ZonedDateTime.of] so DST transitions
     * always align to 8:00 AM wall-clock time.
     */
    fun nextTriggerTime(
        now: ZonedDateTime,
        hour: Int = REMINDER_HOUR,
        minute: Int = REMINDER_MINUTE,
        skipToday: Boolean = false
    ): ZonedDateTime {
        val targetTime = LocalTime.of(hour, minute)
        if (!skipToday) {
            val todayCandidate = ZonedDateTime.of(now.toLocalDate(), targetTime, now.zone)
            if (todayCandidate.isAfter(now)) {
                return todayCandidate
            }
        }
        return ZonedDateTime.of(now.toLocalDate().plusDays(1), targetTime, now.zone)
    }

    /**
     * Pure eligibility check for displaying today's morning puja reminder.
     * Suppresses duplicate reminders on the same calendar day and skips notifying
     * devotees who have already completed their morning puja earlier today.
     */
    fun shouldShowNotification(
        reminderEnabled: Boolean,
        notificationsPermitted: Boolean,
        todayIsoDate: String,
        lastNotifiedIsoDate: String?,
        completedDates: Set<String>
    ): Boolean {
        if (!reminderEnabled || !notificationsPermitted) return false
        if (lastNotifiedIsoDate == todayIsoDate) return false
        if (todayIsoDate in completedDates) return false
        return true
    }

    /** Builds culturally reverent bilingual Hindi/English notification copy for [today]. */
    fun buildCopy(hindi: Boolean, today: LocalDate): Copy {
        val channelName = if (hindi) {
            "प्रातः दैनिक पूजा स्मरण"
        } else {
            "Morning Daily Puja Reminder"
        }
        val channelDescription = if (hindi) {
            "प्रतिदिन प्रातः ८:०० बजे नित्य पूजा एवं आरती का पावन स्मरण"
        } else {
            "Gentle daily 8:00 AM reminder for morning puja and aarti"
        }

        val occasion = TempleOccasions.activeOccasion(today)
        if (occasion != null) {
            return if (hindi) {
                Copy(
                    channelName = channelName,
                    channelDescription = channelDescription,
                    title = "🪔 ${occasion.badgeHi} · प्रातः पूजा का समय",
                    body = "${occasion.titleHi} में दीप प्रज्वलित करें और नित्य पूजा एवं आरती से दिन का मंगलमय शुभारंभ करें।",
                    occasionId = occasion.id
                )
            } else {
                Copy(
                    channelName = channelName,
                    channelDescription = channelDescription,
                    title = "🪔 ${occasion.badgeEn} · Time for Morning Puja",
                    body = "Light the sacred diya in your ${occasion.titleEn} and begin your morning with daily puja and aarti.",
                    occasionId = occasion.id
                )
            }
        }

        return if (hindi) {
            Copy(
                channelName = channelName,
                channelDescription = channelDescription,
                title = "🪔 प्रातः पूजा का पावन समय",
                body = "प्रभात मंगलमय हो! अपने पवित्र मंदिर में दीप प्रज्वलित करें और नित्य पूजा एवं आरती से दिन का शुभारंभ करें।"
            )
        } else {
            Copy(
                channelName = channelName,
                channelDescription = channelDescription,
                title = "🪔 Time for Morning Puja",
                body = "Blessed morning! Light the sacred diya in Pavitra Mandir and begin your day with daily puja and aarti."
            )
        }
    }

    fun isEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_REMINDER_ENABLED, true)

    fun isEnabled(context: Context): Boolean =
        isEnabled(worshipPrefs(context))

    fun setEnabled(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        worshipPrefs(appContext).edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()
        if (!enabled) {
            runCatching { NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID) }
        }
        syncSchedule(appContext)
    }

    fun needsRuntimePermissionRequest(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED

    fun hasNotificationPermission(context: Context): Boolean {
        if (needsRuntimePermissionRequest(context)) return false
        return runCatching {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }.getOrDefault(false)
    }

    fun markPermissionPrompted(prefs: SharedPreferences) {
        prefs.edit().putBoolean(KEY_PERMISSION_PROMPTED, true).apply()
    }

    fun wasPermissionPrompted(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_PERMISSION_PROMPTED, false)

    /**
     * Records today's local date when a devotee completes puja so an early-morning puja
     * suppresses that morning's 8:00 AM reminder (and clears any active reminder card).
     */
    fun recordPujaCompletedToday(
        context: Context,
        today: LocalDate = LocalDate.now(ZoneId.systemDefault())
    ) {
        val appContext = context.applicationContext
        worshipPrefs(appContext).edit()
            .putString(KEY_LAST_COMPLETED_LOCAL_DATE, today.toString())
            .apply()
        runCatching { NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID) }
    }

    fun collectCompletedDates(context: Context): Set<String> {
        val dates = mutableSetOf<String>()
        worshipPrefs(context).getString(KEY_LAST_COMPLETED_LOCAL_DATE, null)
            ?.takeIf { it.isNotBlank() }
            ?.let(dates::add)

        val devotion = context.getSharedPreferences(DEVOTION_PREFS, Context.MODE_PRIVATE)
        devotion.getString("pending_day", null)
            ?.takeIf { it.isNotBlank() }
            ?.let(dates::add)
        devotion.all.forEach { (key, value) ->
            if (key.startsWith("completed_days_") && value is Set<*>) {
                value.filterIsInstance<String>().forEach(dates::add)
            }
        }
        return dates
    }

    fun ensureNotificationChannel(
        context: Context,
        hindi: Boolean = worshipPrefs(context).getBoolean("hindi", true),
        today: LocalDate = LocalDate.now(ZoneId.systemDefault())
    ) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val copy = buildCopy(hindi, today)
        val channel = NotificationChannel(
            CHANNEL_ID,
            copy.channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = copy.channelDescription
            enableLights(true)
            lightColor = 0xFFFF9E2C.toInt()
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Ensures the next 8:00 AM local alarm is scheduled when enabled, or cancelled when disabled.
     */
    fun syncSchedule(context: Context, forceTomorrow: Boolean = false) {
        val appContext = context.applicationContext
        val prefs = worshipPrefs(appContext)
        val hindi = prefs.getBoolean("hindi", true)
        ensureNotificationChannel(appContext, hindi)

        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = reminderBroadcastPendingIntent(appContext)

        if (!isEnabled(prefs)) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val alreadyNotifiedToday =
            prefs.getString(KEY_LAST_NOTIFIED_DATE, null) == now.toLocalDate().toString()
        val nextTrigger = nextTriggerTime(
            now = now,
            skipToday = forceTomorrow || alreadyNotifiedToday
        )
        runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextTrigger.toInstant().toEpochMilli(),
                pendingIntent
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun deliverReminderIfDue(context: Context, forceForTest: Boolean = false): Boolean {
        val appContext = context.applicationContext
        val prefs = worshipPrefs(appContext)
        val today = LocalDate.now(ZoneId.systemDefault())
        val todayIso = today.toString()
        val permitted = hasNotificationPermission(appContext)

        val shouldShow = if (forceForTest) {
            permitted
        } else {
            shouldShowNotification(
                reminderEnabled = isEnabled(prefs),
                notificationsPermitted = permitted,
                todayIsoDate = todayIso,
                lastNotifiedIsoDate = prefs.getString(KEY_LAST_NOTIFIED_DATE, null),
                completedDates = collectCompletedDates(appContext)
            )
        }
        if (!shouldShow) return false

        val hindi = prefs.getBoolean("hindi", true)
        ensureNotificationChannel(appContext, hindi, today)
        val copy = buildCopy(hindi, today)

        val openAppIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_FROM_MORNING_REMINDER, true)
        }
        val contentIntent = PendingIntent.getActivity(
            appContext,
            LAUNCH_REQUEST_CODE,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_diya)
            .setColor(0xFFD85816.toInt())
            .setContentTitle(copy.title)
            .setContentText(copy.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(copy.body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        return runCatching {
            NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
            if (!forceForTest) {
                prefs.edit().putString(KEY_LAST_NOTIFIED_DATE, todayIso).apply()
                MandirAnalytics.from(appContext).logMorningReminderShown(copy.occasionId)
            }
            true
        }.getOrDefault(false)
    }

    fun openAppNotificationSettings(context: Context) {
        val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(settingsIntent)
        } catch (_: Exception) {
            val detailsIntent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(detailsIntent) }
        }
    }

    private fun reminderBroadcastPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyPujaReminderReceiver::class.java).apply {
            action = ACTION_DAILY_PUJA_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun worshipPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(WORSHIP_PREFS, Context.MODE_PRIVATE)
}
