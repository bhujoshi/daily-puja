package com.worship.nityamandir.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// Shared pearl tint and reflections keep temple controls on the same glass material.
val TempleGlassTint = Brush.verticalGradient(
    0f to Color(0xA6FFFDF8),
    .28f to Color(0x80FFFDF8),
    .58f to Color(0x52FAF3E6),
    1f to Color(0x70FFFDF8)
)

// Compact labels and icons need more even frosting over detailed temple artwork.
val TempleGlassControlTint = Brush.verticalGradient(
    0f to Color(0xC2FFFDF8),
    .5f to Color(0xA6FFFDF8),
    1f to Color(0x94FAF3E6)
)

val TempleGlassRim = Brush.linearGradient(
    listOf(Color(0xE6FFFFFF), Color(0x40D8AF52), Color(0xA6FFF5DE))
)

val TempleGlassSheen = Brush.linearGradient(
    0f to Color(0x24FFFFFF),
    .35f to Color.Transparent,
    .72f to Color.Transparent,
    1f to Color(0x14FFFFFF)
)

/** Transparent layers preserve the live scene beneath the controls. */
fun Modifier.templeGlass(shape: Shape, tint: Brush = TempleGlassTint): Modifier =
    shadow(10.dp, shape, ambientColor = Color(0x381A1108), spotColor = Color(0x381A1108))
        .clip(shape)
        .background(tint)
        .background(TempleGlassSheen)
        .border(1.dp, TempleGlassRim, shape)
