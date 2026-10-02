package com.worship.nityamandir.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** The leaves swing from a fixed chaukhat, which fades away at the end of opening. */
@Composable
fun TempleDoors(openProgress: Float) {
    val context = LocalContext.current
    val texture = remember(context) {
        context.assets.open("shrine/entrance/teak_door.png").use {
            BitmapFactory.decodeStream(it).asImageBitmap()
        }
    }
    val masks = remember { listOf(doorLeafMask(false), doorLeafMask(true)) }
    val handles = remember { listOf(doorHandleMask(false), doorHandleMask(true)) }
    val frameMask = remember {
        Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(32f, 28f, 992f, 1494f))
            addRect(Rect(100f, 94f, 924f, 1464f))
        }
    }
    val progress = openProgress.coerceIn(0f, 1f)
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val leafWidth = maxWidth * (412f / 960f)
        val leafHeight = maxHeight * (1370f / 1466f)
        val hingeInset = maxWidth * (68f / 960f)
        val topInset = maxHeight * (66f / 1466f)
        if (progress < 1f) repeat(2) { index ->
            val right = index == 1
            Canvas(Modifier.offset(x = hingeInset + if (right) leafWidth else 0.dp, y = topInset)
                .width(leafWidth).height(leafHeight).graphicsLayer {
                    transformOrigin = TransformOrigin(if (right) 1f else 0f, .5f)
                    rotationY = (if (right) 90f else -90f) * progress
                    cameraDistance = 24f * density
                }) {
                // Milky translucent glazing covers every aperture and travels with the leaf.
                drawRect(Color.White.copy(alpha = .78f))
                scale(size.width / 412f, size.height / 1370f, Offset.Zero) {
                    translate(left = if (right) -512f else -100f, top = -94f) {
                        clipPath(masks[index]) { drawImage(texture) }
                        // Handles project over lattice apertures, so restore them above
                        // the glazing instead of letting the aperture mask cut them away.
                        clipPath(handles[index]) { drawImage(texture) }
                    }
                }
            }
        }
        Canvas(Modifier.fillMaxSize().graphicsLayer {
            alpha = ((1f - progress) / .18f).coerceIn(0f, 1f)
        }) {
            scale(size.width / 960f, size.height / 1466f, Offset.Zero) {
                translate(-32f, -28f) {
                    clipPath(frameMask) { drawImage(texture) }
                }
            }
        }
    }
}

/** Source-space apertures let the live shrine show through the photographed joinery. */
private fun doorLeafMask(right: Boolean): Path = Path().apply {
    fillType = PathFillType.EvenOdd
    val shift = if (right) 412f else 0f
    addRect(Rect(100f + shift, 94f, 512f + shift, 1464f))
    val rows = listOf(137f to 206f, 226f to 298f, 320f to 391f,
        413f to 483f, 505f to 578f, 600f to 668f, 692f to 761f,
        786f to 854f, 878f to 947f, 971f to 1044f, 1067f to 1138f,
        1162f to 1231f, 1255f to 1325f, 1348f to 1418f)
    rows.forEachIndexed { row, (top, bottom) ->
        val columns = if (row == 0 || row == 5 || row == 13) 0..3 else listOf(0, 3)
        columns.forEach { column ->
            val left = 140f + column * 89f + shift
            val width = 66f
            addRect(Rect(left, top, left + width, bottom))
            // Retain the actual bell and its fine suspension within each open cell.
            val cx = left + width * .5f
            val cy = (top + bottom) * .5f + 6f
            moveTo(cx - 4f, top)
            lineTo(cx + 4f, top)
            lineTo(cx + 4f, cy - 18f)
            cubicTo(cx + 14f, cy - 17f, cx + 12f, cy - 1f, cx + 20f, cy + 12f)
            lineTo(cx + 20f, cy + 17f)
            lineTo(cx + 5f, cy + 19f)
            lineTo(cx + 4f, cy + 25f)
            lineTo(cx - 4f, cy + 25f)
            lineTo(cx - 5f, cy + 19f)
            lineTo(cx - 20f, cy + 17f)
            lineTo(cx - 20f, cy + 12f)
            cubicTo(cx - 12f, cy - 1f, cx - 14f, cy - 17f, cx - 4f, cy - 18f)
            close()
        }
    }
    // Three scalloped central openings per leaf, as in the supplied doorway.
    listOf(243f to 578f, 708f to 1044f, 1092f to 1325f).forEach { (top, bottom) ->
        val left = 225f + shift
        val rightEdge = 386f + shift
        val center = (left + rightEdge) * .5f
        moveTo(left, bottom)
        lineTo(left, top + 78f)
        cubicTo(left, top + 38f, center - 17f, top + 42f, center, top)
        cubicTo(center + 17f, top + 42f, rightEdge, top + 38f, rightEdge, top + 78f)
        lineTo(rightEdge, bottom)
        close()
    }
}

/** Crescent pulls in source-image coordinates; each stays attached to its door leaf. */
private fun doorHandleMask(right: Boolean): Path = Path().apply {
    fun x(value: Float) = if (right) 1024f - value else value
    moveTo(x(499f), 766f)
    cubicTo(x(432f), 766f, x(432f), 858f, x(499f), 859f)
    lineTo(x(499f), 853f)
    cubicTo(x(471f), 829f, x(472f), 795f, x(499f), 771f)
    close()
}
