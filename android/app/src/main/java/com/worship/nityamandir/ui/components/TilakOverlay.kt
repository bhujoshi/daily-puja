package com.worship.nityamandir.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.worship.nityamandir.engine.*

@Composable
fun TilakOverlay(applied:Set<Int>,placement:IdolPlacement) {
    placement.heads.forEachIndexed {index,head ->
        key(index) {
            val reveal=remember {Animatable(0f)}
            LaunchedEffect(index in applied) {if(index in applied) reveal.animateTo(1f,tween(900)) else reveal.snapTo(0f)}
            if(reveal.value>0f) Canvas(Modifier.fillMaxSize()) {
                val view=TempleViewport(size.width,size.height);val u=view.imageWidth
                val point=view.pixel(head);val center=Offset(point.x,point.y)
                val t=reveal.value
                val mark=(placement.height*.018f).coerceIn(.003f,.006f)*u
                drawCircle(Color(0xFFEAC57C).copy(alpha=(1-t)*.5f),mark*(2f+4f*t),center,style=Stroke(u*.001f))
                drawLine(Color(0xFFB8271F),center-Offset(0f,mark),center-Offset(0f,mark*(1-2*t)),mark*.8f,StrokeCap.Round)
                drawCircle(Color(0xFFC73A21),mark*.5f*t,center+Offset(0f,mark*.9f))
            }
        }
    }
}
