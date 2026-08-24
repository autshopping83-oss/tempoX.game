package com.tempoX.game.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tempoX.game.R
import com.tempoX.game.audio.SoundManager
import com.tempoX.game.audio.SoundManager.Sfx.CLICK
import com.tempoX.game.game.Challenge
import com.tempoX.game.game.ChallengeType
import com.tempoX.game.game.GameEngine
import com.tempoX.game.game.GameMode
import com.tempoX.game.game.MatchSummary
import com.tempoX.game.game.MockAdManager
import com.tempoX.game.game.Progression
import com.tempoX.game.ui.components.FloatingCard
import com.tempoX.game.ui.components.PrimaryButton
import com.tempoX.game.ui.components.SecondaryButton
import com.tempoX.game.ui.theme.TemproxColors
import com.tempoX.game.ui.theme.TemproxShapes
import com.tempoX.game.ui.theme.TemproxType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val TICK_MS = 60L

/** In-match screen: HUD + active challenge host + pause overlay. */
@Composable
fun GameScreen(
    seedText: String,
    mode: GameMode,
    vipInstant: Boolean = false,
    onFinish: (summary: MatchSummary, engine: GameEngine) -> Unit,
    onQuit: () -> Unit,
) {
    var sessionId by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    var abandoned by remember { mutableStateOf(false) }
    var soundOn by remember { mutableStateOf(SoundManager.isEnabled()) }
    var hapticsOn by remember { mutableStateOf(SoundManager.isHapticsEnabled()) }
    var fxVolume by remember { mutableStateOf(SoundManager.getVolume()) }

    /** Frozen per-challenge clock while a Memory sequence flashes. */
    var memoryWatching by remember { mutableStateOf(false) }

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val engine = remember(sessionId) {
        GameEngine(seedText.hashCode().toLong(), mode).also { eng ->
            eng.onEvent = { ev ->
                when (ev) {
                    GameEngine.Event.CORRECT -> SoundManager.play(SoundManager.Sfx.CORRECT)
                    GameEngine.Event.WRONG -> SoundManager.play(SoundManager.Sfx.WRONG)
                    GameEngine.Event.COMBO_MILESTONE -> {
                        SoundManager.play(SoundManager.Sfx.COMBO)
                        SoundManager.vibrate(longArrayOf(0, 30, 40, 30))
                    }
                }
            }
        }
    }

    // Reset the watch flag whenever a new challenge appears.
    LaunchedEffect(engine.challenge, sessionId) {
        memoryWatching = engine.challenge is Challenge.Memory
    }

    LaunchedEffect(sessionId) {
        while (!engine.finished) {
            if (!paused) {
                engine.tick(TICK_MS)
                if (!memoryWatching) engine.tickChallenge(TICK_MS)
            }
            delay(TICK_MS)
        }
        if (!abandoned && !paused && engine.finished) {
            onFinish(
                MatchSummary(
                    score = engine.score,
                    totalCorrect = engine.totalCorrect,
                    totalIncorrect = engine.totalIncorrect,
                    maxCombo = engine.maxCombo,
                    xpGained = engine.xpGained,
                    completed = engine.timeLeftMillis == 0L,
                    coinsEarned = engine.sessionCoins,
                ),
                engine,
            )
        }
    }

    // Final-count digital ticks over the last 5 seconds. The per-second
    // read is isolated in snapshotFlow so it never recomposes this screen.
    val pausedWhileTicking = rememberUpdatedState(paused)
    var lastTickedSecond by remember { mutableIntStateOf(-1) }
    LaunchedEffect(Unit) {
        snapshotFlow { ceil(engine.timeLeftMillis / 1000f).toInt() }
            .collect { sec ->
                if (!pausedWhileTicking.value && sec in 1..5 && lastTickedSecond != sec) {
                    lastTickedSecond = sec
                    SoundManager.play(SoundManager.Sfx.TICK)
                }
            }
    }

    Box(
        Modifier
            .fillMaxSize(),
    ) {
        com.tempoX.game.ui.components.AnimatedBackground(Modifier.fillMaxSize())
        com.tempoX.game.ui.components.FormulaLayer(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .clickable { SoundManager.play(CLICK); paused = true },
                    contentAlignment = Alignment.Center,
                ) { Text("⏸", fontSize = 17.sp, color = TemproxColors.Ink) }

                Spacer(Modifier.size(12.dp))
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Live progression wallet for THIS match.
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    ) {
                        Text("🪙 ${engine.sessionCoins}", style = TemproxType.micro.copy(color = Color(0xFFB45309)))
                    }
                    Spacer(Modifier.size(10.dp))
                    PenaltyScore(engine)
                }
            }

            Spacer(Modifier.height(10.dp))
            if (engine.survival) {
                // Speed Math: the round gauge IS the match clock.
                QuestionTimeBar(engine)
            } else {
                GlobalTimeBar(engine)
            }

            Spacer(Modifier.height(12.dp))
            val reflex = engine.challenge as? Challenge.Reflex
            val reflexInstr = reflex?.let {
                stringResource(
                    R.string.reflex_find_fmt,
                    stringResource(REFLEX_SHAPE_NAMES[it.targetShape]),
                    stringResource(REFLEX_COLOR_NAMES[it.targetColor]),
                )
            }
            ChallengeBanner(
                engine.challenge,
                combo = engine.combo,
                instrOverride = reflexInstr,
                iconRes = reflex?.let { MEMORY_ICONS[it.targetShape] },
                iconTint = reflex?.let { MEMORY_TINTS[it.targetColor] },
            )
            Spacer(Modifier.height(16.dp))

            key(engine.challenge) {
                when (val ch = engine.challenge) {
                    is Challenge.Memory -> MemoryHost(
                        challenge = ch,
                        onWatchingChange = { memoryWatching = it },
                        onResolve = { ok, len -> engine.resolve(ok, memoryLength = len) },
                    )
                    is Challenge.Reflex -> ReflexHost(
                        challenge = ch,
                        onTarget = { perfect -> engine.resolve(true, perfect = perfect) },
                        onDecoy = { engine.resolve(false) },
                    )
                    is Challenge.Math -> MathHost(
                        challenge = ch,
                        onAnswer = { ok -> engine.resolve(ok) },
                    )
                    is Challenge.Attention -> AttentionHost(ch) { ok -> engine.resolve(ok) }
                }
            }

            Spacer(Modifier.height(14.dp))
            if (!engine.survival) QuestionTimeBar(engine)
        }

        if (paused && !engine.finished) {
            PauseOverlay(
                soundOn = soundOn,
                hapticsOn = hapticsOn,
                fxVolume = fxVolume,
                onToggleSound = {
                    soundOn = it; SoundManager.setEnabled(it); if (it) SoundManager.play(CLICK)
                },
                onToggleHaptics = { hapticsOn = it; SoundManager.setHapticsEnabled(it) },
                onVolumeChange = { fxVolume = it; SoundManager.setVolume(it) },
                onResume = { SoundManager.play(SoundManager.Sfx.CONFIRM); paused = false },
                onRestart = {
                    abandoned = false
                    paused = false
                    memoryWatching = false
                    sessionId++
                },
                onQuit = {
                    abandoned = true
                    paused = false
                    onQuit()
                },
            )
        }

        // Strike/timeout recovery — topmost layer, freezes every clock while up.
        if (engine.awaitingRecovery) {
            var adWatching by remember { mutableStateOf(false) }
            LaunchedEffect(adWatching) {
                if (adWatching) {
                    delay(MockAdManager.rewardedWaitMillis()) // VIP: instant revive
                    adWatching = false
                    SoundManager.play(SoundManager.Sfx.TROPHY)
                    engine.recoverWithAd()
                }
            }
            RecoveryOverlay(
                watching = adWatching,
                instant = vipInstant,
                onWatch = { adWatching = true },
                onDecline = { SoundManager.play(CLICK); engine.giveUpRecovery() },
            )
        }
    }
}

/** Floating red "-N" + shake on the score whenever a penalty lands. */
@Composable
private fun PenaltyScore(engine: GameEngine) {
    val shake = remember { Animatable(0f) }
    val floatY = remember { Animatable(0f) }
    val floatA = remember { Animatable(0f) }
    LaunchedEffect(engine.penaltyFlashKey) {
        if (engine.penaltyFlashKey > 0 && engine.lastPenalty > 0) {
            launch {
                repeat(3) {
                    shake.animateTo(-6f, tween(45))
                    shake.animateTo(6f, tween(45))
                }
                shake.animateTo(0f, tween(45))
            }
            floatY.snapTo(0f)
            floatA.snapTo(1f)
            launch { floatY.animateTo(-30f, tween(700)) }
            floatA.animateTo(0f, tween(700))
        }
    }
    Box(contentAlignment = Alignment.TopEnd) {
        Column(horizontalAlignment = Alignment.End) {
            Text(stringResource(R.string.hud_points), style = TemproxType.micro.copy(color = TemproxColors.Muted))
            Text(
                "${engine.score}",
                style = TemproxType.titleLg.copy(color = TemproxColors.Ink),
                modifier = Modifier.graphicsLayer { translationX = shake.value },
            )
        }
        Text(
            "-${engine.lastPenalty}",
            style = TemproxType.title.copy(color = TemproxColors.Danger),
            modifier = Modifier.graphicsLayer {
                translationY = floatY.value
                alpha = floatA.value
            },
        )
    }
}

/**
 * Full-screen recovery gate shown after 3 strikes (specialized modes) or the
 * first failure in Speed Math. Accepting plays a simulated ad: strikes reset,
 * clocks resume and coins earned so far are doubled (once per match).
 */
@Composable
private fun RecoveryOverlay(
    watching: Boolean,
    instant: Boolean = false,
    onWatch: () -> Unit,
    onDecline: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xB30F172A)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .clip(TemproxShapes.Card)
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), TemproxShapes.Card)
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🛟", fontSize = 34.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.recovery_title),
                style = TemproxType.bodyBold.copy(color = TemproxColors.Ink),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.recovery_body),
                style = TemproxType.caption.copy(color = Color(0xFF475569)),
            )
            Spacer(Modifier.height(18.dp))
            if (!watching) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(TemproxShapes.Button)
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))))
                        .clickable { SoundManager.play(SoundManager.Sfx.CONFIRM); onWatch() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        // VIP keeps the continue button — instant, no video wait.
                        stringResource(if (instant) R.string.recovery_vip_btn else R.string.recovery_watch_btn),
                        style = TemproxType.bodyBold.copy(color = Color(0xFF111827)),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.recovery_decline),
                    style = TemproxType.caption.copy(color = TemproxColors.Muted),
                    modifier = Modifier
                        .clickable(onClick = onDecline)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            } else {
                Text(
                    stringResource(R.string.recovery_ad_loading),
                    style = TemproxType.bodyBold.copy(color = Color(0xFFB45309)),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// Banner + combo chip
// ---------------------------------------------------------------------

@Composable
private fun ChallengeBanner(
    challenge: Challenge,
    combo: Int,
    instrOverride: String? = null,
    iconRes: Int? = null,
    iconTint: Color? = null,
) {
    val tint = TemproxColors.challengeColor(challenge.type)
    val nameRes = when (challenge.type) {
        ChallengeType.MEMORY -> R.string.challenge_memory_name
        ChallengeType.REFLEX -> R.string.challenge_reflex_name
        ChallengeType.MATH -> R.string.challenge_math_name
        ChallengeType.ATTENTION -> R.string.challenge_attention_name
    }
    val instrRes = when (challenge.type) {
        ChallengeType.MEMORY -> R.string.challenge_memory_instruction
        ChallengeType.REFLEX -> R.string.challenge_reflex_instruction
        ChallengeType.MATH -> R.string.challenge_math_instruction
        ChallengeType.ATTENTION -> R.string.challenge_attention_instruction
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(tint.copy(alpha = 0.15f))
                .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Target preview: kills the reading dependency for look-alike
            // shapes (solid disc vs ring) — see icon, then scan the grid.
            if (iconRes != null && iconTint != null) {
                Image(
                    painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    colorFilter = ColorFilter.tint(iconTint),
                )
                Spacer(Modifier.size(8.dp))
            }
            Text(stringResource(nameRes), style = TemproxType.bodyBold.copy(color = tint), maxLines = 1)
            Spacer(Modifier.size(10.dp))
            Text(instrOverride ?: stringResource(instrRes), style = TemproxType.caption.copy(color = Color(0xFF334155)), maxLines = 1)
        }
        if (combo >= 2) {
            Spacer(Modifier.size(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444))))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(stringResource(R.string.combo_chip, combo), style = TemproxType.micro.copy(color = Color.White))
            }
        }
    }
}

// ---------------------------------------------------------------------
// MEMORY — flash sequence, then tap palette in order.
// ---------------------------------------------------------------------

// Premium vector icon set (see res/drawable/ic_mem_*.xml) — one silhouette
// per shape family, shared gradient language and optical padding.
private val MEMORY_ICONS = listOf(
    R.drawable.ic_mem_triangle, R.drawable.ic_mem_square, R.drawable.ic_mem_star,
    R.drawable.ic_mem_circle, R.drawable.ic_mem_diamond, R.drawable.ic_mem_donut,
)
private val MEMORY_TINTS = listOf(
    Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFFF59E0B),
    Color(0xFF22C55E), Color(0xFFF97316), Color(0xFFA855F7),
)

@Composable
private fun MemoryHost(
    challenge: Challenge.Memory,
    onWatchingChange: (Boolean) -> Unit,
    onResolve: (correct: Boolean, length: Int) -> Unit,
) {
    var upTo by remember { mutableIntStateOf(0) } // slots flipped face-up during WATCH
    var inputCursor by remember { mutableIntStateOf(0) } // slots confirmed by the player
    var inputPhase by remember { mutableStateOf(false) }
    var wrongFlash by remember { mutableIntStateOf(0) }
    val watching = !inputPhase

    // Micro-interactions: scale pop on correct tap, horizontal shake on wrong.
    var pulseIdx by remember { mutableIntStateOf(-1) }
    var pulseKey by remember { mutableIntStateOf(0) }
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(pulseKey) {
        if (pulseKey > 0) {
            pulse.snapTo(0.85f)
            pulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    val shakeX = remember { Animatable(0f) }
    LaunchedEffect(wrongFlash) {
        if (wrongFlash > 0) {
            repeat(3) {
                shakeX.animateTo(-6f, tween(45))
                shakeX.animateTo(6f, tween(45))
            }
            shakeX.animateTo(0f, tween(45))
        }
    }

    LaunchedEffect(Unit) { onWatchingChange(true) }
    DisposableEffect(Unit) { onDispose { onWatchingChange(false) } }

    // Progressive in-slot reveal -> full-row hold -> simultaneous hide -> input.
    // The engine clock stays frozen (memoryWatching) until the flip-back ends.
    LaunchedEffect(challenge) {
        delay(400) // let the stage settle
        challenge.sequence.forEachIndexed { i, _ ->
            upTo = i + 1 // flips slot i face-up with its own pop pulse
            SoundManager.play(SoundManager.Sfx.FLIP)
            delay(560) // reveal pacing
        }
        delay(1700) // memorize window with every figure visible side-by-side
        upTo = 0 // simultaneous flip back to "?"
        delay(320)
        inputPhase = true // grid goes live, per-question clock resumes
        onWatchingChange(false)
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(if (watching) R.string.memory_watch_phase else R.string.memory_input_phase),
            style = TemproxType.bodyBold.copy(color = TemproxColors.Ink),
        )
        Spacer(Modifier.height(18.dp))

        // Sequence stage: the slots themselves present the figures (no central card).
        // Repeated shapes stay distinct because each slot flips individually.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val shown = if (inputPhase) inputCursor else upTo
            challenge.sequence.forEachIndexed { i, seqIdx ->
                MemFlipSlot(
                    iconRes = MEMORY_ICONS[seqIdx],
                    tint = MEMORY_TINTS[seqIdx],
                    faceUp = i < shown,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Answer grid — dimmed and inert until the reveal/hide cycle completes.
        // Tile layout is re-shuffled every round to break position muscle memory;
        // taps are matched by icon value, never by slot position.
        val tileOrder = remember(challenge) { MEMORY_ICONS.indices.shuffled() }
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.alpha(if (inputPhase) 1f else 0.35f),
        ) {
            tileOrder.chunked(3).forEach { rowIdx ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    rowIdx.forEach { i ->
                        val isWrongTile = wrongFlash > 0 && i == wrongFlash - 1
                        val isPulseTile = pulseKey > 0 && pulseIdx == i
                        Box(
                            Modifier
                                .size(64.dp)
                                .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0x33475569))
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isWrongTile) MEMORY_TINTS[i].copy(alpha = 0.25f) else Color.White)
                                .border(2.dp, MEMORY_TINTS[i], RoundedCornerShape(16.dp))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    if (!inputPhase || inputCursor >= challenge.sequence.size) return@clickable
                                    if (challenge.sequence[inputCursor] == i) {
                                        pulseIdx = i
                                        pulseKey++
                                        SoundManager.vibrate(longArrayOf(0, 18))
                                        inputCursor++
                                        if (inputCursor == challenge.sequence.size) {
                                            onResolve(true, challenge.sequence.size)
                                        }
                                    } else {
                                        pulseIdx = i
                                        pulseKey++ // shake rides on wrongFlash effect
                                        SoundManager.vibrate(longArrayOf(0, 40, 60, 40))
                                        wrongFlash = i + 1
                                        onResolve(false, 0)
                                    }
                                },
                        ) {
                            Image(
                                painterResource(MEMORY_ICONS[i]),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(34.dp)
                                    .align(Alignment.Center)
                                    .graphicsLayer {
                                        val s = if (isPulseTile) pulse.value else 1f
                                        scaleX = s
                                        scaleY = s
                                        translationX = if (isWrongTile) shakeX.value else 0f
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One card of the sequence stage. Flips around Y via graphicsLayer (draw-phase
 * value reads — zero recomposition per frame) and pops on every reveal so
 * repeated figures are unambiguous.
 */
@Composable
private fun MemFlipSlot(
    iconRes: Int,
    tint: Color,
    faceUp: Boolean,
    modifier: Modifier = Modifier,
) {
    val rot = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    val showFace by remember { derivedStateOf { rot.value >= 90f } }

    LaunchedEffect(faceUp) {
        if (faceUp) {
            launch {
                pop.snapTo(1.25f)
                pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
        }
        rot.animateTo(if (faceUp) 180f else 0f, tween(280))
    }

    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = if (showFace) rot.value - 180f else rot.value
                    cameraDistance = 16f * density
                    scaleX = pop.value
                    scaleY = pop.value
                }
                .shadow(3.dp, RoundedCornerShape(12.dp), spotColor = Color(0x33475569))
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(2.dp, if (faceUp) tint else Color(0xFFCBD5E1), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (showFace && faceUp) {
                Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.fillMaxSize(0.62f))
            } else {
                Text("?", style = TemproxType.titleLg.copy(color = TemproxColors.Muted))
            }
        }
    }
}

// ---------------------------------------------------------------------
// REFLEX — tap the golden target, avoid decoys.
// ---------------------------------------------------------------------

// Localized names for the visual-search instruction banner.
private val REFLEX_SHAPE_NAMES = listOf(
    R.string.mem_shape_triangle, R.string.mem_shape_square, R.string.mem_shape_star,
    R.string.mem_shape_circle, R.string.mem_shape_diamond, R.string.mem_shape_donut,
)
private val REFLEX_COLOR_NAMES = listOf(
    R.string.mem_color_red, R.string.mem_color_blue, R.string.mem_color_yellow,
    R.string.mem_color_green, R.string.mem_color_orange, R.string.mem_color_purple,
)

@Composable
private fun ReflexHost(
    challenge: Challenge.Reflex,
    onTarget: (perfect: Boolean) -> Unit,
    onDecoy: () -> Unit,
) {
    var startAt by remember(challenge) { mutableStateOf(System.currentTimeMillis()) }
    var wrongTap by remember { mutableStateOf(-1L) } // uid of the distractor that was hit
    val shakeX = remember { Animatable(0f) }
    LaunchedEffect(wrongTap) {
        if (wrongTap >= 0) {
            repeat(2) {
                shakeX.animateTo(-5f, tween(40))
                shakeX.animateTo(5f, tween(40))
            }
            shakeX.animateTo(0f, tween(40))
        }
    }

    // Dense visual-search grid: one unique shape+color combo hidden in noise.
    // Cells are matched by id, never by grid position.
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        challenge.cells.chunked(challenge.cols).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { cell ->
                    val isTarget = cell.id == challenge.targetId
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .graphicsLayer { translationX = if (wrongTap == cell.id) shakeX.value else 0f }
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.55f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                if (isTarget) {
                                    SoundManager.vibrate(longArrayOf(0, 18))
                                    onTarget(System.currentTimeMillis() - startAt < 900)
                                } else {
                                    SoundManager.vibrate(longArrayOf(0, 45, 60, 45))
                                    wrongTap = cell.id
                                    onDecoy()
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painterResource(MEMORY_ICONS[cell.shape]),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.72f),
                            colorFilter = ColorFilter.tint(MEMORY_TINTS[cell.color]),
                        )
                    }
                }
            }
        }
    }
}


// ---------------------------------------------------------------------
// MATH — multiple choice.
// ---------------------------------------------------------------------

@Composable
private fun MathHost(challenge: Challenge.Math, onAnswer: (Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(challenge.question + " = ?", style = TemproxType.titleLg.copy(color = TemproxColors.Ink, fontSize = 40.sp))
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            val correctValue = challenge.options[challenge.correctIndex]
            challenge.options.chunked(2).forEach { rowOpts ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowOpts.forEach { opt ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(62.dp)
                                .clip(TemproxShapes.Button)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2E8F0), TemproxShapes.Button)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { SoundManager.play(CLICK); onAnswer(opt == correctValue) },
                            contentAlignment = Alignment.Center,
                        ) { Text("$opt", style = TemproxType.title.copy(color = TemproxColors.Ink)) }
                    }
                    if (rowOpts.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}


// ---------------------------------------------------------------------
// ATTENTION — find the odd symbol.
// ---------------------------------------------------------------------

@Composable
private fun AttentionHost(challenge: Challenge.Attention, onAnswer: (Boolean) -> Unit) {
    var wrongTap by remember { mutableStateOf(-1L) } // uid of the cell that was hit
    val shakeX = remember { Animatable(0f) }
    LaunchedEffect(wrongTap) {
        if (wrongTap >= 0) {
            repeat(2) {
                shakeX.animateTo(-4f, tween(40))
                shakeX.animateTo(4f, tween(40))
            }
            shakeX.animateTo(0f, tween(40))
        }
    }

    // Dense sea of identical figures; the odd one differs only by a subtle
    // programmatic transform (rotation/scale/alpha/offset) set once per round.
    // Cells are matched by uid, never by grid position.
    Column(
        Modifier
            .fillMaxWidth()
            .clip(TemproxShapes.Card)
            .background(Color.White.copy(alpha = 0.03f))
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        challenge.slots.chunked(challenge.cols).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { slot ->
                    val odd = slot.odd
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .graphicsLayer { translationX = if (wrongTap == slot.uid) shakeX.value else 0f }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                if (odd) {
                                    SoundManager.vibrate(longArrayOf(0, 18))
                                    onAnswer(true)
                                } else {
                                    SoundManager.vibrate(longArrayOf(0, 45, 60, 45))
                                    wrongTap = slot.uid
                                    onAnswer(false)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            challenge.symbol,
                            fontSize = if (challenge.cols >= 9) 17.sp else 20.sp,
                            modifier = Modifier.graphicsLayer {
                                if (!odd) return@graphicsLayer
                                when (challenge.mut.kind) {
                                    0 -> rotationZ = challenge.mut.amount
                                    1 -> {
                                        scaleX = challenge.mut.amount
                                        scaleY = challenge.mut.amount
                                    }
                                    2 -> alpha = challenge.mut.amount
                                    else -> translationY = challenge.mut.amount * density
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// PAUSE overlay with audio settings.
// ---------------------------------------------------------------------

@Composable
private fun PauseOverlay(
    soundOn: Boolean,
    hapticsOn: Boolean,
    fxVolume: Float,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            // Light frosted scrim — web parity (bg #F8FAFC at ~98%)
            .background(TemproxColors.Background.copy(alpha = 0.98f))
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(22.dp)) {
            FloatingCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.pause_badge), style = TemproxType.micro.copy(color = TemproxColors.Primary))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.pause_heading), style = TemproxType.titleLg.copy(color = TemproxColors.Ink))
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.pause_subtitle), style = TemproxType.caption.copy(color = TemproxColors.Muted))

                    Spacer(Modifier.height(16.dp))
                    SettingsRow(stringResource(R.string.settings_sound), "🔊") {
                        Switch(checked = soundOn, onCheckedChange = onToggleSound, colors = pauseSwitchColors())
                    }
                    SettingsRow(stringResource(R.string.settings_vibration), "📳") {
                        Switch(checked = hapticsOn, onCheckedChange = onToggleHaptics, colors = pauseSwitchColors())
                    }
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text(stringResource(R.string.settings_volume), style = TemproxType.caption.copy(color = TemproxColors.Ink))
                        Slider(
                            value = fxVolume,
                            onValueChange = onVolumeChange,
                            colors = SliderDefaults.colors(
                                thumbColor = TemproxColors.Primary,
                                activeTrackColor = TemproxColors.Primary,
                                inactiveTrackColor = TemproxColors.BorderLight,
                            ),
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(text = stringResource(R.string.pause_resume), onClick = onResume)
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(text = stringResource(R.string.pause_restart), onClick = onRestart)
                    Spacer(Modifier.height(10.dp))
                    // Danger-tinted abandon action — mirrors the web pause menu.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(TemproxShapes.Button)
                            .background(TemproxColors.Danger.copy(alpha = 0.06f))
                            .border(1.dp, TemproxColors.Danger.copy(alpha = 0.35f), TemproxShapes.Button)
                            .clickable { SoundManager.play(SoundManager.Sfx.CLICK); onQuit() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.pause_quit),
                            style = TemproxType.bodyBold.copy(color = TemproxColors.Danger),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun pauseSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = TemproxColors.Primary,
    uncheckedThumbColor = Color(0xFF94A3B8),
    uncheckedTrackColor = TemproxColors.BorderLight,
)

@Composable
private fun SettingsRow(label: String, emoji: String, control: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 15.sp)
        Spacer(Modifier.size(8.dp))
        Text(label, style = TemproxType.bodyBold.copy(color = TemproxColors.Ink), modifier = Modifier.weight(1f))
        control()
    }
}

// ---------------------------------------------------------------------
// Linear time bars — continuous values are read during layout (fraction)
// and draw (badge offset) phases, so per-tick updates never recompose the
// challenge tree. Only rare discrete flips (integer second, urgency) do.
// ---------------------------------------------------------------------

/** Fractional-width helper: reads [value] at LAYOUT phase, not composition. */
private fun Modifier.fillFraction(value: () -> Float): Modifier =
    layout { measurable, constraints ->
        val w = (constraints.maxWidth * value().coerceIn(0f, 1f)).roundToInt()
        val placeable = measurable.measure(
            Constraints(minWidth = w, maxWidth = w),
        )
        layout(w, placeable.height) { placeable.placeRelative(0, 0) }
    }

/** Chunky global 60s bar with a floating seconds badge riding the fill tip;
 *  turns red under 10 seconds for the final urgency spike. */
@Composable
private fun GlobalTimeBar(engine: GameEngine) {
    val secondsLeft by remember { derivedStateOf { ceil(engine.timeLeftMillis / 1000f).toInt() } }
    val urgent by remember { derivedStateOf { engine.timeLeftMillis < 10_000L } }
    val frac = remember {
        derivedStateOf { (engine.timeLeftMillis / Progression.MATCH_MILLIS.toFloat()).coerceIn(0f, 1f) }
    }
    // Animated off-composition: the fraction value is only ever read during
    // layout (fill width) and draw (badge offset), never in composition.
    val anim = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        snapshotFlow { frac.value }
            .collectLatest { anim.animateTo(it, tween(160)) }
    }

    var barWidthPx by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(22.dp)
            .onSizeChanged { barWidthPx = it.width.toFloat() }
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE2E8F0))
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillFraction { anim.value }
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (urgent) {
                        Brush.horizontalGradient(listOf(Color(0xFFEF4444), TemproxColors.Danger))
                    } else {
                        // Neon purple -> electric blue: unmistakable against the track
                        Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF2563EB)))
                    },
                ),
        )
        Text(
            "${secondsLeft}s",
            style = TemproxType.bodyBold.copy(fontSize = 13.sp, color = Color.White),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    if (barWidthPx > 0f && size.width > 0f) {
                        alpha = 1f
                        // Ride the fill tip, clamped so the badge can never
                        // leave the bar bounds near 0% or 100%.
                        translationX = (barWidthPx * anim.value - size.width / 2f)
                            .coerceIn(8.dp.toPx(), (barWidthPx - size.width - 8.dp.toPx()).coerceAtLeast(8.dp.toPx()))
                    } else {
                        alpha = 0f
                    }
                }
                .shadow(3.dp, RoundedCornerShape(999.dp), spotColor = Color(0x33475569))
                .background(if (urgent) TemproxColors.Danger else Color(0xFF7C3AED), RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

/** Fast per-question urgency bar below the interaction zone — vibrant
 *  orange fill with a live seconds badge riding the fill tip; turns red
 *  under 30% remaining. Mirrors GlobalTimeBar's zero-recomposition model:
 *  the fraction is only read during layout (fill width) and draw (badge
 *  offset); the badge text flips at most once per second. */
@Composable
private fun QuestionTimeBar(engine: GameEngine) {
    val limit = engine.challenge.limitMillis.coerceAtLeast(1L).toFloat()
    val remain = remember(engine.challenge) {
        derivedStateOf { (1f - engine.challengeElapsedMillis / limit).coerceIn(0f, 1f) }
    }
    val low by remember(engine.challenge) {
        derivedStateOf { engine.challengeElapsedMillis / limit >= 0.7f }
    }
    val secondsLeft by remember(engine.challenge) {
        derivedStateOf {
            ceil((limit - engine.challengeElapsedMillis) / 1000f).toInt().coerceAtLeast(0)
        }
    }
    // Sub-1.5s tension pulse: flips composition once at the threshold, the
    // alpha oscillation itself rides the draw phase for free.
    val blink by remember(engine.challenge) {
        derivedStateOf { limit - engine.challengeElapsedMillis <= 1499f }
    }
    val pulse = rememberInfiniteTransition(label = "roundBlink")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(260), RepeatMode.Reverse),
        label = "roundBlinkAlpha",
    )
    // Animated off-composition. Keyed per challenge so each round restarts
    // the gauge full (no backward refill sweep between questions).
    val anim = remember { Animatable(1f) }
    LaunchedEffect(engine.challenge) {
        anim.snapTo(1f)
        snapshotFlow { remain.value }
            .collectLatest { anim.animateTo(it, tween(100)) }
    }

    var barWidthPx by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(16.dp)
            .onSizeChanged { barWidthPx = it.width.toFloat() }
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE2E8F0))
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillFraction { anim.value }
                .graphicsLayer { alpha = if (blink) pulseAlpha else 1f }
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (low) {
                        Brush.horizontalGradient(listOf(Color(0xFFEF4444), TemproxColors.Danger))
                    } else {
                        Brush.horizontalGradient(listOf(Color(0xFFFB923C), Color(0xFFF97316)))
                    },
                ),
        )
        Text(
            "${secondsLeft}s",
            style = TemproxType.micro.copy(color = Color.White),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    if (barWidthPx > 0f && size.width > 0f) {
                        alpha = 1f
                        // Ride the fill tip, clamped inside bar bounds near 0%/100%.
                        translationX = (barWidthPx * anim.value - size.width / 2f)
                            .coerceIn(6.dp.toPx(), (barWidthPx - size.width - 6.dp.toPx()).coerceAtLeast(6.dp.toPx()))
                    } else {
                        alpha = 0f
                    }
                }
                .shadow(2.dp, RoundedCornerShape(999.dp), spotColor = Color(0x33475569))
                .background(if (low) TemproxColors.Danger else Color(0xFFF97316), RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp, vertical = 1.dp),
        )
    }
}
