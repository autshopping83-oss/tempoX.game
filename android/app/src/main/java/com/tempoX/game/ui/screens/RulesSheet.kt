package com.tempoX.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tempoX.game.R
import com.tempoX.game.audio.SoundManager
import com.tempoX.game.game.LangMode
import com.tempoX.game.ui.components.PrimaryButton
import com.tempoX.game.ui.theme.TemproxColors
import com.tempoX.game.ui.theme.TemproxType

private data class RuleItem(val emoji: String, val titleRes: Int, val descRes: Int, val tint: Color)

/** Full-screen "HOW TO PLAY" modal (same content as the original rules sheet). */
@Composable
fun RulesSheet(
    language: LangMode,
    onLanguageChange: (LangMode) -> Unit,
    onClose: () -> Unit,
) {
    var soundOn by remember { mutableStateOf(SoundManager.isEnabled()) }
    var hapticsOn by remember { mutableStateOf(SoundManager.isHapticsEnabled()) }
    val items = listOf(
        RuleItem("🧠", R.string.rules_memory_title, R.string.rules_memory_desc, TemproxColors.Pink),
        RuleItem("⚡", R.string.rules_reflex_title, R.string.rules_reflex_desc, TemproxColors.Warning),
        RuleItem("➗", R.string.rules_math_title, R.string.rules_math_desc, TemproxColors.Info),
        RuleItem("👀", R.string.rules_attention_title, R.string.rules_attention_desc, TemproxColors.Green),
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        // Outer column constrains the card to the available height.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 28.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF231D45), Color(0xFF171230))))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
            ) {
                Text(stringResource(R.string.rules_title), style = TemproxType.titleLg.copy(color = Color.White))
                Text(stringResource(R.string.rules_subtitle), style = TemproxType.micro.copy(color = TemproxColors.Accent))
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.rules_intro),
                    style = TemproxType.body.copy(color = Color(0xFFC9C4DE)),
                )
                Spacer(Modifier.height(16.dp))

                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, item.tint.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .width(44.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(item.tint.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) { Text(item.emoji, fontSize = TemproxType.title.fontSize) }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                stringResource(item.titleRes),
                                style = TemproxType.bodyBold.copy(color = item.tint),
                            )
                            Text(
                                stringResource(item.descRes),
                                style = TemproxType.caption.copy(color = Color(0xFFB7B2CE)),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.rules_combo_tip),
                    style = TemproxType.body.copy(color = TemproxColors.Warning),
                )

                // ---- Quick settings ------------------------------------
                Spacer(Modifier.height(16.dp))
                SheetRow(stringResource(R.string.settings_sound), "🔊") {
                    Switch(
                        checked = soundOn,
                        onCheckedChange = {
                            soundOn = it
                            SoundManager.setEnabled(it)
                            if (it) SoundManager.play(SoundManager.Sfx.CLICK)
                        },
                        colors = sheetSwitch(),
                    )
                }
                SheetRow(stringResource(R.string.settings_vibration), "📳") {
                    Switch(
                        checked = hapticsOn,
                        onCheckedChange = { hapticsOn = it; SoundManager.setHapticsEnabled(it) },
                        colors = sheetSwitch(),
                    )
                }
                SheetRow(stringResource(R.string.settings_language), "🌐") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            LangMode.SYSTEM to stringResource(R.string.lang_auto),
                            LangMode.PT to "PT",
                            LangMode.EN to "EN",
                        ).forEach { (mode, label) ->
                            val selected = mode == language
                            Text(
                                label,
                                style = TemproxType.micro.copy(
                                    color = if (selected) Color.White else Color(0xFF9A94B5),
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (selected) TemproxColors.Primary else Color.White.copy(alpha = 0.08f)
                                    )
                                    .clickable { onLanguageChange(mode) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                PrimaryButton(text = stringResource(R.string.rules_cta), onClick = onClose)
            }
        }
    }
}


@Composable
private fun sheetSwitch() = SwitchDefaults.colors(
    checkedThumbColor = TemproxColors.Accent,
    checkedTrackColor = TemproxColors.Accent.copy(alpha = 0.35f),
    uncheckedThumbColor = Color(0xFF6E6890),
    uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
)

@Composable
private fun SheetRow(label: String, emoji: String, control: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 15.sp)
        Spacer(Modifier.width(8.dp))
        Text(label, style = TemproxType.bodyBold.copy(color = Color.White), modifier = Modifier.weight(1f))
        control()
    }
}
