package com.worship.nityamandir.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives the daily 8:00 AM morning puja alarm as well as device reboot, app update,
 * and clock/timezone change broadcasts so the daily schedule stays continuously armed.
 */
class DailyPujaReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            DailyPujaReminder.ACTION_DAILY_PUJA_REMINDER -> {
                DailyPujaReminder.deliverReminderIfDue(context)
                DailyPujaReminder.syncSchedule(context, forceTomorrow = true)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                DailyPujaReminder.syncSchedule(context)
            }
        }
    }
}
