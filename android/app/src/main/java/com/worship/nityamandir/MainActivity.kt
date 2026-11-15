package com.worship.nityamandir

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.worship.nityamandir.data.DailyPujaReminder
import com.worship.nityamandir.data.MandirAnalytics
import com.worship.nityamandir.ui.screens.MandirHomeScreen
import com.worship.nityamandir.ui.theme.NityaMandirTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleReminderIntent(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            NityaMandirTheme {
                MandirHomeScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleReminderIntent(intent)
    }

    private fun handleReminderIntent(incoming: Intent?) {
        if (incoming?.getBooleanExtra(DailyPujaReminder.EXTRA_FROM_MORNING_REMINDER, false) == true) {
            incoming.removeExtra(DailyPujaReminder.EXTRA_FROM_MORNING_REMINDER)
            MandirAnalytics.from(this).logMorningReminderOpened()
        }
    }
}
