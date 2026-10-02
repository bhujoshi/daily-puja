package com.worship.nityamandir.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.delay
import kotlin.random.Random

private data class ScreenBead(val x:Float,val y:Float,val radius:Float,val stretch:Float,val angle:Float,val arrival:Float)

/** Glass-like beads accumulate across baths and clear only after every idol is bathed. */
@Composable
fun ScreenWaterDroplets(
    bathTarget:Int?, progress:Float, bathed:Set<Int>, deityCount:Int,
    modifier:Modifier=Modifier
) {
    val opacity=remember {Animatable(1f)}
    val allBathed=(0 until deityCount).all {it in bathed}
    LaunchedEffect(allBathed) {
        opacity.snapTo(1f)
        if(allBathed) {
            delay(800)
            opacity.animateTo(0f,tween(5500,easing=LinearEasing))
        }
    }
    val batches=remember(deityCount) {
        List(deityCount) {deity ->
            val random=Random(781+deity*97)
            List(42) {i ->
                ScreenBead(
                    x=.035f+random.nextFloat()*.93f,
                    y=.045f+random.nextFloat()*.91f,
                    radius=when {
                        i%13==0 -> .024f+random.nextFloat()*.012f
                        i%4==0 -> .010f+random.nextFloat()*.007f
                        else -> .0025f+random.nextFloat()*.0055f
                    },
                    stretch=1f+random.nextFloat()*.35f,
                    angle=random.nextFloat()*70f-35f,
                    arrival=.23f+random.nextFloat()*.48f
                )
            }
        }
    }
    // No pointer handlers: the ritual controls remain usable through the overlay.
    Canvas(modifier.fillMaxSize()) {
        val fade=opacity.value
        if(fade<=0f) return@Canvas
        batches.forEachIndexed {deity,beads ->
            val completed=deity in bathed
            if(!completed && deity!=bathTarget) return@forEachIndexed
            beads.forEach {bead ->
                val appear=if(completed) 1f else ((progress-bead.arrival)/.055f).coerceIn(0f,1f)
                if(appear>0f) {
                    val alpha=fade*appear
                    val radius=size.minDimension*bead.radius*(.8f+.2f*appear)
                    val center=Offset(size.width*bead.x,size.height*bead.y)
                    val dimensions=Size(radius*2f,radius*2f*bead.stretch)
                    val corner=center-Offset(dimensions.width/2,dimensions.height/2)
                    val rim=(radius*.10f).coerceAtLeast(size.minDimension*.00065f)
                    rotate(bead.angle,center) {
                        // Transparent centers retain the shrine beneath a dark rim and bright reflections.
                        drawOval(Color.Black.copy(alpha=.14f*alpha),corner+Offset(radius*.13f,radius*.20f),dimensions)
                        drawOval(Brush.radialGradient(listOf(
                            Color(0xFFE2F1F1).copy(alpha=.025f*alpha),
                            Color(0xFFC4E3E5).copy(alpha=.10f*alpha),
                            Color(0xFF203438).copy(alpha=.42f*alpha)
                        ),center-Offset(radius*.18f,radius*.23f),radius*1.35f),corner,dimensions)
                        drawOval(Color(0xFF172C31).copy(alpha=.32f*alpha),corner,dimensions,style=Stroke(rim))
                        drawArc(Color.White.copy(alpha=.78f*alpha),200f,83f,false,
                            corner+Offset(radius*.22f,radius*.18f),
                            Size(radius*1.48f,radius*1.55f*bead.stretch),
                            style=Stroke(rim*1.3f,cap=StrokeCap.Round))
                        drawArc(Color(0xFFDBF4F5).copy(alpha=.53f*alpha),22f,72f,false,
                            corner+Offset(radius*.13f,radius*.12f),
                            Size(radius*1.74f,radius*1.76f*bead.stretch),
                            style=Stroke(rim,cap=StrokeCap.Round))
                        drawCircle(Color.White.copy(alpha=.70f*alpha),radius*.13f,
                            center-Offset(radius*.34f,radius*.40f))
                    }
                }
            }
        }
    }
}
