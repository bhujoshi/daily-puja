package com.worship.nityamandir.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.worship.nityamandir.engine.*
import com.worship.nityamandir.ui.components.*
import kotlinx.coroutines.delay

@Composable
fun MandirHomeScreen(modifier:Modifier=Modifier) {
    val context=LocalContext.current
    val prefs=remember {context.getSharedPreferences("worship",Context.MODE_PRIVATE)}
    val audio=remember {RitualAudio(context)}
    DisposableEffect(Unit) {onDispose {audio.release()}}
    val lifecycle=androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    var foreground by remember {mutableStateOf(lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED))}
    DisposableEffect(lifecycle) {
        val observer=androidx.lifecycle.LifecycleEventObserver { _,event ->
            if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP) {foreground=false;audio.stop()}
            if(event==androidx.lifecycle.Lifecycle.Event.ON_START) foreground=true
        }
        lifecycle.addObserver(observer)
        onDispose {lifecycle.removeObserver(observer)}
    }
    var hindi by remember {mutableStateOf(prefs.getBoolean("hindi",true))}
    var saffron by remember {mutableStateOf(prefs.getBoolean("saffron",false))}
    var menu by remember {mutableStateOf(false)}
    var entered by remember {mutableStateOf(false)}
    val curtain=remember {Animatable(0f)}
    val curtainMoving=curtain.isRunning || curtain.value != (if(entered) 1f else 0f)
    LaunchedEffect(entered) {
        curtain.animateTo(if(entered) 1f else 0f,tween(1600,easing=FastOutSlowInEasing))
    }
    var session by remember {mutableStateOf(WorshipSession())}
    var last by remember {mutableLongStateOf(prefs.getLong("last",System.currentTimeMillis()))}
    var clean by remember {mutableLongStateOf(prefs.getLong("clean",last))}
    var now by remember {mutableLongStateOf(System.currentTimeMillis())}
    var wipe by remember {mutableFloatStateOf(0f)}
    var bathTarget by remember {mutableStateOf<Int?>(null)}
    val flowerFlights=remember {mutableStateListOf<FlowerFlight>()}
    var nextFlowerId by remember {mutableLongStateOf(0L)}
    var prasadRunning by remember {mutableStateOf(false)}
    val prasad=remember {Animatable(0f)}
    var aartiRunning by remember {mutableStateOf(false)}
    var bellRunning by remember {mutableStateOf(false)}
    var conchRunning by remember {mutableStateOf(false)}
    val bell=remember {Animatable(0f)}
    val conch=remember {Animatable(0f)}
    val bath=remember {Animatable(0f)}
    val aarti=remember {Animatable(0f)}
    // Repeat the original 10.5-second motion twice over the 21-second ritual.
    // Keep overall progress separate so the progress bar and completion do not reset.
    val aartiMotion=if(aarti.value>=1f) 1f else (aarti.value*2f)%1f
    var sceneSize by remember {mutableStateOf(IntSize.Zero)}
    var sceneOrigin by remember {mutableStateOf(Offset.Zero)}
    LaunchedEffect(Unit) {while(true) {now=System.currentTimeMillis();delay(30000)}}
    val aging=AgingEngine.calculateAging(last,clean,now)
    val ritualBusy=curtainMoving || bathTarget!=null || aartiRunning || bellRunning || conchRunning || prasadRunning
    val busy=ritualBusy || flowerFlights.isNotEmpty()
    fun tr(hi:String,en:String)=if(hindi) hi else en
    fun closeTemple() {entered=false;menu=false;audio.stop();bathTarget=null;flowerFlights.clear();aartiRunning=false;bellRunning=false;conchRunning=false;prasadRunning=false}
    fun ringBell() {
        if(entered && foreground && !aging.needsCleaning && !busy) bellRunning=true
    }
    fun soundConch() {
        if(entered && foreground && !aging.needsCleaning && !busy) conchRunning=true
    }
    fun offerFlower(index:Int) {
        if(!entered || aging.needsCleaning || ritualBusy || !foreground) return
        val covered=session.flowers+flowerFlights.map {it.offering.deity}
        val target=(listOf(0,1)-covered).randomOrNull() ?: (0..1).random()
        flowerFlights.add(FlowerFlight(nextFlowerId++,FlowerOffering(index,target,TempleSceneLayout.randomFlowerPosition(target))))
    }
    fun light() {if(entered && !aging.needsCleaning && session.step==WorshipStep.LIGHT) {session=session.copy(lit=true);audio.cue(com.worship.nityamandir.R.raw.flower_offering)}}
    fun deityAction(deity:Int) {
        if(!entered || aging.needsCleaning || busy) return
        when(session.step) {
            WorshipStep.BATH -> if(deity !in session.bathed) bathTarget=deity
            WorshipStep.TILAK -> {session=session.copy(tilak=session.tilak+deity);audio.cue(com.worship.nityamandir.R.raw.flower_offering)}
            WorshipStep.FLOWERS -> offerFlower((0 until TempleSceneLayout.FLOWER_COUNT).random())
            else -> Unit
        }
    }
    fun startAarti() {if(entered && !aging.needsCleaning && session.step==WorshipStep.AARTI && !busy && !session.aartiComplete) {session=session.copy(aartiLit=true);aartiRunning=true}}
    LaunchedEffect(bathTarget) {
        val target=bathTarget ?: return@LaunchedEffect
        audio.cue(com.worship.nityamandir.R.raw.water_offering)
        try {bath.snapTo(0f);bath.animateTo(1f,tween(2800,easing=LinearEasing))}
        finally {audio.stopCue(com.worship.nityamandir.R.raw.water_offering)}
        session=session.copy(bathed=session.bathed+target);bathTarget=null
    }
    flowerFlights.toList().forEach { flight ->
        key(flight.id) {
            LaunchedEffect(Unit) {
                audio.cue(com.worship.nityamandir.R.raw.flower_offering)
                flight.progress.animateTo(1f,tween(1350,easing=LinearEasing))
                val offering=flight.offering
                session=session.offerFlower(offering.flowerIndex,offering.deity,offering.position)
                flowerFlights.remove(flight)
            }
        }
    }
    fun offerPrasad() {
        if(entered && foreground && !aging.needsCleaning && !busy && session.step==WorshipStep.PRASAD && !session.prasadOffered) prasadRunning=true
    }
    LaunchedEffect(prasadRunning) {
        if(!prasadRunning) return@LaunchedEffect
        audio.cue(com.worship.nityamandir.R.raw.flower_offering)
        prasad.snapTo(0f);prasad.animateTo(1f,tween(1900,easing=LinearEasing))
        session=session.copy(prasadOffered=true);prasadRunning=false
    }
    LaunchedEffect(aartiRunning,foreground) {
        if(!aartiRunning) return@LaunchedEffect
        if(!foreground) {aartiRunning=false;return@LaunchedEffect}
        try {
            audio.cue(com.worship.nityamandir.R.raw.bell,loop=true)
            aarti.snapTo(0f);aarti.animateTo(1f,tween(21000,easing=LinearEasing))
            session=session.copy(aartiComplete=true);aartiRunning=false
        } finally {audio.stopCue(com.worship.nityamandir.R.raw.bell)}
    }
    LaunchedEffect(bellRunning,foreground) {
        if(!bellRunning) return@LaunchedEffect
        if(!foreground) {bellRunning=false;return@LaunchedEffect}
        try {
            audio.cue(com.worship.nityamandir.R.raw.bell)
            bell.snapTo(0f);bell.animateTo(1f,tween(3500,easing=LinearEasing))
            if(session.step==WorshipStep.BELL) session=session.copy(bellRung=true)
            bellRunning=false
        } finally {audio.stopCue(com.worship.nityamandir.R.raw.bell)}
    }
    LaunchedEffect(conchRunning,foreground) {
        if(!conchRunning) return@LaunchedEffect
        if(!foreground) {conchRunning=false;return@LaunchedEffect}
        try {
            audio.cue(com.worship.nityamandir.R.raw.conch)
            conch.snapTo(0f);conch.animateTo(1f,tween(6000,easing=LinearEasing))
            if(session.step==WorshipStep.CONCH) session=session.copy(conchBlown=true)
            conchRunning=false
        } finally {audio.stopCue(com.worship.nityamandir.R.raw.conch)}
    }
    LaunchedEffect(session, busy, entered, foreground) {
        if(!entered || !foreground || aging.needsCleaning || busy || session.complete || !session.canContinue) return@LaunchedEffect
        delay(650)
        session=session.next()
        if(session.complete) {
            last=System.currentTimeMillis();clean=last
            prefs.edit().putLong("last",last).putLong("clean",clean).apply()
        }
    }
    val cream=Color(0xFFFFF7EC)
    Box(modifier.fillMaxSize().background(cream)) {
        Box(Modifier.fillMaxSize().onGloballyPositioned {sceneSize=it.size;sceneOrigin=it.positionInRoot()}) {
            if(entered || curtain.value>0f) MandirAltarView(session,aging.dustLevel*(1-wipe),aging.flowerWitherFactor,
                bathTarget,bath.value,flowerFlights.toList(),aartiRunning,aartiMotion,
                onDeityClick={deityAction(it)},onDiyaClick={light()},onBellClick={ringBell()},
                prasadRunning=prasadRunning,prasadProgress=prasad.value,
                bellRunning=bellRunning || aartiRunning,bellProgress=if(aartiRunning) aartiMotion else bell.value,conchRunning=conchRunning,conchProgress=conch.value,
                onFlowerClick={offerFlower(it)},onConchClick={soundConch()},onAartiClick={startAarti()})
            if(curtain.value<1f) TempleCurtains(curtain.value,saffron)
            if(entered && !curtainMoving && aging.needsCleaning) Box(Modifier.matchParentSize().pointerInput(Unit) {
                detectDragGestures {change,drag ->
                    change.consume();wipe=(wipe+drag.getDistance()/2500).coerceAtMost(1f)
                    if(wipe>=1) {clean=System.currentTimeMillis();now=clean;prefs.edit().putLong("clean",clean).apply();session=WorshipSession();wipe=0f}
                }
            })
            if(!curtainMoving) RitualGlassPanel(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal=14.dp,vertical=10.dp)
                .fillMaxWidth().heightIn(max=270.dp).verticalScroll(rememberScrollState()),sceneSize,sceneOrigin) {
                val buttonColors=ButtonDefaults.buttonColors(containerColor=RitualInk,contentColor=cream,disabledContainerColor=Color(0xCFE2D9CF),disabledContentColor=Color(0xFF81766C))
                if(!entered) {
                    Text(tr("एक पल, अपने आराध्य के लिए","A moment for the divine"),color=RitualInk,fontSize=19.sp)
                    Text(tr("मैं स्वच्छ हूँ और पूजा के लिए तैयार हूँ।","I am clean and ready to enter my temple."),color=RitualInk,fontSize=13.sp)
                    Button({entered=true},Modifier.fillMaxWidth().heightIn(min=48.dp),colors=buttonColors) {Text(tr("संकल्प लें · मंदिर खोलें","Confirm · Open temple"))}
                } else if(aging.needsCleaning) {
                    Text(tr("मंदिर की स्वच्छता","Refresh your temple"),color=RitualInk,fontSize=20.sp)
                    Text(tr("मंदिर पर उंगली फेरें","Swipe across the temple"),color=RitualGold)
                    LinearProgressIndicator(progress={wipe},modifier=Modifier.fillMaxWidth(),color=RitualGold)
                } else if(session.complete) {
                    Text(tr("पूजा पूर्ण हुई","Worship complete"),color=RitualInk,fontSize=22.sp)
                    Text(tr("आपका दिन मंगलमय हो।","May your day be peaceful."),color=RitualGold)
                    TextButton({closeTemple()}) {Text(tr("पट बंद करें","Close temple"),color=RitualInk)}
                } else {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(if(hindi) session.step.hi else session.step.en,Modifier.weight(1f),color=RitualInk,fontSize=20.sp,fontWeight=FontWeight.Medium)
                        Text("${session.step.ordinal+1} / ${WorshipStep.values().size}",color=RitualGold,fontSize=12.sp)
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        repeat(WorshipStep.values().size) {i -> Box(Modifier.weight(1f).height(2.dp).background(if(i<=session.step.ordinal) Color(0xFFB39268) else Color(0xFFE0D8CF),RoundedCornerShape(4.dp)))}
                    }
                    AnimatedContent(targetState=session.step,transitionSpec={ (slideInHorizontally {it}+fadeIn()) togetherWith (slideOutHorizontally {-it}+fadeOut()) },label="ritual step") { displayedStep ->
                    when(displayedStep) {
                        WorshipStep.LIGHT -> RitualChoice(tr("दीप जलाएँ","Light oil lamp"),session.lit,{light()},Modifier.fillMaxWidth())
                        WorshipStep.FLOWERS -> Text(tr("फूल छूकर अर्पित करें। आगे भी फूल चढ़ा सकते हैं।","Tap flowers to offer; you can keep offering during later steps"),color=RitualInk,fontSize=14.sp)
                        WorshipStep.BATH, WorshipStep.TILAK -> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            val done=when(displayedStep) {WorshipStep.BATH -> session.bathed;WorshipStep.TILAK -> session.tilak;else -> session.flowers}
                            listOf(tr("गणेश जी","Ganesha"),tr("लक्ष्मी जी","Lakshmi")).forEachIndexed {i,label ->
                                RitualChoice(if(bathTarget==i) "$label …" else label,i in done,{deityAction(i)},Modifier.weight(1f),enabled=!busy)
                            }
                        }
                        WorshipStep.BELL -> RitualChoice(tr("घंटी बजाएँ","Ring bell"),session.bellRung,{ringBell()},Modifier.fillMaxWidth())
                        WorshipStep.CONCH -> RitualChoice(tr("शंख बजाएँ","Sound conch"),session.conchBlown,{soundConch()},Modifier.fillMaxWidth())
                        WorshipStep.PRASAD -> RitualChoice(tr("प्रसाद अर्पित करें","Offer prasad"),session.prasadOffered,{offerPrasad()},Modifier.fillMaxWidth(),enabled=!busy)
                        WorshipStep.AARTI -> RitualChoice(if(aartiRunning) tr("आरती चल रही है …","Offering aarti …") else tr("दीप आरती आरंभ करें","Begin diya aarti"),session.aartiComplete,{startAarti()},Modifier.fillMaxWidth(),enabled=!aartiRunning)
                    }
                    }
                    if(busy) LinearProgressIndicator(progress={when {prasadRunning -> prasad.value;bathTarget!=null -> bath.value;flowerFlights.isNotEmpty() -> flowerFlights.first().progress.value;bellRunning -> bell.value;conchRunning -> conch.value;else -> aarti.value}},modifier=Modifier.fillMaxWidth().height(2.dp),color=RitualGold)

                }
            }
        }
        if(entered && !curtainMoving) Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top=8.dp,end=16.dp)) {
            FilledIconButton(
                onClick={menu=true},
                modifier=Modifier.size(48.dp),
                shape=CircleShape,
                colors=IconButtonDefaults.filledIconButtonColors(containerColor=cream,contentColor=RitualInk)
            ) {Icon(Icons.Outlined.MoreHoriz,tr("मंदिर विकल्प","Temple options"))}
            DropdownMenu(menu,{menu=false}) {
                DropdownMenuItem(text={Text(if(hindi) "English" else "हिंदी")},onClick={hindi=!hindi;prefs.edit().putBoolean("hindi",hindi).apply();menu=false})
                DropdownMenuItem(text={Text(tr("पर्दे का रंग बदलें","Change curtain colour"))},onClick={saffron=!saffron;prefs.edit().putBoolean("saffron",saffron).apply();menu=false})
                if(entered) DropdownMenuItem(text={Text(tr("पट बंद करें","Close temple"))},onClick={closeTemple()})
            }
        }
    }
}

@Composable
private fun TempleCurtains(openProgress:Float,saffron:Boolean) {
    val fabric=if(saffron) Color(0xFFAD6629) else Color(0xFF81313C)
    Box(Modifier.fillMaxSize().clipToBounds()) {
        listOf(-1f,1f).forEach {direction ->
            Box(Modifier.fillMaxWidth(0.5f).fillMaxHeight()
                .align(if(direction<0) Alignment.CenterStart else Alignment.CenterEnd)
                .graphicsLayer {translationX=direction*size.width*openProgress}
                .background(Brush.horizontalGradient(listOf(Color(0xFF39101B),fabric,Color(0xFF40131C))))) {
                Row(Modifier.fillMaxSize()) {
                    repeat(9) {
                        Box(Modifier.weight(1f).fillMaxHeight().background(Brush.horizontalGradient(
                            listOf(Color.Transparent,Color(0x44000000),Color.Transparent))))
                    }
                }
                Box(Modifier.align(if(direction<0) Alignment.CenterEnd else Alignment.CenterStart)
                    .fillMaxHeight().width(2.dp).background(Color(0xFFE5BE7B)))
            }
        }
    }
}
