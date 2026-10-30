package com.worship.nityamandir.ui.components

import android.app.Activity
import android.os.SystemClock
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worship.nityamandir.R
import com.worship.nityamandir.data.MindfulnessStats
import com.worship.nityamandir.engine.*
import com.worship.nityamandir.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class SessionStage { SETUP, ACTIVE, COMPLETED }

@Composable
fun MindfulnessMeditationOverlay(
    hindi: Boolean,
    stats: MindfulnessStats,
    audio: RitualAudio,
    onSessionStarted: (MindfulnessPractice, Int, String) -> Unit,
    onSessionCompleted: (MindfulnessPractice, Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    fun tr(hi: String, en: String) = if (hindi) hi else en
    val context = LocalContext.current

    var stage by remember { mutableStateOf(SessionStage.SETUP) }
    var selectedPractice by remember {
        mutableStateOf(
            MindfulnessPractice.entries.firstOrNull { it.id == stats.lastPracticeId }
                ?: MindfulnessPractice.PRANAYAMA
        )
    }
    var selectedPattern by remember { mutableStateOf(BreathingPattern.SAMA_VRITTI) }
    var selectedMantra by remember { mutableStateOf(SacredMantra.OM_PRANAVA) }
    var durationMinutes by remember { mutableIntStateOf(5) }
    var soundCuesEnabled by remember { mutableStateOf(true) }

    var isPaused by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var japaCount by remember { mutableIntStateOf(0) }
    var lastBellCueIndex by remember { mutableIntStateOf(0) }

    val totalSeconds = durationMinutes * 60
    val snapshot = remember(elapsedMs, totalSeconds, selectedPractice, selectedPattern) {
        MindfulnessEngine.evaluate(
            elapsedMs = elapsedMs,
            totalDurationSeconds = totalSeconds,
            practice = selectedPractice,
            breathingPattern = selectedPattern
        )
    }

    // Keep screen awake during active meditation
    DisposableEffect(stage, isPaused) {
        val window = (context as? Activity)?.window
        if (stage == SessionStage.ACTIVE && !isPaused) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Monotonic timer loop for active session
    LaunchedEffect(stage, isPaused, durationMinutes, selectedPractice, selectedPattern) {
        if (stage != SessionStage.ACTIVE || isPaused) return@LaunchedEffect
        var lastTick = SystemClock.elapsedRealtime()
        while (isActive && stage == SessionStage.ACTIVE && !isPaused) {
            delay(50L)
            val now = SystemClock.elapsedRealtime()
            val delta = (now - lastTick).coerceAtLeast(0L)
            lastTick = now
            val nextElapsed = (elapsedMs + delta).coerceAtMost(totalSeconds * 1000L)
            elapsedMs = nextElapsed
            if (nextElapsed >= totalSeconds * 1000L) {
                if (soundCuesEnabled) {
                    audio.cue(R.raw.bell)
                }
                onSessionCompleted(selectedPractice, durationMinutes)
                stage = SessionStage.COMPLETED
                break
            }
        }
    }

    // Play soft temple bell on Trataka or Mantra interval transitions
    LaunchedEffect(snapshot.bellCueIndex, stage, soundCuesEnabled) {
        if (stage == SessionStage.ACTIVE && snapshot.bellCueIndex > lastBellCueIndex && !snapshot.completed) {
            lastBellCueIndex = snapshot.bellCueIndex
            if (soundCuesEnabled) {
                audio.cue(R.raw.bell)
            }
        }
    }

    fun beginSession() {
        elapsedMs = 0L
        japaCount = 0
        lastBellCueIndex = 0
        isPaused = false
        stage = SessionStage.ACTIVE
        if (soundCuesEnabled) {
            audio.cue(R.raw.bell)
        }
        val subOption = when (selectedPractice) {
            MindfulnessPractice.PRANAYAMA -> selectedPattern.id
            MindfulnessPractice.TRATAKA -> "diya_trataka"
            MindfulnessPractice.MANTRA_DHYAN -> selectedMantra.id
        }
        onSessionStarted(selectedPractice, durationMinutes, subOption)
    }

    fun finishEarlyAndComplete() {
        val practicedMinutes = ((elapsedMs / 60_000L).toInt()).coerceAtLeast(1)
        if (soundCuesEnabled) {
            audio.cue(R.raw.bell)
        }
        onSessionCompleted(selectedPractice, practicedMinutes)
        stage = SessionStage.COMPLETED
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xD90D0B0A),
                        Color(0xBF140D0A),
                        Color(0xEB0D0B0A)
                    )
                )
            )
            .testTag("mindfulness-overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Sacred Header Bar
            MindfulnessTopBar(
                hindi = hindi,
                soundCuesEnabled = soundCuesEnabled,
                onToggleSound = { soundCuesEnabled = !soundCuesEnabled },
                onClose = {
                    audio.stopCue(R.raw.bell)
                    onClose()
                }
            )

            // 2. Main Body Area (Setup, Active Sanctum Canvas, or Completion Card)
            AnimatedContent(
                targetState = stage,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label = "mindfulness_stage"
            ) { currentStage ->
                when (currentStage) {
                    SessionStage.SETUP -> {
                        MindfulnessSetupContent(
                            hindi = hindi,
                            selectedPractice = selectedPractice,
                            onSelectPractice = { selectedPractice = it },
                            selectedPattern = selectedPattern,
                            onSelectPattern = { selectedPattern = it },
                            selectedMantra = selectedMantra,
                            onSelectMantra = { selectedMantra = it },
                            durationMinutes = durationMinutes,
                            onSelectDuration = { durationMinutes = it },
                            onStart = ::beginSession
                        )
                    }
                    SessionStage.ACTIVE -> {
                        MindfulnessActiveContent(
                            hindi = hindi,
                            practice = selectedPractice,
                            pattern = selectedPattern,
                            mantra = selectedMantra,
                            snapshot = snapshot,
                            isPaused = isPaused,
                            japaCount = japaCount,
                            onIncrementJapa = {
                                japaCount++
                                if (japaCount % MindfulnessEngine.MALA_BEADS == 0 && soundCuesEnabled) {
                                    audio.cue(R.raw.bell)
                                } else if (soundCuesEnabled) {
                                    audio.cue(R.raw.flower_offering)
                                }
                            },
                            onTogglePause = { isPaused = !isPaused },
                            onCompleteEarly = ::finishEarlyAndComplete,
                            onCancelToSetup = {
                                isPaused = false
                                elapsedMs = 0L
                                stage = SessionStage.SETUP
                            }
                        )
                    }
                    SessionStage.COMPLETED -> {
                        MindfulnessCompletionCard(
                            hindi = hindi,
                            stats = stats,
                            japaCount = japaCount,
                            onMeditateAgain = {
                                elapsedMs = 0L
                                stage = SessionStage.SETUP
                            },
                            onReturnToSanctum = onClose
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MindfulnessTopBar(
    hindi: Boolean,
    soundCuesEnabled: Boolean,
    onToggleSound: () -> Unit,
    onClose: () -> Unit
) {
    fun tr(hi: String, en: String) = if (hindi) hi else en
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = tr("अपने लिए थोड़ा समय", "A moment for yourself"),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SacredGold,
                letterSpacing = 1.sp
            )
            Text(
                text = tr("ध्यान", "Mindfulness"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = SacredTextPrimary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sound Bell Cue Toggle
            Surface(
                onClick = onToggleSound,
                shape = CircleShape,
                color = Color(0x3322140F),
                border = BorderStroke(1.dp, if (soundCuesEnabled) SacredGold else Color(0x55CFC2B2)),
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = if (soundCuesEnabled) {
                            tr("घंटी ध्वनि चालू है", "Bell sound cues enabled")
                        } else {
                            tr("घंटी ध्वनि बंद है", "Bell sound cues muted")
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (soundCuesEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                        contentDescription = null,
                        tint = if (soundCuesEnabled) SacredGold else SacredTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Close Overlay Button
            Surface(
                onClick = onClose,
                shape = CircleShape,
                color = Color(0xFFFAF6EE),
                border = BorderStroke(1.dp, SacredGold),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("mindfulness-close")
                    .semantics {
                        contentDescription = tr("ध्यान साधना बंद करें", "Close mindfulness meditation")
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = RitualInk,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MindfulnessSetupContent(
    hindi: Boolean,
    selectedPractice: MindfulnessPractice,
    onSelectPractice: (MindfulnessPractice) -> Unit,
    selectedPattern: BreathingPattern,
    onSelectPattern: (BreathingPattern) -> Unit,
    selectedMantra: SacredMantra,
    onSelectMantra: (SacredMantra) -> Unit,
    durationMinutes: Int,
    onSelectDuration: (Int) -> Unit,
    onStart: () -> Unit
) {
    fun tr(hi: String, en: String) = if (hindi) hi else en
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.weight(1f).widthIn(max = 520.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFFAF6EE),
            border = BorderStroke(1.dp, RitualGold)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    tr("आराम से बैठें। अपना अभ्यास और समय चुनें।", "Sit comfortably. Choose a practice and a duration."),
                    color = RitualInk,
                    fontSize = 16.sp
                )
                MindfulnessSectionLabel(tr("अभ्यास चुनें", "Choose a practice"))
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MindfulnessPractice.entries.forEach { practice ->
                        MindfulnessOption(
                            title = practice.title(hindi),
                            description = practice.subtitle(hindi),
                            selected = practice == selectedPractice,
                            onClick = { onSelectPractice(practice) },
                            modifier = Modifier.testTag("mindfulness-practice-${practice.id}")
                        )
                    }
                }
                when (selectedPractice) {
                    MindfulnessPractice.PRANAYAMA -> {
                        MindfulnessSectionLabel(tr("साँस की लय", "Breathing rhythm"))
                        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BreathingPattern.entries.forEach { pattern ->
                                MindfulnessOption(
                                    title = pattern.title(hindi),
                                    description = pattern.description(hindi),
                                    selected = pattern == selectedPattern,
                                    onClick = { onSelectPattern(pattern) },
                                    modifier = Modifier.testTag("mindfulness-pattern-${pattern.id}")
                                )
                            }
                        }
                    }
                    MindfulnessPractice.TRATAKA -> {
                        MindfulnessSectionLabel(tr("कैसे करें", "How it works"))
                        Text(
                            tr(
                                "40 सेकंड लौ को आराम से देखें। फिर 20 सेकंड आँखें बंद करके लौ की कल्पना करें।",
                                "Look softly at the flame for 40 seconds. Then close your eyes and picture it for 20 seconds."
                            ),
                            fontSize = 16.sp,
                            color = RitualInk
                        )
                    }
                    MindfulnessPractice.MANTRA_DHYAN -> {
                        MindfulnessSectionLabel(tr("मंत्र या मौन चुनें", "Choose a mantra or silence"))
                        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SacredMantra.entries.forEach { mantra ->
                                MindfulnessOption(
                                    title = mantra.title(hindi),
                                    selected = mantra == selectedMantra,
                                    onClick = { onSelectMantra(mantra) },
                                    modifier = Modifier.testTag("mindfulness-mantra-${mantra.id}")
                                )
                            }
                        }
                    }
                }
                MindfulnessSectionLabel(tr("समय (मिनट)", "Duration (minutes)"))
                FlowRow(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MindfulnessEngine.PRESET_DURATIONS_MINUTES.forEach { minutes ->
                        val selected = minutes == durationMinutes
                        Surface(
                            modifier = Modifier
                                .widthIn(min = 64.dp)
                                .heightIn(min = 52.dp)
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { onSelectDuration(minutes) }
                                )
                                .testTag("mindfulness-duration-$minutes"),
                            shape = RoundedCornerShape(17.dp),
                            color = if (selected) Color(0xFFF0E0C8) else Color.White,
                            border = BorderStroke(if (selected) 1.5.dp else 1.dp, RitualGold)
                        ) {
                            Box(Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (selected) "✓ $minutes" else "$minutes",
                                    color = RitualInk,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
        Button(
            onClick = onStart,
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()
                .heightIn(min = 56.dp).testTag("mindfulness-start"),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SacredGold,
                contentColor = RitualInk
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = tr("ध्यान शुरू करें", "Start meditation"),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MindfulnessSectionLabel(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = RitualInk)
}

@Composable
private fun MindfulnessOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        color = if (selected) Color(0xFFF0E0C8) else Color(0x99FFFFFF),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) RitualGold else Color(0x66B3833B))
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(selected = selected, onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = RitualInk, unselectedColor = RitualInk))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = RitualInk)
                if (description != null) {
                    Text(description, fontSize = 14.sp, color = Color(0xFF5A4438))
                }
            }
        }
    }
}

@Composable
private fun StatBadgeItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = RitualInk
        )
        Text(
            text = label,
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6E5D4F)
        )
    }
}

@Composable
private fun MindfulnessActiveContent(
    hindi: Boolean,
    practice: MindfulnessPractice,
    pattern: BreathingPattern,
    mantra: SacredMantra,
    snapshot: MindfulnessSnapshot,
    isPaused: Boolean,
    japaCount: Int,
    onIncrementJapa: () -> Unit,
    onTogglePause: () -> Unit,
    onCompleteEarly: () -> Unit,
    onCancelToSetup: () -> Unit
) {
    fun tr(hi: String, en: String) = if (hindi) hi else en

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Practice Title Pill
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0x4D22140F),
            border = BorderStroke(1.dp, Color(0x66D8AF52)),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(
                text = when (practice) {
                    MindfulnessPractice.PRANAYAMA -> pattern.title(hindi)
                    MindfulnessPractice.TRATAKA -> practice.title(hindi)
                    MindfulnessPractice.MANTRA_DHYAN -> mantra.title(hindi)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SacredGold
            )
        }

        // Central Immersive Sacred Focal Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when (practice) {
                MindfulnessPractice.PRANAYAMA -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            snapshot.breathPhase.label(hindi),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SacredTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        PranaLotusMandalaCanvas(
                            scale = snapshot.lotusScale,
                            secondsText = "${snapshot.breathPhaseRemainingSeconds}s",
                            modifier = Modifier.size(260.dp)
                        )
                        Text(
                            text = snapshot.breathPhase.hint(hindi),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = SacredTextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
                MindfulnessPractice.TRATAKA -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        TratakaSacredFlameCanvas(
                            phase = snapshot.tratakaPhase,
                            modifier = Modifier.size(250.dp)
                        )
                        Text(
                            text = "${snapshot.tratakaPhase.label(hindi)} (${snapshot.tratakaPhaseRemainingSeconds}s)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = SacredGold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = snapshot.tratakaPhase.instruction(hindi),
                            fontSize = 15.sp,
                            color = SacredTextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
                MindfulnessPractice.MANTRA_DHYAN -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MantraMalaCanvas(
                            mantra = mantra,
                            japaCount = japaCount,
                            lotusPulse = snapshot.lotusScale,
                            onTapMala = onIncrementJapa,
                            interactive = true,
                            modifier = Modifier
                                .size(230.dp)
                                .testTag("mindfulness-japa-target")
                        )
                        Text(
                            text = mantra.sanskrit,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = SacredTextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Text(
                            text = mantra.meaning(hindi),
                            fontSize = 13.sp,
                            color = SacredTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Surface(
                            onClick = onIncrementJapa,
                            shape = RoundedCornerShape(50),
                            color = Color(0x33D8AF52),
                            border = BorderStroke(1.dp, SacredGold)
                        ) {
                            Text(
                                text = tr(
                                    "$japaCount जप · गिनने के लिए छुएँ",
                                    "$japaCount chants · Tap to count"
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SacredTextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Bottom Frosted Sandalwood Timer & Controls Dock
        val dockShape = RoundedCornerShape(26.dp)
        Surface(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .shadow(16.dp, dockShape, ambientColor = Color(0x661A1108), spotColor = Color(0x661A1108)),
            shape = dockShape,
            color = Color(0xFFFAF6EE),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(listOf(Color(0xFFF7E2A4), RitualGold, Color(0xFFE5C168)))
            )
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFFDF8), Color(0xFFFAF3E6))
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (isPaused) tr("विराम", "Paused")
                            else tr("शेष समय", "Time left"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6E5D4F)
                        )
                        Text(
                            text = MindfulnessEngine.formatTimer(snapshot.remainingSeconds),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = RitualInk
                        )
                    }

                    // Change Practice / Reset Link
                    TextButton(onClick = onCancelToSetup) {
                        Text(
                            text = tr("विधि बदलें", "Change practice"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6E5D4F)
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { snapshot.sessionProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = RitualGold,
                    trackColor = Color(0xFFE5D5BC),
                    strokeCap = StrokeCap.Round
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RitualChoice(
                        label = if (isPaused) tr("जारी रखें", "Resume") else tr("विराम", "Pause"),
                        selected = isPaused,
                        onClick = onTogglePause,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mindfulness-pause")
                    )
                    Button(
                        onClick = onCompleteEarly,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .testTag("mindfulness-stop"),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RitualInk,
                            contentColor = SacredTextPrimary
                        )
                    ) {
                        Text(
                            text = tr("समाप्त करें", "Finish"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MindfulnessCompletionCard(
    hindi: Boolean,
    stats: MindfulnessStats,
    japaCount: Int,
    onMeditateAgain: () -> Unit,
    onReturnToSanctum: () -> Unit
) {
    fun tr(hi: String, en: String) = if (hindi) hi else en
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag("mindfulness-complete-card"),
        contentAlignment = Alignment.Center
    ) {
        val shape = RoundedCornerShape(28.dp)
        Surface(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .shadow(20.dp, shape, ambientColor = Color(0x661A1108), spotColor = Color(0x661A1108)),
            shape = shape,
            color = Color(0xFFFAF6EE),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(listOf(Color(0xFFF7E2A4), RitualGold, Color(0xFFE5C168)))
            )
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFFDF8), Color(0xFFFAF3E6), Color(0xFFF3E7D3))
                        )
                    )
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF3E2C3),
                    border = BorderStroke(1.5.dp, RitualGold),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "ॐ",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = RitualInk
                        )
                    }
                }

                Text(
                    text = tr("ॐ शांतिः शांतिः शांतिः", "Om Shantih Shantih Shantih"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = RitualInk,
                    letterSpacing = 1.sp
                )

                Text(
                    text = tr("ध्यान पूरा हुआ", "Session complete"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = RitualInk,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = buildString {
                        append(
                            tr(
                                "आगे बढ़ने से पहले थोड़ा आराम करें।",
                                "Take a moment before you return to your day."
                            )
                        )
                        if (japaCount > 0) {
                            append(
                                tr(
                                    " ($japaCount मंत्र जप)",
                                    " ($japaCount chants)"
                                )
                            )
                        }
                    },
                    fontSize = 14.sp,
                    color = Color(0xFF5A4438),
                    textAlign = TextAlign.Center
                )

                // Updated Stats Summary
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFF3E7D3),
                    border = BorderStroke(1.dp, Color(0x55B3833B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatBadgeItem(
                            value = "${stats.streakDays}",
                            modifier = Modifier.weight(1f),
                            label = tr("साधना क्रम", "Day Streak")
                        )
                        StatBadgeItem(
                            value = "${stats.totalMinutes}",
                            modifier = Modifier.weight(1f),
                            label = tr("ध्यान मिनट", "Total Mins")
                        )
                        StatBadgeItem(
                            value = "${stats.totalSessions}",
                            modifier = Modifier.weight(1f),
                            label = tr("कुल सत्र", "Sessions")
                        )
                    }
                }

                RitualChoice(
                    label = tr("पुनः ध्यान करें", "Meditate Again"),
                    selected = false,
                    onClick = onMeditateAgain,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = onReturnToSanctum,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("mindfulness-return-sanctum"),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RitualInk,
                        contentColor = SacredTextPrimary
                    )
                ) {
                    Text(
                        text = tr("मंदिर लौटें", "Back to mandir"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Sacred Prana Lotus Mandala (प्राण कमल मंडल) for Pranayama breathwork.
 * Expands and contracts smoothly with concentric prabhamandala rings.
 */
@Composable
private fun PranaLotusMandalaCanvas(
    scale: Float,
    secondsText: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lotusShimmer")
    val slowRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 36000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "slowRotation"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = size.minDimension / 2f
            val activeR = maxR * scale.coerceIn(0.5f, 1.0f)

            // 1. Outer diffuse Prabhamandala halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x55FF9E2C),
                        Color(0x22D8AF52),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = maxR
                ),
                radius = maxR,
                center = Offset(cx, cy)
            )

            // 2. Fixed outer reference brass ring (shows full inhale boundary)
            drawCircle(
                color = Color(0x44D8AF52),
                radius = maxR * 0.94f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // 3. Rotating 8-petal Sacred Lotus (अष्टदल कमल)
            rotate(degrees = slowRotation, pivot = Offset(cx, cy)) {
                val petalCount = 8
                for (i in 0 until petalCount) {
                    rotate(degrees = i * (360f / petalCount), pivot = Offset(cx, cy)) {
                        val petalPath = Path().apply {
                            moveTo(cx, cy - activeR * 0.25f)
                            quadraticTo(
                                cx + activeR * 0.34f,
                                cy - activeR * 0.65f,
                                cx,
                                cy - activeR * 0.94f
                            )
                            quadraticTo(
                                cx - activeR * 0.34f,
                                cy - activeR * 0.65f,
                                cx,
                                cy - activeR * 0.25f
                            )
                            close()
                        }
                        drawPath(
                            path = petalPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x88FFE777),
                                    Color(0x66E26A8D),
                                    Color(0x33D8AF52)
                                ),
                                startY = cy - activeR * 0.94f,
                                endY = cy - activeR * 0.25f
                            )
                        )
                        drawPath(
                            path = petalPath,
                            color = Color(0xAAFFE777),
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    }
                }
            }

            // 4. Concentric Prabhamandala rings (3 luminous rings per design system)
            drawCircle(
                color = Color(0x88FFCA3A),
                radius = activeR * 0.72f,
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color(0xCCFFE777),
                radius = activeR * 0.64f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xEE22140F),
                        Color(0xDD140D0A)
                    ),
                    center = Offset(cx, cy),
                    radius = activeR * 0.60f
                ),
                radius = activeR * 0.60f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xDDFFFBE3),
                radius = activeR * 0.60f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        Text(
            text = secondsText,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = SacredTextPrimary
        )
    }
}

/**
 * Sacred Diya Flame Canvas for Trataka (त्राटक ध्यान).
 * Adheres strictly to the 4-stop radial gradient and natural micro-flicker rules.
 */
@Composable
private fun TratakaSacredFlameCanvas(
    phase: TratakaPhase,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tratakaFlame")
    val wobble by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 240, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wobble"
    )
    val heightScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heightScale"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseTopY = size.height * 0.72f
        val flameCenterY = baseTopY - size.height * 0.22f

        if (phase == TratakaPhase.OPEN_GAZE) {
            // 1. Ambient Halo: rgba(255, 158, 44, 0.27) with smooth falloff
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x45FF9E2C),
                        Color(0x1FFF6D00),
                        Color.Transparent
                    ),
                    center = Offset(cx, flameCenterY),
                    radius = size.minDimension * 0.48f * heightScale
                ),
                radius = size.minDimension * 0.48f * heightScale,
                center = Offset(cx, flameCenterY)
            )

            // 2. Concentric subtle Trataka focus ring behind the flame
            drawCircle(
                color = Color(0x44FFE777),
                radius = size.minDimension * 0.34f,
                center = Offset(cx, flameCenterY),
                style = Stroke(width = 1.dp.toPx())
            )

            // 3. Brass Diya Bowl (पीतल का दीपक)
            val bowlWidth = size.width * 0.42f
            val bowlHeight = size.height * 0.18f
            val diyaPath = Path().apply {
                moveTo(cx - bowlWidth / 2f, baseTopY)
                cubicTo(
                    cx - bowlWidth * 0.56f, baseTopY + bowlHeight,
                    cx + bowlWidth * 0.56f, baseTopY + bowlHeight,
                    cx + bowlWidth / 2f, baseTopY
                )
                close()
            }
            drawPath(
                path = diyaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(SacredGold, SacredBrass, Color(0xFF7A5612)),
                    startY = baseTopY,
                    endY = baseTopY + bowlHeight
                )
            )
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF4A2B0B), SacredBrass),
                    center = Offset(cx, baseTopY)
                ),
                topLeft = Offset(cx - bowlWidth / 2f, baseTopY - 7.dp.toPx()),
                size = Size(bowlWidth, 14.dp.toPx())
            )

            // 4. Cotton Wick (बाती)
            drawLine(
                color = Color(0xFF3E2208),
                start = Offset(cx, baseTopY + 2.dp.toPx()),
                end = Offset(cx, baseTopY - 10.dp.toPx()),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 5. Outer Mantle: #FF6D00 (Flame Tangerine) -> #FFC107 (Honey Amber)
            val flameHeight = size.height * 0.36f * heightScale
            val outerWidth = size.width * 0.11f
            val outerFlame = Path().apply {
                moveTo(cx - outerWidth, baseTopY - 6.dp.toPx())
                cubicTo(
                    cx - outerWidth * 1.4f, baseTopY - flameHeight * 0.45f,
                    cx - outerWidth * 0.5f + wobble, baseTopY - flameHeight * 0.86f,
                    cx + wobble, baseTopY - flameHeight
                )
                cubicTo(
                    cx + outerWidth * 0.5f + wobble, baseTopY - flameHeight * 0.86f,
                    cx + outerWidth * 1.4f, baseTopY - flameHeight * 0.45f,
                    cx + outerWidth, baseTopY - 6.dp.toPx()
                )
                close()
            }
            drawPath(
                path = outerFlame,
                brush = Brush.verticalGradient(
                    colors = listOf(DiyaFlameInner, DiyaFlameOuter),
                    startY = baseTopY - flameHeight,
                    endY = baseTopY
                )
            )

            // 6. Inner Body & Karpura Core (#FFFDF8)
            val innerHeight = flameHeight * 0.74f
            val innerWidth = outerWidth * 0.58f
            val innerFlame = Path().apply {
                moveTo(cx - innerWidth, baseTopY - 8.dp.toPx())
                cubicTo(
                    cx - innerWidth * 1.2f, baseTopY - innerHeight * 0.42f,
                    cx - innerWidth * 0.4f + wobble * 0.5f, baseTopY - innerHeight * 0.85f,
                    cx + wobble * 0.5f, baseTopY - innerHeight
                )
                cubicTo(
                    cx + innerWidth * 0.4f + wobble * 0.5f, baseTopY - innerHeight * 0.85f,
                    cx + innerWidth * 1.2f, baseTopY - innerHeight * 0.42f,
                    cx + innerWidth, baseTopY - 8.dp.toPx()
                )
                close()
            }
            drawPath(
                path = innerFlame,
                brush = Brush.verticalGradient(
                    colors = listOf(DiyaFlameCore, DiyaFlameInner),
                    startY = baseTopY - innerHeight,
                    endY = baseTopY
                )
            )

            // 7. Focal Bindu point at the luminous tip for steady Trataka gaze
            drawCircle(
                color = DiyaFlameCore,
                radius = 3.5.dp.toPx(),
                center = Offset(cx + wobble * 0.5f, baseTopY - innerHeight * 0.75f)
            )
        } else {
            // Antaranga Trataka (Inner After-Image Visualization at Ajna Chakra)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x38FFE777),
                        Color(0x18FF9E2C),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = size.minDimension * 0.42f * heightScale
                ),
                radius = size.minDimension * 0.42f * heightScale,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0x88D8AF52),
                radius = size.minDimension * 0.24f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Luminous inner Jyoti bindu
            drawCircle(
                color = DiyaFlameCore,
                radius = 6.dp.toPx() * heightScale,
                center = Offset(cx, cy)
            )
        }
    }
}

/**
 * 108-Bead Sacred Japa Mala Canvas for Mantra & Silent Dhyan.
 */
@Composable
private fun MantraMalaCanvas(
    mantra: SacredMantra,
    japaCount: Int,
    lotusPulse: Float,
    onTapMala: () -> Unit,
    interactive: Boolean,
    modifier: Modifier = Modifier
) {
    val totalBeads = 54 // Visual ring of 54 beads representing the 108 mala (each bead = 2 or full cycle)
    val activeBeads = (japaCount % MindfulnessEngine.MALA_BEADS) * totalBeads / MindfulnessEngine.MALA_BEADS

    Box(
        modifier = if (interactive) {
            modifier.clickable(
                role = Role.Button,
                onClick = onTapMala
            )
        } else {
            modifier
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val ringR = size.minDimension * 0.43f

            // Subtle breathing halo behind the Om / Mantra emblem
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x44FF9E2C),
                        Color(0x1AD8AF52),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = ringR * lotusPulse.coerceIn(0.65f, 1.0f)
                ),
                radius = ringR * lotusPulse.coerceIn(0.65f, 1.0f),
                center = Offset(cx, cy)
            )

            // Sumeru (Guru Bead) at the top
            drawCircle(
                color = SacredSaffron,
                radius = 5.5.dp.toPx(),
                center = Offset(cx, cy - ringR)
            )

            // Mala beads around the perimeter
            for (i in 1 until totalBeads) {
                val angleRad = (i.toDouble() / totalBeads.toDouble()) * 2.0 * PI - (PI / 2.0)
                val bx = cx + (ringR * cos(angleRad)).toFloat()
                val by = cy + (ringR * sin(angleRad)).toFloat()
                val isLit = i <= activeBeads
                drawCircle(
                    color = if (isLit) SacredGold else Color(0x55C5A059),
                    radius = if (isLit) 3.8.dp.toPx() else 2.6.dp.toPx(),
                    center = Offset(bx, by)
                )
            }

            // Inner sanctum medallion
            drawCircle(
                color = Color(0xCC140D0A),
                radius = ringR * 0.72f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = SacredGold,
                radius = ringR * 0.72f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = mantra.emblem,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = SacredGold
            )
            if (interactive) {
                Text(
                    text = "$japaCount / ${MindfulnessEngine.MALA_BEADS}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SacredTextSecondary
                )
            }
        }
    }
}
