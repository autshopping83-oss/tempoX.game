package com.tempoX.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tempoX.game.R
import com.tempoX.game.audio.SoundManager
import com.tempoX.game.game.LangMode
import com.tempoX.game.game.PlayerStats
import com.tempoX.game.game.Progression
import com.tempoX.game.ui.components.BottomNavigation
import com.tempoX.game.ui.components.BrandedProgress
import com.tempoX.game.ui.components.FloatingCard
import com.tempoX.game.ui.components.HomeTab
import com.tempoX.game.ui.components.PrimaryButton
import com.tempoX.game.ui.components.SectionLabel
import com.tempoX.game.ui.components.StatCard
import com.tempoX.game.ui.components.TemproxLogo
import com.tempoX.game.ui.components.TrophyCard
import com.tempoX.game.ui.theme.TemproxColors
import com.tempoX.game.ui.theme.TemproxType

/** Home hub: play / stats / trophies + rules modal + optional match seed. */
@Composable
fun HomeScreen(
    stats: PlayerStats,
    onStartMatch: (seedText: String) -> Unit,
    language: LangMode = LangMode.SYSTEM,
    onLanguageChange: (LangMode) -> Unit = {},
) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(HomeTab.PLAY) }
    var showRules by remember { mutableStateOf(true) } // first-launch onboarding, like the web build
    var soundOn by remember { mutableStateOf(SoundManager.isEnabled()) }
    var seedText by remember { mutableStateOf("") }

    val level = Progression.levelForXp(stats.totalXp)
    val currentLevelFloor = Progression.xpForLevel(level)
    val nextLevelCost = Progression.xpForLevel(level + 1)
    val levelProgress =
        if (nextLevelCost <= currentLevelFloor) 1f
        else (stats.totalXp - currentLevelFloor).toFloat() / (nextLevelCost - currentLevelFloor)

    Box(
        Modifier
            .fillMaxSize(),
    ) {
        com.tempoX.game.ui.components.AnimatedBackground(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(14.dp))

            // ---- Header -------------------------------------------------
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    TemproxLogo(heightText = 26)
                    Text(stringResource(R.string.tagline), style = TemproxType.micro.copy(color = TemproxColors.Muted))
                }
                // Language selector — cycles Automatic → Português → English.
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .clickable { onLanguageChange(com.tempoX.game.game.LanguageManager.next(language)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🌐", fontSize = 17.sp)
                    Text(
                        when (language) {
                            LangMode.SYSTEM -> stringResource(R.string.lang_auto).take(3).uppercase()
                            LangMode.PT -> "PT"
                            LangMode.EN -> "EN"
                        },
                        style = TemproxType.micro.copy(color = TemproxColors.Accent),
                        modifier = Modifier.padding(top = 26.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .clickable {
                            soundOn = !soundOn
                            SoundManager.setEnabled(soundOn)
                            if (soundOn) SoundManager.play(SoundManager.Sfx.CLICK)
                        },
                    contentAlignment = Alignment.Center,
                ) { Text(if (soundOn) "🔊" else "🔇", fontSize = 17.sp) }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when (tab) {
                    HomeTab.PLAY -> PlayTab(stats, level, levelProgress, seedText, { seedText = it }, onStartMatch)
                    HomeTab.STATS -> StatsTab(stats)
                    HomeTab.TROPHIES -> TrophiesTab(stats)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ---- Bottom nav pinned ------------------------------------------
        Box(Modifier.align(Alignment.BottomCenter)) {
            BottomNavigation(
                current = tab,
                onSelect = { tab = it },
                labels = mapOf(
                    HomeTab.PLAY to stringResource(R.string.tab_play),
                    HomeTab.STATS to stringResource(R.string.tab_stats),
                    HomeTab.TROPHIES to stringResource(R.string.tab_trophies),
                ),
            )
        }

        if (showRules) RulesSheet(onClose = { showRules = false })
    }
}

// ---------------------------------------------------------------------

@Composable
private fun PlayTab(
    stats: PlayerStats,
    level: Int,
    levelProgress: Float,
    seedText: String,
    onSeedChange: (String) -> Unit,
    onStartMatch: (String) -> Unit,
) {
    Column {
        // Premium badge
        Box(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(TemproxColors.Accent.copy(alpha = 0.15f))
                .border(1.dp, TemproxColors.Accent.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp),
        ) {
            Text(stringResource(R.string.home_premium_badge), style = TemproxType.micro.copy(color = TemproxColors.Accent))
        }
        Spacer(Modifier.height(16.dp))

        // Best score hero card
        FloatingCard(accent = TemproxColors.Accent) {
            Text(stringResource(R.string.home_best_score), style = TemproxType.caption.copy(color = TemproxColors.Accent))
            Text("${stats.highScore}", style = TemproxType.score.copy(color = Color.White))
            Spacer(Modifier.height(10.dp))

            Text(stringResource(R.string.home_level_profile), style = TemproxType.micro.copy(color = Color(0xFF9A94B5)))
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.home_level, level),
                    style = TemproxType.title.copy(color = TemproxColors.Pink),
                    modifier = Modifier.width(110.dp),
                )
                BrandedProgress(fraction = levelProgress, tint = TemproxColors.Pink)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.home_xp_accumulated, stats.totalXp),
                    style = TemproxType.micro.copy(color = Color(0xFF9A94B5)),
                )
                val floor = Progression.xpForLevel(level)
                val cost = Progression.xpForLevel(level + 1)
                val pct = if (cost <= floor) 100 else ((stats.totalXp - floor) * 100 / (cost - floor)).toInt().coerceIn(0, 100)
                Text(
                    stringResource(R.string.home_percent_to_level, pct, level + 1),
                    style = TemproxType.micro.copy(color = Color(0xFF9A94B5)),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Optional seed
        SectionLabel(stringResource(R.string.home_seed_label))
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = seedText,
            onValueChange = onSeedChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TemproxColors.Ink),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                ) {
                    if (seedText.isEmpty()) Text(
                        stringResource(R.string.home_seed_placeholder),
                        style = TemproxType.body.copy(color = Color(0xFF94A3B8)),
                    )
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        PrimaryButton(text = stringResource(R.string.home_play_now), onClick = { onStartMatch(seedText) })
    }
}

@Composable
private fun StatsTab(stats: PlayerStats) {
    Column {
        SectionLabel(stringResource(R.string.stats_historical_title), TemproxColors.Info)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.stats_matches), "${stats.gamesPlayed}", Modifier.weight(1f), TemproxColors.Primary)
            StatCard(stringResource(R.string.stats_record), "${stats.highScore}", Modifier.weight(1f), TemproxColors.Warning)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.stats_total_xp), "${stats.totalXp}", Modifier.weight(1f), TemproxColors.Success)
            StatCard(stringResource(R.string.stats_max_combo), "x${stats.maxComboEver}", Modifier.weight(1f), TemproxColors.Pink)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(stringResource(R.string.stats_answers), "${stats.totalCorrect + stats.totalIncorrect}", Modifier.weight(1f), TemproxColors.Info)
            val total = stats.totalCorrect + stats.totalIncorrect
            val acc = if (total == 0) 0 else stats.totalCorrect * 100 / total
            StatCard(stringResource(R.string.stats_avg_accuracy), "$acc%", Modifier.weight(1f), TemproxColors.Green)
        }
    }
}

@Composable
private fun TrophiesTab(stats: PlayerStats) {
    Column {
        SectionLabel(
            stringResource(R.string.trophies_title, stats.achievements.size),
            TemproxColors.Accent,
        )
        Spacer(Modifier.height(12.dp))
        com.tempoX.game.game.Achievements.ALL.forEach { id ->
            TrophyCard(
                title = stringResource(com.tempoX.game.game.Achievements.titleRes(id)),
                description = stringResource(com.tempoX.game.game.Achievements.descRes(id)),
                unlocked = id in stats.achievements,
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}
