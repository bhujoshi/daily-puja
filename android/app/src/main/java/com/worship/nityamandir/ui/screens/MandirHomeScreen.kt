package com.worship.nityamandir.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
fun MandirHomeScreen(modifier:Modifier=Modifier) {
    val context=LocalContext.current
    val prefs=remember {context.getSharedPreferences("worship",Context.MODE_PRIVATE)}
    var voiceReady by remember {mutableStateOf(false)}
    val voice=remember {android.speech.tts.TextToSpeech(context) {voiceReady=it==android.speech.tts.TextToSpeech.SUCCESS}}
    val audio=remember {RitualAudio(context)}
    DisposableEffect(Unit) {onDispose {voice.stop();voice.shutdown();audio.release()}}
    val lifecycle=androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    var foreground by remember {mutableStateOf(lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED))}
    DisposableEffect(lifecycle) {
        val observer=androidx.lifecycle.LifecycleEventObserver { _,event ->
            if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP) {foreground=false;audio.stop();voice.stop()}
            if(event==androidx.lifecycle.Lifecycle.Event.ON_START) foreground=true
        }
        lifecycle.addObserver(observer)
        onDispose {lifecycle.removeObserver(observer)}
    }
    var hindi by remember {mutableStateOf(prefs.getBoolean("hindi",true))}
    var saffron by remember {mutableStateOf(prefs.getBoolean("saffron",false))}
    var menu by remember {mutableStateOf(false)}
    var entered by remember {mutableStateOf(false)}
    var session by remember {mutableStateOf(WorshipSession())}
    var last by remember {mutableLongStateOf(prefs.getLong("last",System.currentTimeMillis()))}
    var clean by remember {mutableLongStateOf(prefs.getLong("clean",last))}
    var now by remember {mutableLongStateOf(System.currentTimeMillis())}
    var wipe by remember {mutableFloatStateOf(0f)}
    var bathTarget by remember {mutableStateOf<Int?>(null)}
    var flowerTarget by remember {mutableStateOf<Int?>(null)}
    var aartiRunning by remember {mutableStateOf(false)}
    var selectedFlower by remember {mutableIntStateOf(0)}
    var bellRunning by remember {mutableStateOf(false)}
    var conchRunning by remember {mutableStateOf(false)}
    var reciting by remember {mutableStateOf(false)}
    val bell=remember {Animatable(0f)}
    val conch=remember {Animatable(0f)}
    val bath=remember {Animatable(0f)}
    val flight=remember {Animatable(0f)}
    val aarti=remember {Animatable(0f)}
    var sceneSize by remember {mutableStateOf(IntSize.Zero)}
    var sceneOrigin by remember {mutableStateOf(Offset.Zero)}
    LaunchedEffect(Unit) {while(true) {now=System.currentTimeMillis();delay(30000)}}
    val aging=AgingEngine.calculateAging(last,clean,now)
    val busy=bathTarget!=null || flowerTarget!=null || aartiRunning || bellRunning || conchRunning || reciting
    fun tr(hi:String,en:String)=if(hindi) hi else en
    fun closeTemple() {entered=false;menu=false;voice.stop();audio.stop();bathTarget=null;flowerTarget=null;aartiRunning=false;bellRunning=false;conchRunning=false;reciting=false}
    fun ringBell() {
        if(entered && foreground && !aging.needsCleaning && !busy) bellRunning=true
    }
    fun soundConch() {
        if(entered && foreground && !aging.needsCleaning && !busy) conchRunning=true
    }
    fun offerFlower(index:Int) {
        if(!entered || aging.needsCleaning || busy || session.step!=WorshipStep.FLOWERS || index in session.offeredFlowers) return
        selectedFlower=index
        flowerTarget=(listOf(0,1)-session.flowers).randomOrNull() ?: (0..1).random()
    }
    fun light() {if(entered && !aging.needsCleaning && session.step==WorshipStep.LIGHT) {session=session.copy(lit=true);audio.cue(com.worship.nityamandir.R.raw.flower_offering)}}
    fun deityAction(deity:Int) {
        if(!entered || aging.needsCleaning || busy) return
        when(session.step) {
            WorshipStep.BATH -> if(deity !in session.bathed) bathTarget=deity
            WorshipStep.TILAK -> {session=session.copy(tilak=session.tilak+deity);audio.cue(com.worship.nityamandir.R.raw.flower_offering)}
            WorshipStep.FLOWERS -> (0 until TempleSceneLayout.FLOWER_COUNT).firstOrNull {it !in session.offeredFlowers}?.let {offerFlower(it)}
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
    LaunchedEffect(flowerTarget) {
        val target=flowerTarget ?: return@LaunchedEffect
        audio.cue(com.worship.nityamandir.R.raw.flower_offering)
        flight.snapTo(0f);flight.animateTo(1f,tween(1350,easing=LinearEasing))
        session=session.copy(flowers=session.flowers+target,offeredFlowers=session.offeredFlowers+(selectedFlower to target));flowerTarget=null
    }
    LaunchedEffect(aartiRunning) {
        if(!aartiRunning) return@LaunchedEffect
        aarti.snapTo(0f);aarti.animateTo(1f,tween(10500,easing=LinearEasing))
        session=session.copy(aartiComplete=true);aartiRunning=false
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
            conch.snapTo(0f);conch.animateTo(1f,tween(4000,easing=LinearEasing))
            if(session.step==WorshipStep.CONCH) session=session.copy(conchBlown=true)
            conchRunning=false
        } finally {audio.stopCue(com.worship.nityamandir.R.raw.conch)}
    }
    LaunchedEffect(reciting,foreground) {
        if(!reciting) return@LaunchedEffect
        if(!foreground) {reciting=false;return@LaunchedEffect}
        try {
            val completed=suspendCancellableCoroutine<Boolean> { continuation ->
                voice.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(id:String?) = Unit
                    override fun onDone(id:String?) {if(id=="aarti" && continuation.isActive) continuation.resume(true)}
                    @Deprecated("Required by TextToSpeech")
                    override fun onError(id:String?) {if(continuation.isActive) continuation.resume(false)}
                })
                continuation.invokeOnCancellation {voice.stop()}
                voice.language=java.util.Locale("hi","IN")
                val result=voice.speak("जय गणेश जय गणेश जय गणेश देवा। माता जाकी पार्वती पिता महादेवा। एक दंत दयावंत चार भुजा धारी। माथे सिंदूर सोहे मूसे की सवारी।",android.speech.tts.TextToSpeech.QUEUE_FLUSH,null,"aarti")
                if(result==android.speech.tts.TextToSpeech.ERROR && continuation.isActive) continuation.resume(false)
            }
            if(completed) session=session.copy(recitationComplete=true)
        } finally {reciting=false}
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
    Column(modifier.fillMaxSize().background(cream).statusBarsPadding()) {
        // Header ends immediately after the deity names; the scene starts here with no spacer.
        Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("नित्य मंदिर","Nitya Mandir"),color=RitualInk,fontSize=22.sp,fontWeight=FontWeight.Medium)
                Text(tr("गणेश जी  ·  लक्ष्मी जी","Ganesha  ·  Lakshmi"),color=RitualGold,fontSize=12.sp)
            }
            TextButton({hindi=!hindi;prefs.edit().putBoolean("hindi",hindi).apply()}) {Text(if(hindi) "EN" else "हिंदी",color=RitualInk)}
            Box {
                IconButton({menu=true}) {Icon(Icons.Outlined.MoreHoriz,tr("मंदिर विकल्प","Temple options"),tint=RitualInk)}
                DropdownMenu(menu,{menu=false}) {
                    DropdownMenuItem(text={Text(tr("पर्दे का रंग बदलें","Change curtain colour"))},onClick={saffron=!saffron;prefs.edit().putBoolean("saffron",saffron).apply();menu=false})
                    if(entered) DropdownMenuItem(text={Text(tr("पट बंद करें","Close temple"))},onClick={closeTemple()})
                }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f).onGloballyPositioned {sceneSize=it.size;sceneOrigin=it.positionInRoot()}) {
            if(entered) MandirAltarView(session,aging.dustLevel*(1-wipe),aging.flowerWitherFactor,
                bathTarget,bath.value,flowerTarget,flight.value,aartiRunning,aarti.value,
                onDeityClick={deityAction(it)},onDiyaClick={light()},onBellClick={ringBell()},
                selectedFlower=selectedFlower,bellRunning=bellRunning,bellProgress=bell.value,conchRunning=conchRunning,conchProgress=conch.value,
                onFlowerClick={offerFlower(it)},onConchClick={soundConch()},onAartiClick={startAarti()})
            else Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF39101B),if(saffron) Color(0xFFAD6629) else Color(0xFF81313C),Color(0xFF40131C),if(saffron) Color(0xFFAD6629) else Color(0xFF81313C),Color(0xFF39101B))))) {
                Row(Modifier.fillMaxSize()) {repeat(18) {Box(Modifier.weight(1f).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color.Transparent,Color(0x44000000),Color.Transparent))))}}
                Text("ॐ",Modifier.align(Alignment.Center).padding(bottom=160.dp),color=Color(0xFFE5BE7B),fontSize=70.sp)
            }
            if(entered && aging.needsCleaning) Box(Modifier.matchParentSize().pointerInput(Unit) {
                detectDragGestures {change,drag ->
                    change.consume();wipe=(wipe+drag.getDistance()/2500).coerceAtMost(1f)
                    if(wipe>=1) {clean=System.currentTimeMillis();now=clean;prefs.edit().putLong("clean",clean).apply();session=WorshipSession();wipe=0f}
                }
            })
            RitualGlassPanel(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal=14.dp,vertical=10.dp)
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
                        WorshipStep.FLOWERS -> Text(tr("थाली में किसी फूल को छूकर अर्पित करें","Tap any flower in the plate to offer it"),color=RitualInk,fontSize=14.sp)
                        WorshipStep.BATH, WorshipStep.TILAK -> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            val done=when(displayedStep) {WorshipStep.BATH -> session.bathed;WorshipStep.TILAK -> session.tilak;else -> session.flowers}
                            listOf(tr("गणेश जी","Ganesha"),tr("लक्ष्मी जी","Lakshmi")).forEachIndexed {i,label ->
                                RitualChoice(if(bathTarget==i || flowerTarget==i) "$label …" else label,i in done,{deityAction(i)},Modifier.weight(1f),enabled=!busy)
                            }
                        }
                        WorshipStep.BELL -> RitualChoice(tr("घंटी बजाएँ","Ring bell"),session.bellRung,{ringBell()},Modifier.fillMaxWidth())
                        WorshipStep.CONCH -> RitualChoice(tr("शंख बजाएँ","Sound conch"),session.conchBlown,{soundConch()},Modifier.fillMaxWidth())
                        WorshipStep.PRASAD -> RitualChoice(tr("प्रसाद अर्पित करें","Offer prasad"),session.prasadOffered,{audio.cue(com.worship.nityamandir.R.raw.flower_offering);session=session.copy(prasadOffered=true)},Modifier.fillMaxWidth())
                        WorshipStep.RECITATION -> TextButton(enabled=voiceReady && !busy,onClick={reciting=true}) {Text(tr("▶ आरती पाठ सुनें","▶ Listen to aarti"),color=RitualInk)}
                        WorshipStep.AARTI -> RitualChoice(if(aartiRunning) tr("आरती चल रही है …","Offering aarti …") else tr("दीप आरती आरंभ करें","Begin diya aarti"),session.aartiComplete,{startAarti()},Modifier.fillMaxWidth(),enabled=!aartiRunning)
                    }
                    }
                    if(reciting) LinearProgressIndicator(modifier=Modifier.fillMaxWidth().height(2.dp),color=RitualGold)
                    else if(busy) LinearProgressIndicator(progress={when {bathTarget!=null -> bath.value;flowerTarget!=null -> flight.value;bellRunning -> bell.value;conchRunning -> conch.value;else -> aarti.value}},modifier=Modifier.fillMaxWidth().height(2.dp),color=RitualGold)

                }
            }
        }
    }
}
