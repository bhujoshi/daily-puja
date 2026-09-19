package com.worship.nityamandir.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.zIndex
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
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
import io.github.sceneview.math.Scale
import io.github.sceneview.math.toQuaternion
import kotlinx.coroutines.yield
import kotlin.math.sin

class FlowerFlight(val id: Long, val offering: FlowerOffering) {
    val progress = androidx.compose.animation.core.Animatable(0f)
}

@Composable
fun MandirAltarView(
    session: WorshipSession,
    dustLevel: Float,
    flowerWitherFactor: Float,
    bathTarget: Int?, bathProgress: Float,
    flowerFlights: List<FlowerFlight>,
    aartiRunning: Boolean, aartiProgress: Float,
    onDeityClick: (Int) -> Unit, onDiyaClick: () -> Unit, onBellClick: () -> Unit,
    bellRunning: Boolean, bellProgress: Float,
    conchRunning: Boolean, conchProgress: Float,
    prasadRunning: Boolean, prasadProgress: Float,
    onFlowerClick: (Int) -> Unit, onConchClick: () -> Unit, onAartiClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val loader = rememberModelLoader(engine)
    val camera = rememberCameraNode(engine) { position = Position(z=3f) }
    val nodes = rememberNodes()
    val flowers = remember { mutableListOf<ModelNode>() }
    var modelsReady by remember { mutableStateOf(false) }
    val aartiActive by rememberUpdatedState(aartiRunning)
    val aartiTime by rememberUpdatedState(aartiProgress)
    val offerings = remember { mutableListOf<ModelNode>() }
    val laddus = remember { mutableListOf<ModelNode>() }
    var conchRestScale by remember { mutableStateOf(Scale(1f)) }
    var aartiNode by remember { mutableStateOf<ModelNode?>(null) }
    var bellNode by remember { mutableStateOf<ModelNode?>(null) }
    var conchNode by remember { mutableStateOf<ModelNode?>(null) }
    val bellActive by rememberUpdatedState(bellRunning)
    val bellTime by rememberUpdatedState(bellProgress)
    val conchActive by rememberUpdatedState(conchRunning)
    val conchTime by rememberUpdatedState(conchProgress)
    val sessionNow by rememberUpdatedState(session)
    val flightsNow by rememberUpdatedState(flowerFlights)
    val flyingNodes = remember { mutableMapOf<Long, ModelNode>() }
    val prasadActive by rememberUpdatedState(prasadRunning)
    val prasadTime by rememberUpdatedState(prasadProgress)

    LaunchedEffect(loader) {
        // Instances share geometry and textures; replenishing the plate must not reload GLBs.
        val flowerInstances=listOf("sunflower","single_peony_flower").associateWith {
            loader.createInstancedModel("models/$it.glb",TempleSceneLayout.FLOWER_COUNT/2).toMutableList()
        }
        fun model(file: String, point: TemplePoint, units: Float, tilt: Float=0f, z: Float=0f): ModelNode {
            return ModelNode(flowerInstances[file]?.removeAt(0) ?: loader.createModelInstance("models/$file.glb"),scaleToUnits=units).apply {
                position = Position(2*point.x-1,1-2*point.y,z)
                rotation = Rotation(x=tilt)
                isTouchable=false
                nodes.add(this)
            }
        }
        model("golden_oil_lamp",TempleSceneLayout.oil,TempleSceneLayout.OIL_SIZE,12f)
        model("royal_side_plate",TempleSceneLayout.plate,TempleSceneLayout.PLATE_SIZE,32f)
        aartiNode=model("golden_aarti_lamp",TempleSceneLayout.aartiRest,TempleSceneLayout.AARTI_MODEL_SIZE,20f,TempleSceneLayout.AARTI_DEPTH).apply {
            // Apply yaw before the viewing tilt so the broad bowl points up toward
            // the idols and the curved handle extends down toward the worshipper.
            quaternion=Rotation(x=TempleSceneLayout.AARTI_TILT).toQuaternion() *
                Rotation(y=TempleSceneLayout.AARTI_YAW).toQuaternion()
        }
        bellNode=model("hindu_temple_bell",TempleSceneLayout.bell,.22f,10f)
        conchNode=model("sankh",TempleSceneLayout.conch,TempleSceneLayout.CONCH_SIZE,0f,TempleSceneLayout.CONCH_REST_DEPTH).apply {
            conchRestScale=scale
            rotation=Rotation(y=-90f)
        }
        loader.createInstancedModel("models/tirupathi_laddu.glb",TempleSceneLayout.LADDU_COUNT).forEachIndexed { i,instance ->
            val point=TempleSceneLayout.plateLaddu(i)
            val node=ModelNode(instance,scaleToUnits=TempleSceneLayout.LADDU_SIZE).apply {
                position=Position(2*point.x-1,1-2*point.y,.25f)
                isTouchable=false
            }
            nodes.add(node);laddus.add(node)
        }
        yield()
        repeat(TempleSceneLayout.FLOWER_COUNT) { i ->
            flowers.add(model(if(i%2==0) "sunflower" else "single_peony_flower",TempleSceneLayout.plateFlower(i),TempleSceneLayout.flowerSize(i),45f+(i%4)*8f,.15f+i*.001f))
            yield()
        }
        modelsReady=true
    }

    // Keep offering history while rendering a bounded, neatly arranged flower bed.
    LaunchedEffect(session.offeredFlowers.size,modelsReady) {
        if(!modelsReady) return@LaunchedEffect
        offerings.forEach { nodes.remove(it);it.destroy() }
        offerings.clear()
        val visibleOfferings=(0..1).flatMap { deity ->
            session.offeredFlowers.filter {it.deity==deity}.takeLast(TempleSceneLayout.OFFERED_FLOWER_SLOTS)
        }
        while(offerings.size<visibleOfferings.size) {
            val offering=visibleOfferings[offerings.size]
            val instance=checkNotNull(loader.createInstance(flowers[offering.flowerIndex].model))
            val node=ModelNode(instance,scaleToUnits=TempleSceneLayout.flowerSize(offering.flowerIndex)).apply {
                position=Position(2*offering.position.x-1,1-2*offering.position.y,.20f)
                rotation=Rotation(x=55f,z=(offering.flowerIndex%3-1)*12f)
                isTouchable=false
            }
            nodes.add(node);offerings.add(node)
        }
    }

    LaunchedEffect(flowerFlights.map {it.id},modelsReady) {
        if(!modelsReady) return@LaunchedEffect
        val activeIds=flowerFlights.map {it.id}.toSet()
        flyingNodes.keys.filter {it !in activeIds}.forEach { id ->
            flyingNodes.remove(id)?.let {nodes.remove(it);it.destroy()}
        }
        flowerFlights.forEach { flight ->
            if(flight.id !in flyingNodes) {
                val index=flight.offering.flowerIndex
                val instance=checkNotNull(loader.createInstance(flowers[index].model))
                val point=TempleSceneLayout.plateFlower(index)
                val node=ModelNode(instance,scaleToUnits=TempleSceneLayout.flowerSize(index)).apply {
                    position=Position(2*point.x-1,1-2*point.y,.35f)
                    isTouchable=false
                }
                nodes.add(node);flyingNodes[flight.id]=node
            }
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val density=LocalDensity.current
        val viewport=remember(maxWidth,maxHeight,density) { with(density) { TempleViewport(maxWidth.toPx(),maxHeight.toPx()) } }
        Image(painterResource(R.drawable.temple_portrait),null,Modifier.fillMaxSize().graphicsLayer {scaleX=TempleViewport.ZOOM;scaleY=TempleViewport.ZOOM;transformOrigin=TransformOrigin(.5f,0f)},alignment=Alignment.TopCenter,contentScale=ContentScale.Crop)
        Scene(Modifier.fillMaxSize(),engine=engine,modelLoader=loader,cameraNode=camera,cameraManipulator=null,childNodes=nodes,isOpaque=false,
            onFrame={ _ ->
                val halfWidth=viewport.width/viewport.imageWidth
                camera.setProjection(Camera.Projection.ORTHO,-halfWidth.toDouble(),halfWidth.toDouble(),(1-2*viewport.height/viewport.imageWidth).toDouble(),1.0,.1,10.0)
                flightsNow.forEach { flight ->
                    flyingNodes[flight.id]?.let { node ->
                        val progress=flight.progress.value
                        val point=TempleSceneLayout.flowerFlight(TempleSceneLayout.plateFlower(flight.offering.flowerIndex),flight.offering.position,progress)
                        node.position=Position(2*point.x-1,1-2*point.y,.35f)
                        node.rotation=Rotation(x=55f,z=progress*220f)
                    }
                }
                laddus.forEachIndexed { i,node ->
                    val point=when {
                        sessionNow.prasadOffered -> TempleSceneLayout.offeredLaddu(i)
                        prasadActive -> TempleSceneLayout.flowerFlight(TempleSceneLayout.plateLaddu(i),TempleSceneLayout.offeredLaddu(i),prasadTime)
                        else -> TempleSceneLayout.plateLaddu(i)
                    }
                    node.position=Position(2*point.x-1,1-2*point.y,if(prasadActive) .40f else .25f)
                    node.rotation=Rotation(y=if(prasadActive) prasadTime*360f else 0f)
                }
                conchNode?.apply {
                    val progress=if(conchActive) conchTime else 0f
                    val lift=TempleSceneLayout.pickup(progress)
                    scale=conchRestScale*(1f+.55f*lift)
                    val point=TempleSceneLayout.conchPosition(progress)
                    position=Position(2*point.x-1,1-2*point.y,TempleSceneLayout.CONCH_REST_DEPTH+.18f*lift)
                    // Upright spiral face while sounding; exactly opposite on the plate.
                    rotation=Rotation(y=-90f+180f*TempleSceneLayout.conchTurn(progress))
                }

                aartiNode?.apply {
                    val point=if(aartiActive) TempleSceneLayout.aartiPosition(aartiTime) else TempleSceneLayout.aartiRest
                    // Keep the entire lamp in front of the plate and offerings.
                    position=Position(2*point.x-1,1-2*point.y,TempleSceneLayout.AARTI_DEPTH)
                }
                bellNode?.apply {
                    val lift=if(bellActive) kotlin.math.min(bellTime/.15f,(1-bellTime)/.15f).coerceIn(0f,1f) else 0f
                    position=Position(2*TempleSceneLayout.bell.x-1,1-2*(TempleSceneLayout.bell.y-.045f*lift),.12f*lift)
                    rotation=Rotation(x=10f,z=sin(bellTime*3.5f*18f*1.5f)*12f*lift)
                }
            })
        if(session.aartiComplete && !aartiRunning) DeityHalosOverlay()
        val aartiPoint=if(aartiRunning) TempleSceneLayout.aartiPosition(aartiProgress) else TempleSceneLayout.aartiRest
        Box(Modifier.fillMaxSize().zIndex(10f)) {
            RitualFlamesOverlay(session.lit,session.aartiLit,aartiPoint)
        }
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
        Box(target(TemplePoint(.455f,.59f),.10f,.16f).clickable(onClickLabel="दीप जलाएँ · Light oil lamp",onClick=onDiyaClick))
        Box(target(TemplePoint(TempleSceneLayout.bell.x-.06f,TempleSceneLayout.bell.y-.08f),.12f,.16f).clickable(onClickLabel="घंटी · Bell",onClick=onBellClick))
        val offerFlower by rememberUpdatedState(onFlowerClick)
        repeat(TempleSceneLayout.FLOWER_COUNT) { i ->
                val point=TempleSceneLayout.plateFlower(i)
                Box(target(TemplePoint(point.x-.0325f,point.y-.0325f),.065f,.065f)
                    .pointerInput(i) {
                        var drag=Offset.Zero
                        detectDragGestures(
                            onDragStart={drag=Offset.Zero},
                            onDragCancel={drag=Offset.Zero},
                            onDragEnd={
                                if(drag.y < -24.dp.toPx() && -drag.y > kotlin.math.abs(drag.x)) offerFlower(i)
                            },
                            onDrag={change,amount -> change.consume();drag+=amount}
                        )
                    }.clickable(onClickLabel="पुष्प ${i+1} · Offer flower ${i+1}") {onFlowerClick(i)})
        }
        Box(target(TemplePoint(TempleSceneLayout.conch.x-.075f,TempleSceneLayout.conch.y-.12f),.15f,.24f).clickable(onClickLabel="शंख · Sound conch",onClick=onConchClick))
        Box(target(TemplePoint(TempleSceneLayout.aartiRest.x-TempleSceneLayout.AARTI_WIDTH/2f,TempleSceneLayout.aartiRest.y-TempleSceneLayout.AARTI_HEIGHT/2f),TempleSceneLayout.AARTI_WIDTH,TempleSceneLayout.AARTI_HEIGHT).clickable(onClickLabel="दीप आरती · Diya aarti",onClick=onAartiClick))
    }
}
