package com.worship.nityamandir.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.*
import com.worship.nityamandir.R
import com.worship.nityamandir.engine.TempleViewport

val RitualInk=Color(0xFF47352C)
val RitualGold=Color(0xFF986D3C)

/** Samples the actual carpet beneath the dock, then adds frosting and reflective edges. */
@Composable
fun RitualGlassPanel(
    modifier:Modifier=Modifier,
    sceneSize:IntSize=IntSize.Zero,
    sceneOrigin:Offset=Offset.Zero,
    content:@Composable ColumnScope.()->Unit
) {
    val shape=RoundedCornerShape(28.dp)
    val backdrop=ImageBitmap.imageResource(R.drawable.temple_portrait)
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(modifier.onGloballyPositioned {origin=it.positionInRoot()-sceneOrigin}
        .shadow(14.dp,shape,ambientColor=Color(0x55462A10),spotColor=Color(0x55462A10))
        .clip(shape).border(1.dp,Brush.linearGradient(listOf(Color(0xF2FFFFFF),Color(0x55FFFFFF),Color(0xDDFFFFFF))),shape)) {
        if(sceneSize.width>0 && sceneSize.height>0) Canvas(Modifier.matchParentSize().blur(18.dp)) {
            val viewport=TempleViewport(sceneSize.width.toFloat(),sceneSize.height.toFloat())
            val factor=backdrop.width/viewport.imageWidth
            val x=((origin.x-viewport.left)*factor).toInt().coerceIn(0,backdrop.width-1)
            val y=(origin.y*factor).toInt().coerceIn(0,backdrop.height-1)
            val w=(size.width*factor).toInt().coerceIn(1,backdrop.width-x)
            val h=(size.height*factor).toInt().coerceIn(1,backdrop.height-y)
            drawImage(backdrop,srcOffset=IntOffset(x,y),srcSize=IntSize(w,h),dstSize=IntSize(size.width.toInt(),size.height.toInt()))
        }
        Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(Color(0xE6FFFFFF),Color(0xC7FFF3DF),Color(0xDDFFFFFF)))))
        Column(Modifier.padding(horizontal=16.dp,vertical=14.dp),verticalArrangement=Arrangement.spacedBy(9.dp),content=content)
    }
}

@Composable
fun RitualChoice(label:String,selected:Boolean,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true) {
    OutlinedButton(onClick,modifier.heightIn(min=48.dp),enabled=enabled,shape=RoundedCornerShape(17.dp),
        border=androidx.compose.foundation.BorderStroke(1.dp,if(selected) Color(0xFFBDA17B) else Color(0xB8CFC5B9)),
        colors=ButtonDefaults.outlinedButtonColors(containerColor=if(selected) Color(0xDDF0E2CC) else Color(0x62FFFFFF),contentColor=RitualInk,disabledContentColor=Color(0xFF887B6D)),
        contentPadding=PaddingValues(horizontal=10.dp,vertical=8.dp)) { Text(if(selected) "✓ $label" else label,fontSize=14.sp) }
}
