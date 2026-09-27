package com.worship.nityamandir.ui.components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Readable account and catalog controls over the cream surfaces, independent of the scene theme. */
@Composable
fun ShrineControlsTheme(content:@Composable ()->Unit) {
    MaterialTheme(colorScheme=lightColorScheme(
        primary=RitualInk,onPrimary=Color.White,secondary=RitualGold,
        surface=Color(0xFFFFF7EC),onSurface=RitualInk,
        surfaceVariant=Color(0xFFF1DFC1),onSurfaceVariant=Color(0xFF645044),
        secondaryContainer=Color(0xFFE8D0A7),onSecondaryContainer=RitualInk,
        outline=Color(0xFF8B7560),error=Color(0xFF9C2525)
    ),content=content)
}
