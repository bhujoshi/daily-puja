package com.worship.nityamandir

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.worship.nityamandir.ui.screens.MandirHomeScreen
import com.worship.nityamandir.ui.theme.NityaMandirTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
        setContent {
            NityaMandirTheme {
                MandirHomeScreen()
            }
        }
    }
}
