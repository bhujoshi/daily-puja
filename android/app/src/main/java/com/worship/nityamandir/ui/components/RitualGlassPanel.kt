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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.worship.nityamandir.R
import com.worship.nityamandir.engine.TempleViewport

val RitualInk=Color(0xFF261C16)
val RitualGold=Color(0xFFB3833B)

/** Samples the actual carpet beneath the dock, then adds frosting and reflective edges. */
@Composable
fun RitualGlassPanel(
    modifier:Modifier=Modifier,
    sceneSize:IntSize=IntSize.Zero,
    sceneOrigin:Offset=Offset.Zero,
    backgroundPath:String?=null,
    content:@Composable ColumnScope.()->Unit
) {
    val shape=RoundedCornerShape(28.dp)
    val hostView=LocalView.current
    val context=LocalContext.current
    // The backdrop is blurred with an 18dp radius, so a 4x downsampled cached bitmap
    // preserves visual fidelity while reducing memory footprint by 16x.
    val backdrop=if(backgroundPath==null) ImageBitmap.imageResource(R.drawable.temple_portrait)
        else remember(backgroundPath) {AssetImageCache.image(context,backgroundPath,sampleSize=4)}
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(modifier.onGloballyPositioned {val location=IntArray(2);hostView.getLocationOnScreen(location);origin=it.positionInRoot()+Offset(location[0].toFloat(),location[1].toFloat())-sceneOrigin}
        .shadow(16.dp,shape,ambientColor=Color(0x661A1108),spotColor=Color(0x661A1108))
        .clip(shape).border(1.dp,Brush.linearGradient(listOf(Color(0xFFFAF4E8),Color(0x80D8AF52),Color(0xFFF2E6D0))),shape)) {
        if(sceneSize.width>0 && sceneSize.height>0) Canvas(Modifier.matchParentSize().blur(18.dp)) {
            val viewport=TempleViewport(sceneSize.width.toFloat(),sceneSize.height.toFloat())
            val factor=backdrop.width/viewport.imageWidth
            val x=((origin.x-viewport.left)*factor).toInt().coerceIn(0,backdrop.width-1)
            val y=(origin.y*factor).toInt().coerceIn(0,backdrop.height-1)
            val w=(size.width*factor).toInt().coerceIn(1,backdrop.width-x)
            val h=(size.height*factor).toInt().coerceIn(1,backdrop.height-y)
            drawImage(backdrop,srcOffset=IntOffset(x,y),srcSize=IntSize(w,h),dstSize=IntSize(size.width.toInt(),size.height.toInt()))
        }
        Box(Modifier.matchParentSize().background(Brush.linearGradient(listOf(Color(0xF5FFFDF8),Color(0xEEFAF3E6),Color(0xF5FFFDF8)))))
        Column(Modifier.padding(horizontal=16.dp,vertical=14.dp),verticalArrangement=Arrangement.spacedBy(9.dp),content=content)
    }
}

@Composable
fun RitualChoice(label:String,selected:Boolean,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,leadingIcon:(@Composable ()->Unit)?=null) {
    OutlinedButton(onClick,modifier.heightIn(min=52.dp),enabled=enabled,shape=RoundedCornerShape(17.dp),
        border=androidx.compose.foundation.BorderStroke(if(selected) 1.5.dp else 1.dp,if(selected) RitualGold else Color(0x66B3833B)),
        colors=ButtonDefaults.outlinedButtonColors(containerColor=if(selected) Color(0xFFF0E0C8) else Color(0x99FFFFFF),contentColor=RitualInk,disabledContentColor=Color(0xFF887B6D)),
        contentPadding=PaddingValues(horizontal=10.dp,vertical=8.dp)) {
        if(leadingIcon!=null) {leadingIcon();Spacer(Modifier.width(8.dp))}
        Text(if(selected) "✓ $label" else label,fontSize=16.sp,fontWeight=if(selected) FontWeight.SemiBold else FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}
