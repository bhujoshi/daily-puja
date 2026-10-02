package com.worship.nityamandir.ui.components

import android.graphics.BitmapFactory
import com.worship.nityamandir.data.ShrineSelection
import com.worship.nityamandir.data.ShrineCatalog
import io.github.sceneview.node.Node
import androidx.compose.ui.platform.LocalContext
import io.github.sceneview.node.ImageNode
import io.github.sceneview.utils.destroy
import io.github.sceneview.math.Size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.zIndex
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as CanvasSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.android.filament.Camera
import com.worship.nityamandir.engine.*
import io.github.sceneview.Scene
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberNodes
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.toQuaternion
import kotlinx.coroutines.yield
import kotlin.math.sin

/** Asset origins vary. Anchor transformed bounds, including during animation. */
private class ShrineModelNode(instance: com.google.android.filament.gltfio.FilamentInstance, units:Float):
    ModelNode(instance,scaleToUnits=units) {
    var scenePosition=Position()
    var bottomAligned=false
    fun projectedHalfHeight():Float {
        val half=halfExtent*scale
        return kotlin.math.abs((quaternion*Position(half.x,0f,0f)).y)+
            kotlin.math.abs((quaternion*Position(0f,half.y,0f)).y)+
            kotlin.math.abs((quaternion*Position(0f,0f,half.z)).y)
    }
    fun alignBounds() {
        position=scenePosition-quaternion*(center*scale)+Position(y=if(bottomAligned) projectedHalfHeight() else 0f)
    }
}
private fun Node.placePosition(value:Position) {
    if(this is ShrineModelNode) {scenePosition=value;alignBounds()} else position=value
}

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
    modifier: Modifier = Modifier,
    cleanedAreas:List<TemplePoint> = emptyList()
) {
    val sceneView=remember { arrayOfNulls<io.github.sceneview.SceneView>(1) }
    val context = LocalContext.current
    val eglContext=remember {io.github.sceneview.SceneView.createEglContext()}
    val engine=remember {io.github.sceneview.SceneView.createEngine(eglContext)}
    val loader=remember {io.github.sceneview.loaders.ModelLoader(engine,context)}
    val materialLoader=remember {io.github.sceneview.loaders.MaterialLoader(engine,context)}
    val catalog=remember {ShrineCatalog(context)}
    val single=selection.deityCount==1
    val idolPlacement=remember(selection) {IdolPlacement(selection)}
    val oilPoint=idolPlacement.space.lamp
    var oilWick by remember {mutableStateOf(oilPoint)}
    val conchYaw=if(selection["shankh"]=="original") -90f else 0f
    val bathTargetNow by rememberUpdatedState(bathTarget)
    val bathTime by rememberUpdatedState(bathProgress)
    var lotaNode by remember {mutableStateOf<ModelNode?>(null)}
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
    // Update static transforms once after input changes, and once when motion ends.
    val plateDepths=remember {TempleSceneLayout.flowerDepths(
        (0 until TempleSceneLayout.FLOWER_COUNT).map(TempleSceneLayout::plateFlower),
        (0 until TempleSceneLayout.FLOWER_COUNT).map(TempleSceneLayout::flowerSize),.15f)}
    val flightDepths=remember {mutableMapOf<Long,Float>()}
    val frameDirty = remember { booleanArrayOf(true) }
    SideEffect { frameDirty[0] = true }

    LaunchedEffect(loader) {
        // Instances share geometry and textures; replenishing the plate must not reload GLBs.
        try {
        val flowerPaths=catalog.flowerPaths(selection)
        val flowerInstances=flowerPaths.associateWith {
            loader.createInstancedModel(it,(TempleSceneLayout.FLOWER_COUNT+flowerPaths.size-1)/flowerPaths.size).toMutableList()
        }
        fun model(file: String, point: TemplePoint, units: Float, tilt: Float=0f, z: Float=0f): ModelNode {
            return ShrineModelNode(flowerInstances[file]?.removeAt(0) ?: loader.createModelInstance(file),units).apply {
                placePosition(Position(2*point.x-1,1-2*point.y,z))
                rotation = Rotation(x=tilt)
                isTouchable=false
                nodes.add(this)
            }
        }
        (model(catalog.option("lamp",selection).path,oilPoint, .34f,8f) as ShrineModelNode).apply {
            bottomAligned=true
            alignBounds()
            oilWick=oilPoint.copy(y=oilPoint.y-projectedHalfHeight())
        }
        lotaNode=model("shrine/accessories/copper_lota.glb",TemplePoint(.2f,1.0f),.21f,0f,.8f).apply {isVisible=false}
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
                placePosition(Position(2*point.x-1,1-2*point.y,TempleSceneLayout.PRASAD_DEPTH))
                isTouchable=false;nodes.add(this)
            }
        }
        yield()
        repeat(TempleSceneLayout.FLOWER_COUNT) { i ->
            flowers.add(model(flowerPaths[i%flowerPaths.size],TempleSceneLayout.plateFlower(i),TempleSceneLayout.flowerSize(i),45f+(i%4)*8f,plateDepths[i]).apply {
                rotation=Rotation(x=45f+(i%4)*8f,z=plateFlowerAngles[i])
            })
            yield()
        }
        nodes.filterIsInstance<ShrineModelNode>().forEach {it.alignBounds()}
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
        val depths=TempleSceneLayout.flowerDepths(visibleOfferings.map {it.position},
            visibleOfferings.map {TempleSceneLayout.offeredFlowerSize(it.flowerIndex)},.20f)
        while(offerings.size<visibleOfferings.size) {
            val offering=visibleOfferings[offerings.size]
            val instance=checkNotNull(loader.createInstance(flowers[offering.flowerIndex].model))
            val node=ShrineModelNode(instance,TempleSceneLayout.offeredFlowerSize(offering.flowerIndex)).apply {
                placePosition(Position(2*offering.position.x-1,1-2*offering.position.y,depths[offerings.size]))
                rotation=Rotation(x=55f,z=offering.rotation)
                isTouchable=false
            }
            node.alignBounds()
            nodes.add(node);offerings.add(node);offeringScales.add(node.scale);offeringAngles.add(offering.rotation)
        }
        frameDirty[0]=true
    }

    LaunchedEffect(flowerFlights.map {it.id},session.offeredFlowers.size,modelsReady) {
        if(!modelsReady) return@LaunchedEffect
        val activeIds=flowerFlights.map {it.id}.toSet()
        flyingNodes.keys.filter {it !in activeIds}.forEach { id ->
            flightDepths.remove(id)
            flyingNodes.remove(id)?.let {nodes.remove(it);it.destroy()}
        }
        flowerFlights.forEach { flight ->
            if(flight.id !in flyingNodes) {
                val future=(0 until selection.deityCount).flatMap {deity ->
                    (session.offeredFlowers+flight.offering).filter {it.deity==deity}
                        .takeLast(TempleSceneLayout.OFFERED_FLOWER_SLOTS)
                }
                flightDepths[flight.id]=TempleSceneLayout.flowerDepths(future.map {it.position},
                    future.map {TempleSceneLayout.offeredFlowerSize(it.flowerIndex)},.20f)[future.indexOf(flight.offering)]
                val index=flight.offering.flowerIndex
                val instance=checkNotNull(loader.createInstance(flowers[index].model))
                val point=TempleSceneLayout.plateFlower(index)
                val node=ShrineModelNode(instance,TempleSceneLayout.flowerSize(index)).apply {
                    placePosition(Position(2*point.x-1,1-2*point.y,plateDepths[index]))
                    isTouchable=false
                }
                nodes.add(node);flyingNodes[flight.id]=node
            }
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val density=LocalDensity.current
        val viewport=remember(maxWidth,maxHeight,density) { with(density) { TempleViewport(maxWidth.toPx(),maxHeight.toPx()) } }
        // Keep the backdrop, idols and ritual models on the same surface. A transparent
        // SurfaceView below Compose punches through Compose backgrounds as well as models.
        val background=remember(selection["shrine"]) {
            context.assets.open(catalog.option("shrine",selection).path).use {BitmapFactory.decodeStream(it)}
        }
        val idol=remember(selection["idols"]) {
            val bitmap=context.assets.open(catalog.option("idols",selection).path).use {BitmapFactory.decodeStream(it)}
            var left=bitmap.width;var top=bitmap.height;var right=0;var bottom=0
            val pixels=IntArray(bitmap.width*bitmap.height)
            bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
            pixels.forEachIndexed {i,color -> if((color ushr 24)>0) {
                val x=i%bitmap.width;val y=i/bitmap.width
                left=minOf(left,x);right=maxOf(right,x);top=minOf(top,y);bottom=maxOf(bottom,y)
            }}
            android.graphics.Bitmap.createBitmap(bitmap,left,top,right-left+1,bottom-top+1)
        }
        DisposableEffect(materialLoader,background,idol,viewport) {
            val cropScale=maxOf(viewport.width/background.width,viewport.height/background.height)*TempleViewport.ZOOM
            val bgWidth=2f*background.width*cropScale/viewport.imageWidth
            val bgHeight=2f*background.height*cropScale/viewport.imageWidth
            val backdrop=ImageNode(materialLoader=materialLoader,bitmap=background,
                size=Size(bgWidth,bgHeight,0f),normal=Position(0f,0f,1f)).apply {
                position=Position(0f,1f-bgHeight/2f,-2f)
                isTouchable=false
            }
            val fit=minOf(idolPlacement.width/idol.width,idolPlacement.height/idol.height)
            val idolWidth=idol.width*fit
            val idolHeight=idol.height*fit
            val deity=ImageNode(materialLoader=materialLoader,bitmap=idol,
                size=Size(idolWidth*2f,idolHeight*2f,0f),normal=Position(0f,0f,1f)).apply {
                position=Position(2f*(idolPlacement.left+idolPlacement.width/2f)-1f,
                    1f-2f*(idolPlacement.top+idolPlacement.height-idolHeight/2f),-1f)
                isTouchable=false
            }
            nodes.add(backdrop);nodes.add(deity)
            frameDirty[0]=true
            onDispose {
                nodes.remove(backdrop);nodes.remove(deity)
                backdrop.destroy();deity.destroy()
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            fun shadow(point:TemplePoint,width:Float,height:Float) {
                val p=viewport.pixel(point);val u=viewport.imageWidth
                val center=Offset(p.x,p.y)
                // Nested translucent ellipses give a soft contact shadow on the floor.
                repeat(8) {i ->
                    val scale=1f-i*.085f
                    val w=u*width*scale;val h=u*height*scale
                    drawOval(Color(0xFF493321).copy(alpha=.018f),center-Offset(w/2,h/2),CanvasSize(w,h))
                }
            }
            shadow(oilPoint,.12f,.028f)
            shadow(TempleSceneLayout.plate.copy(y=1.19f),.51f,.14f)
            shadow(TempleSceneLayout.bell.copy(y=1.02f),.11f,.024f)
        }
        ShrineAgingOverlay(viewport,dustLevel,cobwebLevel,space=idolPlacement.space,cleaned=cleanedAreas)
        Scene(Modifier.fillMaxSize(),engine=engine,modelLoader=loader,cameraNode=camera,cameraManipulator=null,childNodes=nodes,isOpaque=false,
            onViewCreated={
                sceneView[0]=this
                // Compose doors and glazing must occlude every scene object.
                setZOrderOnTop(false)
                setZOrderMediaOverlay(true)
            },
            onFrame=frame@{ _ ->
                val moving=flightsNow.isNotEmpty() || bathTargetNow!=null ||
                    prasadActive || conchActive || aartiActive || bellActive
                if(!moving && !frameDirty[0]) return@frame
                frameDirty[0]=moving
                val halfWidth=viewport.width/viewport.imageWidth
                camera.setProjection(Camera.Projection.ORTHO,-halfWidth.toDouble(),halfWidth.toDouble(),(1-2*viewport.height/viewport.imageWidth).toDouble(),1.0,.1,10.0)
                offerings.forEachIndexed { i,node ->
                    offeringScales.getOrNull(i)?.let { base ->
                        node.scale=Scale(base.x*(1f-.22f*wiltNow),base.y*(1f-.55f*wiltNow),base.z*(1f-.22f*wiltNow))
                        node.rotation=Rotation(x=55f+30f*wiltNow,z=offeringAngles[i])
                    }
                }
                flowers.forEachIndexed { index,node ->
                    node.isVisible=flightsNow.none {it.offering.flowerIndex==index}
                }
                flightsNow.forEach { flight ->
                    flyingNodes[flight.id]?.let { node ->
                        val progress=flight.progress.value
                        val point=TempleSceneLayout.flowerFlight(TempleSceneLayout.plateFlower(flight.offering.flowerIndex),flight.offering.position,progress)
                        val startDepth=plateDepths[flight.offering.flowerIndex]
                        val endDepth=flightDepths[flight.id] ?: startDepth
                        val depth=startDepth+(endDepth-startDepth)*progress+.4f*sin(kotlin.math.PI.toFloat()*progress)
                        node.placePosition(Position(2*point.x-1,1-2*point.y,depth))
                        val startAngle=plateFlowerAngles[flight.offering.flowerIndex]
                        node.rotation=Rotation(x=55f,z=startAngle+(flight.offering.rotation-startAngle)*progress)
                    }
                }
                lotaNode?.apply {
                    val target=bathTargetNow
                    isVisible=target!=null
                    if(target!=null) {
                        val t=bathTime
                        val lift=kotlin.math.min(t/.12f,(1f-t)/.14f).coerceIn(0f,1f)
                        val source=idolPlacement.bathSource(target)
                        val point=TemplePoint(source.x,source.y-.055f+.10f*(1-lift))
                        // Tip away from the viewer while keeping the pour above the crown.
                        rotation=Rotation(x=-105f*lift)
                        placePosition(Position(2*point.x-1,1-2*point.y,.8f))
                    }
                }
                prasadNode?.apply {
                    val point=when {
                        prasadActive -> idolPlacement.prasadPosition(prasadTime)
                        prasadOffered -> idolPlacement.prasadFloor
                        else -> TempleSceneLayout.prasadRest
                    }
                    placePosition(Position(2*point.x-1,1-2*point.y,TempleSceneLayout.PRASAD_DEPTH))
                }
                conchNode?.apply {
                    val progress=if(conchActive) conchTime else 0f
                    val lift=TempleSceneLayout.pickup(progress)
                    scale=conchRestScale*(1f+.55f*lift)
                    val point=TempleSceneLayout.conchPosition(progress)
                    placePosition(Position(2*point.x-1,1-2*point.y,TempleSceneLayout.CONCH_REST_DEPTH+.18f*lift))
                    // Upright spiral face while sounding; exactly opposite on the plate.
                    rotation=Rotation(y=conchYaw+180f*TempleSceneLayout.conchTurn(progress))
                }

                aartiNode?.apply {
                    val point=if(aartiActive) idolPlacement.aartiPosition(aartiTime) else TempleSceneLayout.aartiRest
                    // Keep the entire lamp in front of the plate and offerings.
                    placePosition(Position(2*point.x-1,1-2*point.y,TempleSceneLayout.AARTI_DEPTH))
                }
                bellNode?.apply {
                    val lift=if(bellActive) kotlin.math.min(bellTime/.15f,(1-bellTime)/.15f).coerceIn(0f,1f) else 0f
                    placePosition(Position(2*TempleSceneLayout.bell.x-1,1-2*(TempleSceneLayout.bell.y-.045f*lift),.12f*lift))
                    rotation=Rotation(x=10f,z=sin(bellTime*3.5f*18f*1.5f)*12f*lift)
                }
                nodes.forEach { if(it is ShrineModelNode) it.alignBounds() }
            })
        if(session.aartiComplete && !aartiRunning) DeityHalosOverlay(heads=idolPlacement.heads)
        val aartiPoint=if(aartiRunning) idolPlacement.aartiPosition(aartiProgress) else TempleSceneLayout.aartiRest
        Box(Modifier.fillMaxSize().zIndex(10f)) {
            RitualFlamesOverlay(session.lit,session.aartiLit,aartiPoint,oilPoint=oilPoint,traditional=selection["aarti"]!="original",brass=selection["lamp"]!="original",oilWick=oilWick)
        }
        WaterFlowOverlay(bathTarget,bathProgress,placement=idolPlacement)
        ScreenWaterDroplets(
            bathTarget=bathTarget, progress=bathProgress, bathed=session.bathed,
            deityCount=session.deityCount, modifier=Modifier.zIndex(11f)
        )
        TilakOverlay(session.tilak,idolPlacement)
        OfferedFlowerAgingOverlay(viewport,flowerWitherFactor,session.offeredFlowers)
        fun target(point:TemplePoint,width:Float,height:Float):Modifier {
            val p=viewport.pixel(point)
            return with(density) { Modifier.offset(p.x.toDp(),p.y.toDp()).size((viewport.imageWidth*width).toDp(),(viewport.imageWidth*height).toDp()) }
        }
        val names=selection.deityNames(false)
        idolPlacement.heads.forEachIndexed {index,head ->
            val width=if(single) idolPlacement.width else idolPlacement.width/2
            Box(target(TemplePoint(head.x-width/2,idolPlacement.top),width,idolPlacement.height)
                .clickable(onClickLabel=names[index]) {onDeityClick(index)})
        }
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
    // AndroidView release may run after remembered loaders have been disposed.
    // Stop native frame callbacks before any model assets are released.
    DisposableEffect(engine) {
        onDispose {
            sceneView[0]?.apply {
                // Detach nodes before releasing assets: a queued frame can otherwise
                // call ModelNode.onFrame on an already destroyed native asset.
                childNodes=emptyList()
                onFrame=null
                destroy()
            };sceneView[0]=null
            // Let Compose dispose Scene, camera and nodes while their engine is alive.
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                loader.destroy()
                materialLoader.destroy()
                engine.destroy()
                eglContext.destroy()
            }
        }
    }

}
