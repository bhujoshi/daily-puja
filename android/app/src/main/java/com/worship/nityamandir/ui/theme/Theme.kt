package com.worship.nityamandir.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MandirColorScheme = darkColorScheme(
    primary = SacredGold,
    onPrimary = Color.Black,
    primaryContainer = SacredDeepSaffron,
    onPrimaryContainer = SacredGold,
    secondary = SacredBrass,
    onSecondary = Color.Black,
    background = DarkSanctumBackground,
    onBackground = SacredTextPrimary,
    surface = AltarTeakWood,
    onSurface = SacredTextPrimary,
    surfaceVariant = AltarTeakBorder,
    onSurfaceVariant = SacredTextSecondary
)

@Composable
fun NityaMandirTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MandirColorScheme,
        content = content
    )
}
