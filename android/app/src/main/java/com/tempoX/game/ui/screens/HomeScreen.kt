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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.tempoX.game.game.BillingRepository
import com.tempoX.game.game.EconomyRepository
import com.tempoX.game.game.EconomyState
import com.tempoX.game.game.GameMode
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
import kotlinx.coroutines.delay

/** Home hub: play / stats / trophies + rules modal + optional match seed. */
@Composable
fun HomeScreen(
    stats: PlayerStats,
    economy: EconomyState,
    billing: BillingRepository,
    onStartMatch: (mode: GameMode, seedText: String) -> Unit,
    language: LangMode = LangMode.SYSTEM,
    onLanguageChange: (LangMode) -> Unit = {},
    onUnlockWithCoins: (GameMode) -> Boolean = { false },
    onUnlockWithAd: (GameMode) -> Unit = {},
) {
    val context = LocalContext.current
    val adFree by billing.isAdFreeUser.collectAsState()
    var tab by remember { mutableStateOf(HomeTab.PLAY) }
    // First-launch onboarding only — persisted flag survives navigation.
    var showRules by remember {
        val prefs = context.getSharedPreferences("temprox_settings", android.content.Context.MODE_PRIVATE)
        mutableStateOf(!prefs.getBoolean("rules_seen", false))
    }
    var seedText by remember { mutableStateOf("") }
    var unlockFor by remember { mutableStateOf<GameMode?>(null) }
    var showPurchaseSheet by remember { mutableStateOf(false) }
    var purchaseError by remember { mutableStateOf(false) }

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
        com.tempoX.game.ui.components.FormulaLayer(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(14.dp))

            // ---- Header -------------------------------------------------
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .clickable { showRules = true },
                    contentAlignment = Alignment.Center,
                ) { Text("☰", fontSize = 19.sp, color = TemproxColors.Ink) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.home_title),
                        style = TemproxType.title.copy(color = TemproxColors.Ink),
                        maxLines = 1,
                    )
                    Text(stringResource(R.string.tagline), style = TemproxType.micro.copy(color = TemproxColors.Muted))
                }
                Spacer(Modifier.width(10.dp))
                TopChip(
                    glyph = "👑",
                    value = "L$level",
                    onClick = { tab = HomeTab.TROPHIES },
                )
                Spacer(Modifier.width(8.dp))
                TopChip(
                    glyph = "⭐",
                    value = "${stats.highScore}",
                    onClick = { tab = HomeTab.STATS },
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when (tab) {
                    HomeTab.PLAY -> PlayTab(
                        stats = stats,
                        level = level,
                        levelProgress = levelProgress,
                        economy = economy,
                        seedText = seedText,
                        onSeedChange = { seedText = it },
                        onPlay = { mode, seed ->
                            if (mode == GameMode.ARCADE || mode in economy.unlockedModes) {
                                onStartMatch(mode, seed)
                            } else {
                                unlockFor = mode
                            }
                        },
                    )
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

        if (showRules) RulesSheet(
                language = language,
                onLanguageChange = onLanguageChange,
                onClose = {
                    context.getSharedPreferences("temprox_settings", android.content.Context.MODE_PRIVATE)
                        .edit().putBoolean("rules_seen", true).apply()
                    showRules = false
                },
            )

        unlockFor?.let { mode ->
            UnlockModeDialog(
                mode = mode,
                coins = economy.coins,
                onUseCoins = {
                    if (onUnlockWithCoins(mode)) {
                        SoundManager.play(SoundManager.Sfx.TROPHY)
                        SoundManager.vibrate(longArrayOf(0, 30, 40, 30))
                        unlockFor = null
                    }
                },
                onWatchAd = {
                    SoundManager.play(SoundManager.Sfx.TROPHY)
                    onUnlockWithAd(mode)
                    unlockFor = null
                },
                onDismiss = { unlockFor = null },
            )
        }

        // Simulated Google Play purchase sheet for the Remove Ads product.
        if (showPurchaseSheet) {
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
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("💳", fontSize = 34.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.purchase_title),
                        style = TemproxType.bodyBold.copy(color = TemproxColors.Ink),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.purchase_body),
                        style = TemproxType.caption.copy(color = Color(0xFF475569)),
                    )
                    if (purchaseError) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.purchase_error),
                            style = TemproxType.micro.copy(color = TemproxColors.Danger),
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(TemproxColors.Primary)
                            .clickable {
                                billing.purchaseRemoveAds(
                                    onSuccess = {
                                        SoundManager.play(SoundManager.Sfx.TROPHY)
                                        SoundManager.vibrate(longArrayOf(0, 30, 40, 30))
                                        showPurchaseSheet = false
                                    },
                                    onError = { purchaseError = true },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.purchase_confirm),
                            style = TemproxType.bodyBold.copy(color = Color.White),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.purchase_cancel),
                        style = TemproxType.caption.copy(color = TemproxColors.Muted),
                        modifier = Modifier
                            .clickable { showPurchaseSheet = false }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------

@Composable
private fun PlayTab(
    stats: PlayerStats,
    level: Int,
    levelProgress: Float,
    economy: EconomyState,
    seedText: String,
    onSeedChange: (String) -> Unit,
    onPlay: (GameMode, String) -> Unit,
) {
    Column {
        // ---- Mode cards (specialized modes are economy-locked) --------
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TaskCard(
                header = stringResource(R.string.card_pattern_header),
                caption = stringResource(R.string.card_pattern_caption),
                flow = "🧠  ▸  △ ▢ ◯  ▸  ⊘  ▸  ● ■ ▲ ✓",
                tint = Color(0xFF22C55E),
                glyph = "🧠",
                locked = GameMode.SHAPE !in economy.unlockedModes,
                modifier = Modifier.weight(1f),
            ) { onPlay(GameMode.SHAPE, "") }
            TaskCard(
                header = stringResource(R.string.card_calc_header),
                caption = stringResource(R.string.card_calc_caption),
                flow = "15 − 8 = ?  ▸  ▦▦▦  ▸  7 ✓",
                tint = Color(0xFF3B82F6),
                glyph = "⚡",
                locked = GameMode.MATH !in economy.unlockedModes,
                modifier = Modifier.weight(1f),
            ) { onPlay(GameMode.MATH, "") }
        }
        Spacer(Modifier.height(16.dp))

        // Premium / Remove-Ads card (ARCADE PREMIUM spot)
        Box(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(TemproxColors.Accent.copy(alpha = if (adFree) 0.22f else 0.15f))
                .border(
                    1.dp,
                    TemproxColors.Accent.copy(alpha = if (adFree) 0.6f else 0.45f),
                    RoundedCornerShape(999.dp),
                )
                .clickable(enabled = !adFree) {
                    SoundManager.play(SoundManager.Sfx.CLICK)
                    purchaseError = false
                    showPurchaseSheet = true
                }
                .padding(horizontal = 12.dp, vertical = 5.dp),
        ) {
            Text(
                stringResource(if (adFree) R.string.premium_vip_badge else R.string.premium_remove_btn),
                style = TemproxType.micro.copy(color = TemproxColors.Accent),
            )
        }
        Spacer(Modifier.height(16.dp))

        // Best score hero card
        FloatingCard(accent = TemproxColors.Accent) {
            Text(stringResource(R.string.home_best_score), style = TemproxType.caption.copy(color = Color(0xFFB45309)))
            Text("${stats.highScore}", style = TemproxType.score.copy(color = TemproxColors.Ink))
            Spacer(Modifier.height(10.dp))

            Text(stringResource(R.string.home_level_profile), style = TemproxType.micro.copy(color = TemproxColors.Muted))
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
                    style = TemproxType.micro.copy(color = TemproxColors.Muted),
                )
                val floor = Progression.xpForLevel(level)
                val cost = Progression.xpForLevel(level + 1)
                val pct = if (cost <= floor) 100 else ((stats.totalXp - floor) * 100 / (cost - floor)).toInt().coerceIn(0, 100)
                Text(
                    stringResource(R.string.home_percent_to_level, pct, level + 1),
                    style = TemproxType.micro.copy(color = TemproxColors.Muted),
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
        PrimaryButton(text = stringResource(R.string.home_play_now), onClick = { onPlay(GameMode.ARCADE, seedText) })
    }
}

/**
 * Unlock gate for specialized modes: 150 coins offline purchase or a
 * simulated rewarded video. Coin path always works — network failures on
 * the ad path just leave the dialog open with a retry hint.
 */
@Composable
private fun UnlockModeDialog(
    mode: GameMode,
    coins: Int,
    onUseCoins: () -> Unit,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit,
) {
    var adLoading by remember { mutableStateOf(false) }
    var adFailed by remember { mutableStateOf(false) }
    val modeName = stringResource(
        if (mode == GameMode.MATH) R.string.card_calc_header else R.string.card_pattern_header,
    )
    LaunchedEffect(adLoading) {
        if (adLoading) {
            delay(1600) // simulated rewarded video
            adLoading = false
            if ((0..99).random() < 20) adFailed = true else onWatchAd()
        }
    }
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
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🔓", fontSize = 34.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.unlock_title, modeName),
                style = TemproxType.bodyBold.copy(color = TemproxColors.Ink),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.unlock_body, coins),
                style = TemproxType.caption.copy(color = Color(0xFF475569)),
            )
            Spacer(Modifier.height(18.dp))
            // Offline coin purchase — disabled while short on balance.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (coins >= EconomyRepository.UNLOCK_COST) TemproxColors.Primary else Color(0xFFE2E8F0))
                    .clickable(enabled = coins >= EconomyRepository.UNLOCK_COST && !adLoading) {
                        SoundManager.play(SoundManager.Sfx.CLICK)
                        onUseCoins()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.unlock_coins_btn, EconomyRepository.UNLOCK_COST),
                    style = TemproxType.bodyBold.copy(color = Color.White),
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))))
                    .clickable(enabled = !adLoading) { SoundManager.play(SoundManager.Sfx.CLICK); adLoading = true },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (adLoading) stringResource(R.string.unlock_ad_loading) else stringResource(R.string.unlock_ad_btn),
                    style = TemproxType.bodyBold.copy(color = Color(0xFF111827)),
                )
            }
            if (adFailed) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.unlock_ad_fail),
                    style = TemproxType.micro.copy(color = TemproxColors.Danger),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.unlock_close),
                style = TemproxType.caption.copy(color = TemproxColors.Muted),
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
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


/** Small circular stat chip for the top bar (crown = level, star = record). */
@Composable
private fun TopChip(glyph: String, value: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .clickable { SoundManager.play(SoundManager.Sfx.CLICK); onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(glyph, fontSize = 15.sp)
        Text(value, style = TemproxType.micro.copy(color = TemproxColors.Muted), maxLines = 1)
    }
}

/** Horizontal featured-task card: colored rail + stylized flow + affordance. */
@Composable
private fun TaskCard(
    header: String,
    caption: String,
    flow: String,
    tint: Color,
    glyph: String,
    locked: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .height(112.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = if (locked) 0.72f else 1f))
            .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .clickable {
                SoundManager.play(SoundManager.Sfx.CLICK)
                SoundManager.vibrate(longArrayOf(0, 18))
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(46.dp)
                .fillMaxHeight()
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Text(glyph, fontSize = 22.sp) }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                header,
                style = TemproxType.micro.copy(color = tint, letterSpacing = 1.6.sp),
            )
            Spacer(Modifier.height(3.dp))
            Text(caption, style = TemproxType.bodyBold.copy(color = TemproxColors.Ink), maxLines = 1)
            Spacer(Modifier.height(5.dp))
            Text(flow, style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569)), maxLines = 1)
        }
        Box(
            Modifier
                .padding(end = 10.dp)
                .size(30.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) { Text(if (locked) "🔒" else "▶", fontSize = 12.sp, color = tint) }
    }
}
