package com.tempoX.game.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.tempoX.game.R
import com.tempoX.game.audio.SoundManager
import com.tempoX.game.game.Achievements
import com.tempoX.game.game.MatchSummary
import com.tempoX.game.ui.components.FloatingCard
import com.tempoX.game.ui.components.PrimaryButton
import com.tempoX.game.ui.components.SecondaryButton
import com.tempoX.game.ui.components.TrophyCard
import com.tempoX.game.ui.theme.TemproxColors
import com.tempoX.game.ui.theme.TemproxShapes
import com.tempoX.game.ui.theme.TemproxType
import kotlinx.coroutines.delay

/** Post-match screen: score, XP pool with simulated "double reward" ad, new trophies, share. */
@Composable
fun ResultScreen(
    summary: MatchSummary,
    isRecord: Boolean,
    doubledAlready: Boolean,
    unlockedIds: List<String>,
    onPlayAgain: () -> Unit,
    onMenu: () -> Unit,
) {
    val context = LocalContext.current

    // Ad reward state machine: idle -> loading(1.6s) -> doubled
    var doubleState by remember { mutableStateOf(if (doubledAlready) 2 else 0) }
    var bonusXp by remember { mutableStateOf(if (doubledAlready) summary.xpGained else 0) }

    LaunchedEffect(Unit) {
        SoundManager.play(if (isRecord) SoundManager.Sfx.RECORD else SoundManager.Sfx.GAME_OVER)
        if (unlockedIds.isNotEmpty()) {
            SoundManager.vibrate(longArrayOf(0, 60, 80, 60))
            // Metallic trophy shine layered just after the main sting.
            delay(700)
            SoundManager.play(SoundManager.Sfx.TROPHY)
        }
    }

    val total = summary.totalCorrect + summary.totalIncorrect
    val accuracy = if (total == 0) 0 else summary.totalCorrect * 100 / total

    Box(
        Modifier
            .fillMaxSize(),
    ) {
        com.tempoX.game.ui.components.AnimatedBackground(Modifier.fillMaxSize())
        com.tempoX.game.ui.components.FormulaLayer(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // ---- Scrollable content zone ---------------------------------
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                Text("🏁", fontSize = TemproxType.display.fontSize)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.result_time_up), style = TemproxType.titleLg.copy(color = TemproxColors.Ink))

                if (isRecord) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(TemproxColors.Warning.copy(alpha = 0.12f))
                            .border(1.dp, TemproxColors.Warning.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(stringResource(R.string.result_new_record), style = TemproxType.caption.copy(color = Color(0xFFB45309)))
                    }
                }

                Spacer(Modifier.height(18.dp))
                FloatingCard(accent = TemproxColors.Accent) {
                    Text(stringResource(R.string.result_final_score), style = TemproxType.caption.copy(color = TemproxColors.Muted))
                    Text("${summary.score}", style = TemproxType.score.copy(color = TemproxColors.Ink))
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniStat(stringResource(R.string.stat_challenges), "${summary.totalCorrect}", Modifier.weight(1f))
                    MiniStat(stringResource(R.string.stat_accuracy), "$accuracy%", Modifier.weight(1f))
                    MiniStat(stringResource(R.string.stat_max_combo), "x${summary.maxCombo}", Modifier.weight(1f))
                }

                Spacer(Modifier.height(16.dp))

                // ---- Reward pool / double -----------------------------------
                FloatingCard(accent = if (doubleState == 2) TemproxColors.Success else null) {
                    Text(stringResource(R.string.result_reward_pool), style = TemproxType.micro.copy(color = TemproxColors.Muted))
                    Text(
                        "+${summary.xpGained + bonusXp} XP",
                        style = TemproxType.titleLg.copy(color = if (doubleState == 2) TemproxColors.Success else Color(0xFFB45309)),
                    )
                    when (doubleState) {
                        0 -> {
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(R.string.result_double_hint), style = TemproxType.caption.copy(color = Color(0xFF475569)))
                            Spacer(Modifier.height(10.dp))
                            // Amber CTA — mirrors the web rewarded-ad button.
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                                    .clip(TemproxShapes.Button)
                                    .background(Brush.horizontalGradient(listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))))
                                    .clickable { SoundManager.play(SoundManager.Sfx.CLICK); doubleState = 1 },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    stringResource(R.string.result_double_btn),
                                    style = TemproxType.title.copy(color = Color(0xFF111827)),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.result_ad_note), style = TemproxType.micro.copy(color = TemproxColors.Muted))
                        }
                        1 -> {
                            Spacer(Modifier.height(12.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(TemproxColors.BorderSofter),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📺", fontSize = TemproxType.titleLg.fontSize)
                                    Spacer(Modifier.height(6.dp))
                                    Text(stringResource(R.string.result_ad_loading), style = TemproxType.bodyBold.copy(color = TemproxColors.Ink))
                                    Text(stringResource(R.string.result_ad_video), style = TemproxType.micro.copy(color = TemproxColors.Muted))
                                }
                            }
                            LaunchedEffect(Unit) {
                                delay(1600)
                                bonusXp = summary.xpGained
                                doubleState = 2
                                SoundManager.play(SoundManager.Sfx.RECORD)
                            }
                        }
                        else -> {
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.result_doubled_ok), style = TemproxType.bodyBold.copy(color = TemproxColors.Success))
                        }
                    }
                }

                if (unlockedIds.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.result_new_trophies), style = TemproxType.bodyBold.copy(color = Color(0xFFB45309)))
                    Spacer(Modifier.height(10.dp))
                    unlockedIds.forEach { id ->
                        TrophyCard(
                            title = stringResource(Achievements.titleRes(id)),
                            description = stringResource(Achievements.descRes(id)),
                            unlocked = true,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // ---- Pinned footer CTAs — always visible, never scrolled away --
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(TemproxColors.Background.copy(alpha = 0.92f))
                    .padding(horizontal = 22.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
            ) {
                PrimaryButton(text = stringResource(R.string.result_play_again), onClick = onPlayAgain)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        text = stringResource(R.string.action_share),
                        onClick = {
                            val text = context.getString(R.string.share_text, summary.score, summary.maxCombo)
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    },
                                    context.getString(R.string.app_name),
                                ),
                            )
                            Toast.makeText(
                                context,
                                context.getString(R.string.share_alert, summary.score),
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = stringResource(R.string.result_main_menu),
                        onClick = onMenu,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = TemproxType.title.copy(color = TemproxColors.Ink))
            Text(label, style = TemproxType.micro.copy(color = TemproxColors.Muted), maxLines = 1)
        }
    }
}
