package com.worship.nityamandir.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import com.worship.nityamandir.engine.TemplePoint
import com.worship.nityamandir.engine.TempleViewport
import kotlin.math.sin
import kotlin.math.hypot

/** Hollow halos leave the photographed faces and crowns untouched. */
@Composable
fun DeityHalosOverlay() {
    var seconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while(true) withFrameNanos { seconds = (it-start)/1_000_000_000f }
    }
    Canvas(Modifier.fillMaxSize()) {
        val viewport = TempleViewport(size.width, size.height)
        val unit = viewport.imageWidth
        fun smooth(value: Float): Float {
            val t = value.coerceIn(0f, 1f)
            return t*t*(3f-2f*t)
        }
        val fade = smooth(seconds/1.8f)
        val spread = smooth((seconds-.4f)/3.6f)
        val shimmer = (.94f + .06f*sin(seconds*1.4f))*fade
        val screenRadius = hypot(size.width, size.height)
        // Light travels outward once and clears completely. A permanent full-screen
        // tint flattens the photograph's shadows and makes the shrine look washed out.
        val waveAlpha = .12f * fade * (1f-smooth((seconds-1.6f)/2.4f))
        listOf(TemplePoint(.405f, .477f), TemplePoint(.614f, .447f)).forEachIndexed { index, head ->
            val pixel = viewport.pixel(head)
            val center = Offset(pixel.x, pixel.y)
            val radius = unit * if(index == 0) .073f else .061f
            if(waveAlpha > .001f) {
                val lightRadius = radius*1.4f + screenRadius*spread
                drawCircle(Brush.radialGradient(
                    0f to Color.Transparent,
                    .70f to Color.Transparent,
                    .86f to Color(0xFFFFEEA8).copy(alpha=waveAlpha),
                    1f to Color.Transparent,
                    center=center, radius=lightRadius), lightRadius, center,
                    blendMode=BlendMode.Screen)
            }
            // The background is a single photograph: mask the head and lower body
            // so the light reads as sitting behind the deity rather than on the face.
            val foreground = Path().apply {
                addOval(Rect(center.x-radius*.74f, center.y-radius*.88f,
                    center.x+radius*.74f, center.y+radius*1.3f))
                addRect(Rect(center.x-radius*1.5f, center.y+radius*.58f,
                    center.x+radius*1.5f, center.y+radius*1.5f))
            }
            clipPath(foreground, ClipOp.Difference) {
                // Keep the lasting bloom close to the rings with a clear center.
                drawCircle(Brush.radialGradient(
                    0f to Color.Transparent,
                    .55f to Color.Transparent,
                    .74f to Color(0xFFFFCA3A).copy(alpha=.30f*shimmer),
                    .88f to Color(0xFFFFDE75).copy(alpha=.13f*shimmer),
                    1f to Color.Transparent,
                    center=center, radius=radius*1.35f), radius*1.35f, center,
                    blendMode=BlendMode.Screen)
                drawCircle(Color(0xFFFFCE43).copy(alpha=.22f*shimmer), radius, center,
                    style=Stroke(unit*.012f), blendMode=BlendMode.Screen)
                drawCircle(Color(0xFFFFE777).copy(alpha=shimmer), radius, center,
                    style=Stroke(unit*.0045f), blendMode=BlendMode.Screen)
                // A fine near-white core makes the ring luminous, not flat yellow.
                drawCircle(Color(0xFFFFFBE3).copy(alpha=shimmer), radius, center,
                    style=Stroke(unit*.0018f), blendMode=BlendMode.Screen)
                drawCircle(Color(0xFFFFEDAA).copy(alpha=.75f*shimmer), radius*1.13f, center,
                    style=Stroke(unit*.0015f), blendMode=BlendMode.Screen)
            }
        }
    }
}
