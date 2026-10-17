package com.worship.nityamandir.ui.components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Readable account and catalog controls over warm sacred sandalwood surfaces, independent of the scene theme. */
@Composable
fun ShrineControlsTheme(content:@Composable ()->Unit) {
    MaterialTheme(colorScheme=lightColorScheme(
        primary=RitualInk,onPrimary=Color(0xFFFFFDF8),secondary=RitualGold,
        surface=Color(0xFFFAF6EE),onSurface=RitualInk,
        surfaceVariant=Color(0xFFF3E7D3),onSurfaceVariant=Color(0xFF5A4438),
        secondaryContainer=Color(0xFFEBD9BD),onSecondaryContainer=RitualInk,
        outline=Color(0xFFBFA993),error=Color(0xFFA82424)
    ),content=content)
}
