package com.worship.nityamandir.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.android.filament.Camera
import com.worship.nityamandir.R
import com.worship.nityamandir.engine.*
import io.github.sceneview.Scene
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import kotlinx.coroutines.yield
import kotlin.math.sin

@Composable
fun MandirAltarView(
    session: WorshipSession,
    dustLevel: Float,
    flowerWitherFactor: Float,
    bathTarget: Int?, bathProgress: Float,
    flowerTarget: Int?, flowerProgress: Float,
    aartiRunning: Boolean, aartiProgress: Float,
    onDeityClick: (Int) -> Unit, onDiyaClick: () -> Unit, onBellClick: () -> Unit,
    onPlateClick: () -> Unit, onAartiClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val loader = rememberModelLoader(engine)
    val camera = rememberCameraNode(engine) { position = Position(z=3f) }
    val nodes = rememberNodes()
    val flowers = remember { mutableListOf<ModelNode>() }
    var bellNode by remember { mutableStateOf<ModelNode?>(null) }
    val sessionNow by rememberUpdatedState(session)
    val targetNow by rememberUpdatedState(flowerTarget)
    val flightNow by rememberUpdatedState(flowerProgress)
    val aartiNow by rememberUpdatedState(aartiRunning)
    val aartiTime by rememberUpdatedState(aartiProgress)

    LaunchedEffect(loader) {
        fun model(file: String, point: TemplePoint, units: Float, tilt: Float=0f, z: Float=0f): ModelNode {
            return ModelNode(loader.createModelInstance("models/$file.glb"),scaleToUnits=units).apply {
                position = Position(2*point.x-1,1-2*point.y,z)
                rotation = Rotation(x=tilt)
                isTouchable=false
                nodes.add(this)
            }
        }
        model("golden_oil_lamp",TempleSceneLayout.oil,TempleSceneLayout.OIL_SIZE,12f)
        model("royal_side_plate",TempleSceneLayout.plate,TempleSceneLayout.PLATE_SIZE,32f)
        bellNode=model("hindu_temple_bell",TempleSceneLayout.bell,.22f,10f)
        yield()
        repeat(10) { i ->
            flowers.add(model(if(i%2==0) "sunflower" else "single_peony_flower",TempleSceneLayout.plateFlower(i),.10f+(i%3)*.008f,45f+(i%4)*8f,.15f+i*.001f))
            yield()
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val density=LocalDensity.current
        val viewport=remember(maxWidth,maxHeight,density) { with(density) { TempleViewport(maxWidth.toPx(),maxHeight.toPx()) } }
        Image(painterResource(R.drawable.temple_portrait),null,Modifier.fillMaxSize(),alignment=Alignment.TopCenter,contentScale=ContentScale.Crop)
        Scene(Modifier.fillMaxSize(),engine=engine,modelLoader=loader,cameraNode=camera,cameraManipulator=null,childNodes=nodes,isOpaque=false,
            onFrame={ _ ->
                val halfWidth=viewport.width/viewport.imageWidth
                camera.setProjection(Camera.Projection.ORTHO,-halfWidth.toDouble(),halfWidth.toDouble(),(1-2*viewport.height/viewport.imageWidth).toDouble(),1.0,.1,10.0)
                flowers.forEachIndexed { i,node ->
                    val owner=if(i<4) i/2 else -1
                    val point=when {
                        owner>=0 && owner in sessionNow.flowers -> TempleSceneLayout.feet(owner,i%2)
                        owner>=0 && owner==targetNow -> TempleSceneLayout.flowerFlight(TempleSceneLayout.plateFlower(i),TempleSceneLayout.feet(owner,i%2),flightNow)
                        else -> TempleSceneLayout.plateFlower(i)
                    }
                    node.position=Position(2*point.x-1,1-2*point.y,if(owner==targetNow) .35f else .15f+i*.001f)
                    node.rotation=Rotation(x=55f,z=if(owner==targetNow) flightNow*220f else (i*31f))
                }
                bellNode?.apply {
                    val lift=if(aartiNow) kotlin.math.min(aartiTime/.10f,(1-aartiTime)/.10f).coerceIn(0f,1f) else 0f
                    position=Position(2*TempleSceneLayout.bell.x-1,1-2*(TempleSceneLayout.bell.y-.045f*lift),.12f*lift)
                    rotation=Rotation(x=10f,z=sin(aartiTime*10.5f*18f)*12f*lift)
                }
            })
        val aartiPoint=if(aartiRunning) TempleSceneLayout.aartiPosition(aartiProgress) else TempleSceneLayout.aartiRest
        val lampPixel=viewport.pixel(aartiPoint)
        with(density) {
            Image(painterResource(R.drawable.aarti_diya),"Brass aarti diya",
                Modifier.offset((lampPixel.x-viewport.imageWidth*.10f).toDp(),(lampPixel.y-viewport.imageWidth*.125f).toDp())
                    .size((viewport.imageWidth*.20f).toDp(),(viewport.imageWidth*.25f).toDp()),contentScale=ContentScale.FillBounds)
        }
        RitualFlamesOverlay(session.lit,session.aartiLit,
            if(aartiRunning) TempleSceneLayout.aartiPosition(aartiProgress) else TempleSceneLayout.aartiRest)
        WaterFlowOverlay(bathTarget,bathProgress)
        Canvas(Modifier.fillMaxSize()) {
            fun at(x:Float,y:Float)=viewport.pixel(TemplePoint(x,y)).let {Offset(it.x,it.y)}
            if(0 in session.tilak) drawCircle(Color(0xFFAA2012),viewport.imageWidth*.003f,at(.405f,.483f))
            if(1 in session.tilak) drawCircle(Color(0xFFAA2012),viewport.imageWidth*.0025f,at(.614f,.449f))
            repeat(170) { i -> drawCircle(Color(0xFF918173).copy(alpha=dustLevel*.6f),1.5.dp.toPx(),at((i*71%173)/173f,.64f+(i*37%101)/200f)) }
            if(flowerWitherFactor>0) drawOval(Color(0xFF806D32).copy(alpha=flowerWitherFactor*.4f),at(.38f,1.055f),androidx.compose.ui.geometry.Size(viewport.imageWidth*.24f,viewport.imageWidth*.06f))
        }
        fun target(point:TemplePoint,width:Float,height:Float):Modifier {
            val p=viewport.pixel(point)
            return with(density) { Modifier.offset(p.x.toDp(),p.y.toDp()).size((viewport.imageWidth*width).toDp(),(viewport.imageWidth*height).toDp()) }
        }
        Box(target(TemplePoint(.31f,.43f),.18f,.26f).clickable(onClickLabel="गणेश जी · Ganesha") {onDeityClick(0)})
        Box(target(TemplePoint(.53f,.41f),.16f,.28f).clickable(onClickLabel="लक्ष्मी जी · Lakshmi") {onDeityClick(1)})
        Box(target(TemplePoint(.44f,.59f),.13f,.17f).clickable(onClickLabel="दीप जलाएँ · Light oil lamp",onClick=onDiyaClick))
        Box(target(TemplePoint(.16f,.98f),.12f,.16f).clickable(onClickLabel="घंटी · Bell",onClick=onBellClick))
        Box(target(TemplePoint(.32f,1.00f),.36f,.17f).clickable(onClickLabel="पुष्प अर्पण · Offer flowers",onClick=onPlateClick))
        Box(target(TemplePoint(.70f,.95f),.18f,.25f).clickable(onClickLabel="दीप आरती · Diya aarti",onClick=onAartiClick))
    }
}
