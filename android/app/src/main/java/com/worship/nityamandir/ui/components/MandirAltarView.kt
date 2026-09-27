package com.worship.nityamandir.ui.components

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import com.worship.nityamandir.data.ShrineSelection
import com.worship.nityamandir.data.ShrineCatalog
import io.github.sceneview.node.Node
import androidx.compose.ui.platform.LocalContext
import io.github.sceneview.node.ImageNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.math.Size
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
    cobwebLevel: Float = 0f,
    selection: ShrineSelection = ShrineSelection(),
    modifier: Modifier = Modifier
) {
    val engine = rememberEngine()
    val loader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val context = LocalContext.current
    val catalog=remember {ShrineCatalog(context)}
    val single=selection.deityCount==1
    val oilPoint=TemplePoint(if(single) .69f else TempleSceneLayout.oil.x,(if(single) .735f else TempleSceneLayout.oil.y)+selection.altarOffset)
    val conchYaw=if(selection["shankh"]=="original") -90f else 0f
    var prasadNode by remember { mutableStateOf<Node?>(null) }
    var loadError by remember {mutableStateOf(false)}
    val prasadActive by rememberUpdatedState(prasadRunning)
    val prasadTime by rememberUpdatedState(prasadProgress)
    val prasadOffered by rememberUpdatedState(session.prasadOffered)
    val camera = rememberCameraNode(engine) { position = Position(z=3f) }
    val nodes = rememberNodes()
    val flowers = remember { mutableListOf<ModelNode>() }
    val plateFlowerAngles = remember { List(TempleSceneLayout.FLOWER_COUNT) { kotlin.random.Random.nextFloat()*360f } }
    val offeringAngles = remember { mutableListOf<Float>() }
    val wiltNow by rememberUpdatedState(flowerWitherFactor)
    var modelsReady by remember { mutableStateOf(false) }
    val aartiActive by rememberUpdatedState(aartiRunning)
    val aartiTime by rememberUpdatedState(aartiProgress)
    val offerings = remember { mutableListOf<ModelNode>() }
    val offeringScales = remember { mutableListOf<Scale>() }
    var conchRestScale by remember { mutableStateOf(Scale(1f)) }
    var aartiNode by remember { mutableStateOf<ModelNode?>(null) }
    var bellNode by remember { mutableStateOf<ModelNode?>(null) }
    var conchNode by remember { mutableStateOf<ModelNode?>(null) }
    val bellActive by rememberUpdatedState(bellRunning)
    val bellTime by rememberUpdatedState(bellProgress)
    val conchActive by rememberUpdatedState(conchRunning)
    val conchTime by rememberUpdatedState(conchProgress)
    val flightsNow by rememberUpdatedState(flowerFlights)
    val flyingNodes = remember { mutableMapOf<Long, ModelNode>() }

    LaunchedEffect(loader) {
        // Instances share geometry and textures; replenishing the plate must not reload GLBs.
        try {
        val flowerPaths=if(selection["flowers"]=="original") listOf("shrine/flowers/sunflower.glb","shrine/flowers/peony.glb") else listOf(catalog.option("flowers",selection).path)
        val flowerInstances=flowerPaths.associateWith {
            loader.createInstancedModel(it,TempleSceneLayout.FLOWER_COUNT/flowerPaths.size).toMutableList()
        }
        fun model(file: String, point: TemplePoint, units: Float, tilt: Float=0f, z: Float=0f): ModelNode {
            return ModelNode(flowerInstances[file]?.removeAt(0) ?: loader.createModelInstance(file),scaleToUnits=units).apply {
                position = Position(2*point.x-1,1-2*point.y,z)
                rotation = Rotation(x=tilt)
                isTouchable=false
                nodes.add(this)
            }
        }
        model(catalog.option("lamp",selection).path,oilPoint,TempleSceneLayout.OIL_SIZE,12f)
        model("shrine/accessories/plate.glb",TempleSceneLayout.plate,TempleSceneLayout.PLATE_SIZE,32f)
        aartiNode=model(catalog.option("aarti",selection).path,TempleSceneLayout.aartiRest,TempleSceneLayout.AARTI_MODEL_SIZE,20f,TempleSceneLayout.AARTI_DEPTH).apply {
            // Apply yaw before the viewing tilt so the broad bowl points up toward
            // the idols and the curved handle extends down toward the worshipper.
            quaternion=Rotation(x=TempleSceneLayout.AARTI_TILT).toQuaternion() *
                Rotation(y=TempleSceneLayout.AARTI_YAW).toQuaternion()
        }
        bellNode=model("shrine/accessories/bell.glb",TempleSceneLayout.bell,.22f,10f)
        conchNode=model(catalog.option("shankh",selection).path,TempleSceneLayout.conch,TempleSceneLayout.CONCH_SIZE,0f,TempleSceneLayout.CONCH_REST_DEPTH).apply {
            conchRestScale=scale
            rotation=Rotation(y=conchYaw)
        }
        // A textured scene node shares depth ordering with the plate's SurfaceView.
        // A Compose Image above Scene can still be covered by its separate surface.
        val prasadAsset=catalog.option("prasad",selection)
        if(prasadAsset.kind=="model") {
            prasadNode=model(prasadAsset.path,TempleSceneLayout.prasadRest,TempleSceneLayout.PRASAD_SIZE*2f,0f,TempleSceneLayout.PRASAD_DEPTH)
        } else {
            val bowlBitmap=context.assets.open(prasadAsset.path).use {BitmapFactory.decodeStream(it)}
            prasadNode=ImageNode(materialLoader=materialLoader,bitmap=bowlBitmap,
                size=Size(TempleSceneLayout.PRASAD_SIZE*2f,TempleSceneLayout.PRASAD_SIZE*2f*bowlBitmap.height/bowlBitmap.width,0f),normal=Position(0f,0f,1f)).apply {
                val point=TempleSceneLayout.prasadRest
                position=Position(2*point.x-1,1-2*point.y,TempleSceneLayout.PRASAD_DEPTH)
                isTouchable=false;nodes.add(this)
            }
        }
        yield()
        repeat(TempleSceneLayout.FLOWER_COUNT) { i ->
            flowers.add(model(flowerPaths[i%flowerPaths.size],TempleSceneLayout.plateFlower(i),TempleSceneLayout.flowerSize(i),45f+(i%4)*8f,.15f+i*.001f).apply {
                rotation=Rotation(x=45f+(i%4)*8f,z=plateFlowerAngles[i])
            })
            yield()
        }
        modelsReady=true
        } catch(e: kotlinx.coroutines.CancellationException) {throw e}
        catch(_:Exception) {loadError=true}
    }

    // Keep offering history while rendering a bounded, neatly arranged flower bed.
    LaunchedEffect(session.offeredFlowers.size,modelsReady) {
        if(!modelsReady) return@LaunchedEffect
        offerings.forEach { nodes.remove(it);it.destroy() }
        offerings.clear();offeringScales.clear();offeringAngles.clear()
        val visibleOfferings=(0 until selection.deityCount).flatMap { deity ->
            session.offeredFlowers.filter {it.deity==deity}.takeLast(TempleSceneLayout.OFFERED_FLOWER_SLOTS)
        }
        while(offerings.size<visibleOfferings.size) {
            val offering=visibleOfferings[offerings.size]
            val instance=checkNotNull(loader.createInstance(flowers[offering.flowerIndex].model))
            val node=ModelNode(instance,scaleToUnits=TempleSceneLayout.flowerSize(offering.flowerIndex)).apply {
                position=Position(2*offering.position.x-1,1-2*offering.position.y,.20f)
                rotation=Rotation(x=55f,z=offering.rotation)
                isTouchable=false
            }
            nodes.add(node);offerings.add(node);offeringScales.add(node.scale);offeringAngles.add(offering.rotation)
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
        val originalScene=selection["shrine"]=="original" && selection["idols"]=="original"
        if(originalScene) {
            Image(painterResource(R.drawable.temple_portrait),null,Modifier.fillMaxSize().graphicsLayer {scaleX=TempleViewport.ZOOM;scaleY=TempleViewport.ZOOM;transformOrigin=TransformOrigin(.5f,0f)},alignment=Alignment.TopCenter,contentScale=ContentScale.Crop)
        } else {
            val background=remember(selection["shrine"]) {context.assets.open(catalog.option("shrine",selection).path).use {BitmapFactory.decodeStream(it)}.asImageBitmap()}
            Image(background,null,Modifier.fillMaxSize().graphicsLayer {scaleX=TempleViewport.ZOOM;scaleY=TempleViewport.ZOOM;transformOrigin=TransformOrigin(.5f,0f)},alignment=Alignment.TopCenter,contentScale=ContentScale.Crop)
            val idol=remember(selection["idols"]) {context.assets.open(catalog.option("idols",selection).path).use {BitmapFactory.decodeStream(it)}.asImageBitmap()}
            val idolWidth=if(single) .30f else .38f
            val idolHeight=if(single) .34f else .30f
            val p=viewport.pixel(TemplePoint(.5f-idolWidth/2,.69f+selection.altarOffset-idolHeight))
            Image(idol,selection.deityNames(false).joinToString(),with(density) {Modifier.offset(p.x.toDp(),p.y.toDp()).size((viewport.imageWidth*idolWidth).toDp(),(viewport.imageWidth*idolHeight).toDp())},contentScale=ContentScale.Fit,alignment=Alignment.BottomCenter)
        }

        ShrineAgingOverlay(viewport,dustLevel,cobwebLevel)
        Scene(Modifier.fillMaxSize(),engine=engine,modelLoader=loader,cameraNode=camera,cameraManipulator=null,childNodes=nodes,isOpaque=false,
            onFrame={ _ ->
                val halfWidth=viewport.width/viewport.imageWidth
                camera.setProjection(Camera.Projection.ORTHO,-halfWidth.toDouble(),halfWidth.toDouble(),(1-2*viewport.height/viewport.imageWidth).toDouble(),1.0,.1,10.0)
                offerings.forEachIndexed { i,node ->
                    offeringScales.getOrNull(i)?.let { base ->
                        node.scale=Scale(base.x*(1f-.22f*wiltNow),base.y*(1f-.55f*wiltNow),base.z*(1f-.22f*wiltNow))
                        node.rotation=Rotation(x=55f+30f*wiltNow,z=offeringAngles[i])
                    }
                }
                flightsNow.forEach { flight ->
                    flyingNodes[flight.id]?.let { node ->
                        val progress=flight.progress.value
                        val point=TempleSceneLayout.flowerFlight(TempleSceneLayout.plateFlower(flight.offering.flowerIndex),flight.offering.position,progress)
                        node.position=Position(2*point.x-1,1-2*point.y,.35f)
                        val startAngle=plateFlowerAngles[flight.offering.flowerIndex]
                        node.rotation=Rotation(x=55f,z=startAngle+(flight.offering.rotation-startAngle)*progress)
                    }
                }
                prasadNode?.apply {
                    val point=when {
                        prasadActive -> TempleSceneLayout.prasadPosition(prasadTime)
                        prasadOffered -> TempleSceneLayout.prasadFloor
                        else -> TempleSceneLayout.prasadRest
                    }
                    position=Position(2*point.x-1,1-2*point.y,TempleSceneLayout.PRASAD_DEPTH)
                }
                conchNode?.apply {
                    val progress=if(conchActive) conchTime else 0f
                    val lift=TempleSceneLayout.pickup(progress)
                    scale=conchRestScale*(1f+.55f*lift)
                    val point=TempleSceneLayout.conchPosition(progress)
                    position=Position(2*point.x-1,1-2*point.y,TempleSceneLayout.CONCH_REST_DEPTH+.18f*lift)
                    // Upright spiral face while sounding; exactly opposite on the plate.
                    rotation=Rotation(y=conchYaw+180f*TempleSceneLayout.conchTurn(progress))
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
        if(session.aartiComplete && !aartiRunning) DeityHalosOverlay(single=single,altarOffset=selection.altarOffset)
        val aartiPoint=if(aartiRunning) TempleSceneLayout.aartiPosition(aartiProgress) else TempleSceneLayout.aartiRest
        Box(Modifier.fillMaxSize().zIndex(10f)) {
            RitualFlamesOverlay(session.lit,session.aartiLit,aartiPoint,oilPoint=oilPoint,traditional=selection["aarti"]!="original",brass=selection["lamp"]!="original")
        }
        WaterFlowOverlay(bathTarget,bathProgress,single=single,altarOffset=selection.altarOffset)
        Canvas(Modifier.fillMaxSize()) {
            fun at(x:Float,y:Float)=viewport.pixel(TemplePoint(x,y)).let {Offset(it.x,it.y)}
            if(0 in session.tilak) drawCircle(Color(0xFFAA2012),viewport.imageWidth*.003f,at(if(single) .50f else .405f,(if(single) .43f else .483f)+selection.altarOffset))
            if(1 in session.tilak) drawCircle(Color(0xFFAA2012),viewport.imageWidth*.0025f,at(.614f,.449f+selection.altarOffset))

        }
        OfferedFlowerAgingOverlay(viewport,flowerWitherFactor,session.offeredFlowers)
        fun target(point:TemplePoint,width:Float,height:Float):Modifier {
            val p=viewport.pixel(point)
            return with(density) { Modifier.offset(p.x.toDp(),p.y.toDp()).size((viewport.imageWidth*width).toDp(),(viewport.imageWidth*height).toDp()) }
        }
        val names=selection.deityNames(false)
        Box(target(TemplePoint(if(single) .35f else .31f,.39f+selection.altarOffset),if(single) .30f else .18f,.30f).clickable(onClickLabel=names[0]) {onDeityClick(0)})
        if(!single) Box(target(TemplePoint(.53f,.39f+selection.altarOffset),.16f,.30f).clickable(onClickLabel=names[1]) {onDeityClick(1)})
        Box(target(TemplePoint(oilPoint.x-.05f,oilPoint.y-.14f),.10f,.22f).clickable(onClickLabel="दीप जलाएँ · Light oil lamp",onClick=onDiyaClick))
        Box(target(TemplePoint(TempleSceneLayout.bell.x-.06f,TempleSceneLayout.bell.y-.08f),.12f,.16f).clickable(onClickLabel="घंटी · Bell",onClick=onBellClick))
        if(loadError) androidx.compose.material3.Text("Some items could not load. Reopen the temple to retry.",Modifier.align(Alignment.Center),color=Color.White)
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
