package cloud.bizflow.tempox.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.bizflow.tempox.R
import cloud.bizflow.tempox.audio.SoundManager
import cloud.bizflow.tempox.game.LangMode
import cloud.bizflow.tempox.ui.LegalType
import cloud.bizflow.tempox.ui.components.PrimaryButton
import cloud.bizflow.tempox.ui.openLegal
import cloud.bizflow.tempox.ui.theme.TemproxColors
import cloud.bizflow.tempox.ui.theme.TemproxType

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
    val context = LocalContext.current
    val items = listOf(
        RuleItem("🧠", R.string.rules_memory_title, R.string.rules_memory_desc, TemproxColors.Pink),
        RuleItem("⚡", R.string.rules_reflex_title, R.string.rules_reflex_desc, TemproxColors.Warning),
        RuleItem("➗", R.string.rules_math_title, R.string.rules_math_desc, TemproxColors.Info),
        RuleItem("👀", R.string.rules_attention_title, R.string.rules_attention_desc, TemproxColors.Green),
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(TemproxColors.Background.copy(alpha = 0.98f)),
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
                    .background(Color.White)
                    .border(1.dp, TemproxColors.BorderLight, RoundedCornerShape(24.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
            ) {
                Text(stringResource(R.string.rules_title), style = TemproxType.titleLg.copy(color = TemproxColors.Ink))
                Text(stringResource(R.string.rules_subtitle), style = TemproxType.micro.copy(color = TemproxColors.Primary))
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.rules_intro),
                    style = TemproxType.body.copy(color = TemproxColors.Muted),
                )
                Spacer(Modifier.height(16.dp))

                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(item.tint.copy(alpha = 0.06f))
                            .border(1.dp, item.tint.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .width(44.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(item.tint.copy(alpha = 0.14f)),
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
                                style = TemproxType.caption.copy(color = TemproxColors.Muted),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.rules_combo_tip),
                    style = TemproxType.body.copy(color = Color(0xFFB45309)),
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
                                    color = if (selected) Color.White else TemproxColors.Muted,
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (selected) TemproxColors.Primary else TemproxColors.BorderSofter)
                                    .clickable { onLanguageChange(mode) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                // ---- Legal pages -----------------------------------------
                Spacer(Modifier.height(14.dp))
                LegalLinkRow("🔒", stringResource(R.string.legal_privacy_btn)) {
                    openLegal(context, LegalType.PRIVACY)
                }
                LegalLinkRow("📜", stringResource(R.string.legal_terms_btn)) {
                    openLegal(context, LegalType.TERMS)
                }

                Spacer(Modifier.height(18.dp))
                PrimaryButton(text = stringResource(R.string.rules_cta), onClick = onClose)
            }
        }
    }
}


@Composable
private fun sheetSwitch() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = TemproxColors.Primary,
    uncheckedThumbColor = Color(0xFF94A3B8),
    uncheckedTrackColor = TemproxColors.BorderLight,
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
        Text(label, style = TemproxType.bodyBold.copy(color = TemproxColors.Ink), modifier = Modifier.weight(1f))
        control()
    }
}

@Composable
private fun LegalLinkRow(emoji: String, label: String, onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TemproxColors.Background)
            .clickable {
                SoundManager.play(SoundManager.Sfx.CLICK)
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 15.sp)
        Spacer(Modifier.width(8.dp))
        Text(label, style = TemproxType.bodyBold.copy(color = TemproxColors.Primary))
        Spacer(Modifier.weight(1f))
        Text("▸", color = TemproxColors.Muted)
    }
}
