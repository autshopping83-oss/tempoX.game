package com.tempoX.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.tempoX.game.audio.SoundManager
import com.tempoX.game.ui.theme.TemproxColors
import com.tempoX.game.ui.theme.TemproxShapes
import com.tempoX.game.ui.theme.TemproxType

/** Frosted dark card used across the arcade UI. */
@Composable
fun FloatingCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(TemproxShapes.Card)
            .background(Brush.verticalGradient(listOf(Color(0xFF2A2352), Color(0xFF1E183E))))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(accent?.copy(alpha = 0.55f) ?: Color(0x40FFFFFF), Color(0x14FFFFFF))),
                shape = TemproxShapes.Card,
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        content = content,
    )
}

/** Solid brand CTA — plays the click SFX and optional haptic on press. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(TemproxShapes.Button)
            .background(
                if (enabled) Brush.horizontalGradient(listOf(TemproxColors.Primary, Color(0xFF8B5CF6)))
                else Brush.horizontalGradient(listOf(Color(0xFF494158), Color(0xFF575066)))
            )
            .clickable(enabled = enabled) { SoundManager.play(SoundManager.Sfx.CLICK); onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TemproxType.title.copy(color = Color.White), textAlign = TextAlign.Center)
    }
}

/** Outlined secondary action. `light` adapts the chip to bright surfaces. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    light: Boolean = false,
) {
    val container = if (light) Color.White else Color.White.copy(alpha = 0.06f)
    val line = if (light) Color(0xFFCBD5E1) else Color.White.copy(alpha = 0.25f)
    val content = if (light) TemproxColors.Ink else Color.White
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(TemproxShapes.Button)
            .background(container)
            .border(1.dp, line, TemproxShapes.Button)
            .clickable { SoundManager.play(SoundManager.Sfx.CLICK); onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TemproxType.bodyBold.copy(color = content), textAlign = TextAlign.Center)
    }
}

/** Small stat tile (label + value). */
@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, tint: Color = TemproxColors.Accent) {
    FloatingCard(modifier = modifier, accent = tint) {
        Text(value, style = TemproxType.titleLg.copy(color = tint))
        Spacer(Modifier.height(2.dp))
        Text(label, style = TemproxType.micro.copy(color = Color(0xFF94A3B8)), maxLines = 1)
    }
}

/** Trophy row with unlocked / locked styling. */
@Composable
fun TrophyCard(title: String, description: String, unlocked: Boolean) {
    val tint = if (unlocked) TemproxColors.Accent else Color(0xFF4A4560)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (unlocked) Color(0xFF2A2352) else Color(0xFF181430))
            .border(1.dp, tint.copy(alpha = if (unlocked) 0.5f else 0.18f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🏆", fontSize = TemproxType.title.fontSize, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = TemproxType.bodyBold.copy(color = if (unlocked) Color.White else Color(0xFF8B86A3)))
            Text(description, style = TemproxType.caption.copy(color = Color(0xFF6E6890), fontWeight = FontWeight.Medium))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            if (unlocked) "✓ LIBERADO" else "BLOQUEADO",
            style = TemproxType.micro.copy(color = tint),
        )
    }
}

/** Thin branded progress bar. */
@Composable
fun BrandedProgress(fraction: Float, tint: Color = TemproxColors.Accent, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        color = tint,
        trackColor = Color.White.copy(alpha = 0.10f),
        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        modifier = modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(999.dp)),
    )
}

/** TEMPOX wordmark: "TEMPO" white + accent X block. */
@Composable
fun TemproxLogo(heightText: Int = 30) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "TEMPO",
            style = TemproxType.display.copy(fontSize = heightText.sp, color = TemproxColors.Ink),
        )
        Box(
            modifier = Modifier
                .padding(start = 2.dp)
                .size((heightText * 0.92f).dp)
                .clip(RoundedCornerShape((heightText * 0.22f).dp))
                .background(TemproxColors.Primary),
            contentAlignment = Alignment.Center,
        ) {
            Text("X", style = TemproxType.display.copy(fontSize = (heightText * 0.72f).sp, color = Color.White))
        }
    }
}

/** Section header with small colored dot. */
@Composable
fun SectionLabel(text: String, tint: Color = TemproxColors.Accent) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(99)).background(tint))
        Text(text.uppercase(), style = TemproxType.caption.copy(color = Color(0xFFB7B2CE), letterSpacing = 1.2.sp))
    }
}
