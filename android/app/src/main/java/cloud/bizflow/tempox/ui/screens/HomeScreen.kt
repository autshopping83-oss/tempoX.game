package cloud.bizflow.tempox.ui.screens

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.bizflow.tempox.BuildConfig
import cloud.bizflow.tempox.R
import cloud.bizflow.tempox.audio.SoundManager
import cloud.bizflow.tempox.game.BillingRepository
import cloud.bizflow.tempox.game.EconomyRepository
import cloud.bizflow.tempox.game.EconomyState
import cloud.bizflow.tempox.game.GameMode
import cloud.bizflow.tempox.monetization.AdMobManager
import cloud.bizflow.tempox.game.LangMode
import cloud.bizflow.tempox.game.PlayerStats
import cloud.bizflow.tempox.game.Progression
import cloud.bizflow.tempox.ui.LegalType
import cloud.bizflow.tempox.ui.components.BottomNavigation
import cloud.bizflow.tempox.ui.components.BrandedProgress
import cloud.bizflow.tempox.ui.components.FloatingCard
import cloud.bizflow.tempox.ui.components.HomeTab
import cloud.bizflow.tempox.ui.components.LegalScreen
import cloud.bizflow.tempox.ui.components.SectionLabel
import cloud.bizflow.tempox.ui.components.StatCard
import cloud.bizflow.tempox.ui.components.TempoxStudioBackground
import cloud.bizflow.tempox.ui.components.TempoxThemeColors
import cloud.bizflow.tempox.ui.components.TrophyCard
import cloud.bizflow.tempox.ui.theme.TemproxColors
import cloud.bizflow.tempox.ui.theme.TemproxType
import kotlinx.coroutines.delay

// ── Floating Light Studio palette (centralized in TempoxThemeColors) ───
private val CardBg = TempoxThemeColors.CardSurface
private val CardBorder = TempoxThemeColors.CardBorder
private val InkText = TempoxThemeColors.TextPrimary
private val MutedText = TempoxThemeColors.TextSecondary
private val NeonYellow = Color(0xFFFACC15)

/** Home hub: play / stats / trophies + rules modal + optional match seed. */
@OptIn(ExperimentalMaterial3Api::class)
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
    val billingPrice by billing.formattedPrice.collectAsState()
    val billingLoading by billing.isLoading.collectAsState()
    var tab by remember { mutableStateOf(HomeTab.PLAY) }
    var showRules by remember {
        val prefs = context.getSharedPreferences("temprox_settings", android.content.Context.MODE_PRIVATE)
        mutableStateOf(!prefs.getBoolean("rules_seen", false))
    }
    var seedText by remember { mutableStateOf("") }
    var unlockFor by remember { mutableStateOf<GameMode?>(null) }
    var showPaywallSheet by remember { mutableStateOf(false) }
    // Native legal reader — no browser, 100% offline.
    var legalDoc by remember { mutableStateOf<LegalType?>(null) }

    val level = Progression.levelForXp(stats.totalXp)
    val currentLevelFloor = Progression.xpForLevel(level)
    val nextLevelCost = Progression.xpForLevel(level + 1)
    val levelProgress =
        if (nextLevelCost <= currentLevelFloor) 1f
        else (stats.totalXp - currentLevelFloor).toFloat() / (nextLevelCost - currentLevelFloor)

    Box(Modifier.fillMaxSize()) {
        // ── Floating Light Studio backdrop (radial vignette, zero assets) ──
        TempoxStudioBackground(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(14.dp))

                // ── Header: TEMPOX branding + VIP crown icon ──────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Hamburger / Rules
                    Box(
                        Modifier
                            .size(44.dp)
                            .shadow(TempoxThemeColors.ElevationInfo, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardBg)
                            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                            .clickable { showRules = true },
                        contentAlignment = Alignment.Center,
                    ) { Text("☰", fontSize = 19.sp, color = InkText) }

                    Spacer(Modifier.width(10.dp))

                    // Brand wordmark + tagline
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.home_title),
                            style = TemproxType.title.copy(color = InkText),
                            maxLines = 1,
                        )
                        Text(
                            stringResource(R.string.tagline),
                            style = TemproxType.micro.copy(color = MutedText),
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    // Level chip
                    TopChip(
                        glyph = "👑",
                        value = stringResource(R.string.home_chip_level, level),
                        onClick = { tab = HomeTab.TROPHIES },
                    )
                    Spacer(Modifier.width(8.dp))

                    // High-score chip
                    TopChip(
                        glyph = "⭐",
                        value = "${stats.highScore}",
                        onClick = { tab = HomeTab.STATS },
                    )
                    Spacer(Modifier.width(8.dp))

                    // VIP Crown — gold beacon that opens the paywall
                    IconButton(
                        onClick = { showPaywallSheet = true },
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(TempoxThemeColors.ElevationInfo, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardBg)
                            .border(1.5.dp, NeonYellow.copy(alpha = 0.9f), RoundedCornerShape(14.dp)),
                    ) {
                        Text("👑", fontSize = 20.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Scrollable body ───────────────────────────────────────────────
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
                            onOpenPrivacy = { legalDoc = LegalType.PRIVACY },
                        )
                        HomeTab.STATS -> StatsTab(stats)
                        HomeTab.TROPHIES -> TrophiesTab(stats)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        // ── Bottom nav pinned ────────────────────────────────────────────────────
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

        // ── Rules sheet ──────────────────────────────────────────────────────────
        if (showRules) RulesSheet(
            language = language,
            onLanguageChange = onLanguageChange,
            onClose = {
                context.getSharedPreferences("temprox_settings", android.content.Context.MODE_PRIVATE)
                    .edit().putBoolean("rules_seen", true).apply()
                showRules = false
            },
            onOpenLegal = { legalDoc = it },
        )

        // ── Native legal reader (privacy / terms) ────────────────────────────────
        legalDoc?.let { doc ->
            val isPrivacy = doc == LegalType.PRIVACY
            LegalScreen(
                title = stringResource(
                    if (isPrivacy) R.string.legal_privacy_btn else R.string.legal_terms_btn,
                ),
                updated = stringResource(
                    if (isPrivacy) R.string.legal_privacy_updated else R.string.legal_terms_updated,
                ),
                body = stringResource(
                    if (isPrivacy) R.string.legal_privacy_body else R.string.legal_terms_body,
                ),
                onDismiss = { legalDoc = null },
            )
        }

        // ── Unlock-mode dialog ───────────────────────────────────────────────────
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

        // ── Premium Paywall Bottom Sheet ─────────────────────────────────────────
        if (showPaywallSheet) {
            PremiumPaywallBottomSheet(
                isAdFree = adFree,
                billing = billing,
                formattedPrice = billingPrice,
                isLoading = billingLoading,
                onDismiss = { showPaywallSheet = false },
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Paywall Bottom Sheet
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumPaywallBottomSheet(
    isAdFree: Boolean,
    billing: BillingRepository,
    formattedPrice: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var purchaseError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TempoxThemeColors.CardSurface,
        contentColor = InkText,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Crown header
            Text("👑", fontSize = 40.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.paywall_title),
                style = TemproxType.title.copy(
                    color = InkText,
                    textAlign = TextAlign.Center,
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.paywall_subtitle),
                style = TemproxType.caption.copy(color = MutedText),
            )

            Spacer(Modifier.height(24.dp))

            // Benefits column
            listOf(
                Triple("🚫", stringResource(R.string.paywallBenefit1), Color(0xFF10B981)),
                Triple("⚡", stringResource(R.string.paywallBenefit2), Color(0xFFB45309)),
                Triple("🎮", stringResource(R.string.paywallBenefit3), Color(0xFF7C3AED)),
            ).forEach { (icon, text, tint) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardBg)
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(icon, fontSize = 22.sp)
                    Spacer(Modifier.width(14.dp))
                    Text(text, style = TemproxType.bodyBold.copy(color = tint))
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(24.dp))

            if (isAdFree) {
                // Already premium
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFF9800))))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.premium_vip_badge),
                        style = TemproxType.bodyBold.copy(color = Color(0xFF3E2723)),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                // CTA button
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(TemproxColors.Primary, Color(0xFF8B5CF6)),
                            ),
                        )
                        .clickable(enabled = !isLoading) {
                            SoundManager.play(SoundManager.Sfx.CLICK)
                            SoundManager.vibrate(longArrayOf(0, 18))
                            billing.purchaseRemoveAds(
                                onSuccess = {
                                    SoundManager.play(SoundManager.Sfx.TROPHY)
                                    SoundManager.vibrate(longArrayOf(0, 30, 40, 30))
                                    onDismiss()
                                },
                                onError = { purchaseError = true },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                        )
                    } else {
                        val ctaText = if (formattedPrice.isNotEmpty()) {
                            stringResource(R.string.paywall_cta_dynamic, formattedPrice)
                        } else {
                            stringResource(R.string.paywall_cta)
                        }
                        Text(
                            ctaText,
                            style = TemproxType.bodyBold.copy(
                                color = Color.White,
                                letterSpacing = 0.8.sp,
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            if (purchaseError) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.purchase_error),
                    style = TemproxType.micro.copy(color = TemproxColors.Danger),
                )
            }

            Spacer(Modifier.height(12.dp))

            // Restore purchases
            Text(
                stringResource(R.string.paywall_restore),
                style = TemproxType.caption.copy(color = MutedText),
                modifier = Modifier
                    .clickable {
                        SoundManager.play(SoundManager.Sfx.CLICK)
                        billing.restorePurchases()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )

            Spacer(Modifier.height(4.dp))

            // Close
            Text(
                stringResource(R.string.paywall_close),
                style = TemproxType.caption.copy(color = MutedText),
                modifier = Modifier
                    .clickable {
                        SoundManager.play(SoundManager.Sfx.CLICK)
                        onDismiss()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Play Tab
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlayTab(
    stats: PlayerStats,
    level: Int,
    levelProgress: Float,
    economy: EconomyState,
    seedText: String,
    onSeedChange: (String) -> Unit,
    onPlay: (GameMode, String) -> Unit,
    onOpenPrivacy: () -> Unit = {},
) {
    Column {
        // ── Mode cards ────────────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TaskCard(
                header = stringResource(R.string.card_pattern_header),
                caption = stringResource(R.string.card_pattern_caption),
                flow = "🧠  ▸  △ ▢ ◯  ▸  ⊘  ▸  ● ■ ▲ ✓",
                tint = Color(0xFF10B981),
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

        // ── Profile Level card (floating white surface) ───────────────────────
        FloatingCard(
            accent = TemproxColors.Pink,
            modifier = Modifier.shadow(TempoxThemeColors.ElevationInfo, RoundedCornerShape(24.dp)),
        ) {
            Text(stringResource(R.string.home_best_score), style = TemproxType.caption.copy(color = Color(0xFFB45309)))
            Text("${stats.highScore}", style = TemproxType.score.copy(color = InkText))
            Spacer(Modifier.height(10.dp))

            Text(stringResource(R.string.home_level_profile), style = TemproxType.micro.copy(color = MutedText))
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
                    style = TemproxType.micro.copy(color = MutedText),
                )
                val floor = Progression.xpForLevel(level)
                val cost = Progression.xpForLevel(level + 1)
                val pct = if (cost <= floor) 100 else ((stats.totalXp - floor) * 100 / (cost - floor)).toInt().coerceIn(0, 100)
                Text(
                    stringResource(R.string.home_percent_to_level, pct, level + 1),
                    style = TemproxType.micro.copy(color = MutedText),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Optional seed ─────────────────────────────────────────────────────
        SectionLabel(stringResource(R.string.home_seed_label), TemproxColors.Accent)
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = seedText,
            onValueChange = onSeedChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = InkText),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardBg)
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                ) {
                    if (seedText.isEmpty()) Text(
                        stringResource(R.string.home_seed_placeholder),
                        style = TemproxType.body.copy(color = MutedText),
                    )
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        ArcadePlayButton(text = stringResource(R.string.home_play_now)) {
            onPlay(GameMode.ARCADE, seedText)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(
                R.string.home_footer_version,
                BuildConfig.VERSION_NAME,
                stringResource(R.string.legal_privacy_btn),
            ),
            style = TemproxType.micro.copy(color = TemproxColors.Primary),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable {
                    SoundManager.play(SoundManager.Sfx.CLICK)
                    onOpenPrivacy()
                },
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Arcade-play CTA (unchanged logic, kept as-is)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ArcadePlayButton(text: String, onClick: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "playPulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "playPulseScale",
    )
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            // 12dp hero shadow — the "floating above the studio floor" accent.
            .shadow(
                elevation = TempoxThemeColors.ElevationCta,
                shape = shape,
                ambientColor = Color(0xFF7C3AED),
                spotColor = Color(0xFF6D28D9),
            )
            .graphicsLayer { scaleX = pulse; scaleY = pulse }
            .drawBehind {
                drawRoundRect(
                    color = Color(0xFF4C1D95),
                    topLeft = Offset(0f, 4.dp.toPx()),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                )
            }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))))
            .clickable {
                SoundManager.play(SoundManager.Sfx.CLICK)
                SoundManager.vibrate(longArrayOf(0, 18))
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.uppercase(),
            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp),
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Unlock gate dialog (unchanged)
// ──────────────────────────────────────────────────────────────────────────────

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
    val activity = LocalContext.current as? Activity
    val modeName = stringResource(
        if (mode == GameMode.MATH) R.string.card_calc_header else R.string.card_pattern_header,
    )
    LaunchedEffect(adLoading) {
        if (adLoading && activity != null) {
            AdMobManager.showRewarded(
                activity = activity,
                onRewardEarned = {
                    adLoading = false
                    onWatchAd()
                },
                onAdDismissed = {
                    adLoading = false
                    adFailed = true
                },
            )
        } else if (adLoading) {
            adLoading = false
            adFailed = true
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

// ──────────────────────────────────────────────────────────────────────────────
// Stats / Trophies tabs (unchanged)
// ──────────────────────────────────────────────────────────────────────────────

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
        cloud.bizflow.tempox.game.Achievements.ALL.forEach { id ->
            TrophyCard(
                title = stringResource(cloud.bizflow.tempox.game.Achievements.titleRes(id)),
                description = stringResource(cloud.bizflow.tempox.game.Achievements.descRes(id)),
                unlocked = id in stats.achievements,
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Studio-light top chips (crown / star) for the header
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopChip(glyph: String, value: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .shadow(TempoxThemeColors.ElevationInfo, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .clickable { SoundManager.play(SoundManager.Sfx.CLICK); onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(glyph, fontSize = 15.sp)
        Text(value, style = TemproxType.micro.copy(color = MutedText), maxLines = 1)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Studio-light task card (selectable → 8dp elevation)
// ──────────────────────────────────────────────────────────────────────────────

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
    Row(
        modifier = modifier
            .height(112.dp)
            // Shadow hierarchy: selectable mode cards float at 8dp (locked = informative 4dp).
            .shadow(
                if (locked) TempoxThemeColors.ElevationInfo else TempoxThemeColors.ElevationCard,
                RoundedCornerShape(20.dp),
            )
            .alpha(if (locked) 0.75f else 1f)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(
                1.dp,
                if (locked) CardBorder else tint.copy(alpha = 0.45f),
                RoundedCornerShape(20.dp),
            )
            .clickable {
                SoundManager.play(SoundManager.Sfx.CLICK)
                SoundManager.vibrate(longArrayOf(0, 18))
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(58.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(tint.copy(alpha = 0.12f))
                    .border(1.dp, CardBorder, RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(glyph, fontSize = 24.sp) }
        }
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(
                header,
                style = TemproxType.micro.copy(color = tint, letterSpacing = 1.2.sp, fontSize = 11.sp),
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.height(3.dp))
            Text(caption, style = TemproxType.bodyBold.copy(color = InkText), maxLines = 1)
            Spacer(Modifier.height(5.dp))
            Text(flow, style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = MutedText), maxLines = 1)
        }
        Box(
            Modifier
                .padding(end = 10.dp)
                .size(34.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (locked) CardBorder else tint),
            contentAlignment = Alignment.Center,
        ) { Text(if (locked) "🔒" else "▶", fontSize = 13.sp, color = if (locked) MutedText else Color.White) }
    }
}
