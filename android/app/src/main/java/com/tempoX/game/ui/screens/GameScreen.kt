package com.tempoX.game.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tempoX.game.R
import com.tempoX.game.audio.SoundManager
import com.tempoX.game.audio.SoundManager.Sfx.CLICK
import com.tempoX.game.game.Challenge
import com.tempoX.game.game.ChallengeType
import com.tempoX.game.game.GameEngine
import com.tempoX.game.game.MatchSummary
import com.tempoX.game.game.Progression
import com.tempoX.game.ui.components.CircularTimer
import com.tempoX.game.ui.components.FloatingCard
import com.tempoX.game.ui.components.PrimaryButton
import com.tempoX.game.ui.components.SecondaryButton
import com.tempoX.game.ui.theme.TemproxColors
import com.tempoX.game.ui.theme.TemproxShapes
import com.tempoX.game.ui.theme.TemproxType
import kotlinx.coroutines.delay
import kotlin.math.ceil

private const val TICK_MS = 60L

/** In-match screen: HUD + active challenge host + pause overlay. */
@Composable
fun GameScreen(
    seedText: String,
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
        GameEngine(seedText.hashCode().toLong()).also { eng ->
            eng.onEvent = { ev ->
                when (ev) {
                    GameEngine.Event.CORRECT -> SoundManager.play(SoundManager.Sfx.SUCCESS)
                    GameEngine.Event.WRONG -> SoundManager.play(SoundManager.Sfx.ERROR)
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
        if (!abandoned && !paused) {
            onFinish(
                MatchSummary(
                    score = engine.score,
                    totalCorrect = engine.totalCorrect,
                    totalIncorrect = engine.totalIncorrect,
                    maxCombo = engine.maxCombo,
                    xpGained = engine.xpGained,
                    completed = engine.timeLeftMillis == 0L,
                ),
                engine,
            )
        }
    }

    val secondsLeft = ceil(engine.timeLeftMillis / 1000f).toInt()

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF191238), Color(0xFF0B081C)))),
    ) {
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
                        .background(Color.White.copy(alpha = 0.07f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .clickable { SoundManager.play(CLICK); paused = true },
                    contentAlignment = Alignment.Center,
                ) { Text("⏸", fontSize = 17.sp, color = Color.White) }

                Spacer(Modifier.size(12.dp))
                CircularTimer(
                    remainingFraction = engine.timeLeftMillis.toFloat() / Progression.MATCH_MILLIS,
                    secondsLeft = secondsLeft,
                )
                Spacer(Modifier.size(12.dp))
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.hud_points), style = TemproxType.micro.copy(color = Color(0xFF9A94B5)))
                    Text("${engine.score}", style = TemproxType.titleLg.copy(color = Color.White))
                }
            }

            Spacer(Modifier.height(14.dp))
            ChallengeBanner(engine.challenge, combo = engine.combo)
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
                onResume = { SoundManager.play(CLICK); paused = false },
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
    }
}

// ---------------------------------------------------------------------
// Banner + combo chip
// ---------------------------------------------------------------------

@Composable
private fun ChallengeBanner(challenge: Challenge, combo: Int) {
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
            Text(stringResource(nameRes), style = TemproxType.bodyBold.copy(color = tint), maxLines = 1)
            Spacer(Modifier.size(10.dp))
            Text(stringResource(instrRes), style = TemproxType.caption.copy(color = Color(0xFFC9C4DE)), maxLines = 1)
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

private val MEMORY_SHAPES = listOf("🔺", "🟦", "⭐", "🟢", "🔶", "🟣")
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
    var cursor by remember { mutableIntStateOf(0) } // flash position during WATCHING
    var inputCursor by remember { mutableIntStateOf(0) }
    var wrongFlash by remember { mutableIntStateOf(0) }
    val watching = cursor < challenge.sequence.size

    LaunchedEffect(Unit) { onWatchingChange(true) }
    DisposableEffect(Unit) { onDispose { onWatchingChange(false) } }

    // Advance the flash sequence while watching.
    LaunchedEffect(watching) {
        while (cursor < challenge.sequence.size) {
            delay(700)
            cursor++
        }
        onWatchingChange(false)
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(if (watching) R.string.memory_watch_phase else R.string.memory_input_phase),
            style = TemproxType.bodyBold.copy(color = Color.White),
        )
        Spacer(Modifier.height(18.dp))

        if (watching) {
            val idx = challenge.sequence[cursor]
            Box(
                Modifier
                    .size(150.dp)
                    .clip(TemproxShapes.Card)
                    .background(MEMORY_TINTS[idx].copy(alpha = 0.25f))
                    .border(3.dp, MEMORY_TINTS[idx], TemproxShapes.Card),
                contentAlignment = Alignment.Center,
            ) { Text(MEMORY_SHAPES[idx], fontSize = 64.sp) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                challenge.sequence.forEachIndexed { i, seqIdx ->
                    Box(
                        Modifier
                            .size(if (i < inputCursor) 34.dp else 28.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (i < inputCursor) MEMORY_TINTS[seqIdx] else Color.White.copy(alpha = 0.07f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(if (i < inputCursor) "✓" else "?", color = Color.White, fontSize = 12.sp) }
                }
            }
            Spacer(Modifier.height(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MEMORY_SHAPES.indices.chunked(3).forEach { rowIdx ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        rowIdx.forEach { i ->
                            Box(
                                Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MEMORY_TINTS[i].copy(alpha = if (wrongFlash > 0 && i == wrongFlash - 1) 0.5f else 0.16f))
                                    .border(2.dp, MEMORY_TINTS[i], RoundedCornerShape(16.dp))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        if (inputCursor < challenge.sequence.size) {
                                            if (challenge.sequence[inputCursor] == i) {
                                                inputCursor++
                                                if (inputCursor == challenge.sequence.size) {
                                                    onResolve(true, challenge.sequence.size)
                                                }
                                            } else {
                                                wrongFlash = i + 1
                                                onResolve(false, 0)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) { Text(MEMORY_SHAPES[i], fontSize = 30.sp) }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// REFLEX — tap the golden target, avoid decoys.
// ---------------------------------------------------------------------

@Composable
private fun ReflexHost(
    challenge: Challenge.Reflex,
    onTarget: (perfect: Boolean) -> Unit,
    onDecoy: () -> Unit,
) {
    var startAt by remember { mutableStateOf(System.currentTimeMillis()) }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(TemproxShapes.Card)
            .background(Color.White.copy(alpha = 0.03f)),
    ) {
        LaunchedEffect(Unit) { startAt = System.currentTimeMillis() }
        Column(Modifier.fillMaxSize()) {
            repeat(3) { r ->
                Row(Modifier.weight(1f)) {
                    repeat(3) { c ->
                        val index = r * 3 + c
                        val isTarget = index == challenge.targetCell
                        val isDecoy = index in challenge.decoyCells
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(5.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    when {
                                        isTarget -> Brush.radialGradient(listOf(Color(0xFFFFC93D), Color(0xFFF59E0B)))
                                        isDecoy -> Brush.radialGradient(listOf(Color(0xFFF87171), Color(0xFFDC2626)))
                                        else -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    }
                                )
                                .clickable {
                                    when {
                                        isTarget -> onTarget(System.currentTimeMillis() - startAt < 250)
                                        isDecoy -> onDecoy()
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isTarget) Text("🎯", fontSize = 30.sp)
                            if (isDecoy) Text("💥", fontSize = 24.sp)
                        }
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
        Text(challenge.question + " = ?", style = TemproxType.titleLg.copy(color = Color.White, fontSize = 40.sp))
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
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.18f), TemproxShapes.Button)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { SoundManager.play(CLICK); onAnswer(opt == correctValue) },
                            contentAlignment = Alignment.Center,
                        ) { Text("$opt", style = TemproxType.title.copy(color = Color.White)) }
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
    Column(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(TemproxShapes.Card)
            .background(Color.White.copy(alpha = 0.03f))
            .padding(8.dp),
    ) {
        repeat(challenge.rows) { r ->
            Row(Modifier.weight(1f)) {
                repeat(challenge.cols) { c ->
                    val index = r * challenge.cols + c
                    val odd = index == challenge.oddIndex
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onAnswer(odd) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (odd) challenge.oddSymbol else challenge.baseSymbol,
                            fontSize = 26.sp,
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
            .background(Color.Black.copy(alpha = 0.78f))
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(22.dp)) {
            FloatingCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.pause_badge), style = TemproxType.micro.copy(color = TemproxColors.Warning))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.pause_heading), style = TemproxType.titleLg.copy(color = Color.White))
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.pause_subtitle), style = TemproxType.caption.copy(color = Color(0xFFB7B2CE)))

                    Spacer(Modifier.height(16.dp))
                    SettingsRow(stringResource(R.string.settings_sound), "🔊") {
                        Switch(checked = soundOn, onCheckedChange = onToggleSound, colors = pauseSwitchColors())
                    }
                    SettingsRow(stringResource(R.string.settings_vibration), "📳") {
                        Switch(checked = hapticsOn, onCheckedChange = onToggleHaptics, colors = pauseSwitchColors())
                    }
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text(stringResource(R.string.settings_volume), style = TemproxType.caption.copy(color = Color.White))
                        Slider(
                            value = fxVolume,
                            onValueChange = onVolumeChange,
                            colors = SliderDefaults.colors(
                                thumbColor = TemproxColors.Accent,
                                activeTrackColor = TemproxColors.Accent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                            ),
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(text = stringResource(R.string.pause_resume), onClick = onResume)
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(text = stringResource(R.string.pause_restart), onClick = onRestart)
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(text = stringResource(R.string.pause_quit), onClick = onQuit)
                }
            }
        }
    }
}

@Composable
private fun pauseSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = TemproxColors.Accent,
    checkedTrackColor = TemproxColors.Accent.copy(alpha = 0.35f),
    uncheckedThumbColor = Color(0xFF6E6890),
    uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
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
        Text(label, style = TemproxType.bodyBold.copy(color = Color.White), modifier = Modifier.weight(1f))
        control()
    }
}
