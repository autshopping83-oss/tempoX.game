package com.tempoX.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tempoX.game.ui.theme.TemproxColors
import com.tempoX.game.ui.theme.TemproxType

/**
 * Circular countdown ring — mirrors the original CircularTimer:
 * green > 50%, amber 20–50%, pulsing red under 20%.
 */
@Composable
fun CircularTimer(
    remainingFraction: Float,
    secondsLeft: Int,
    diameter: Dp = 92.dp,
    strokeWidth: Dp = 9.dp,
) {
    val frac = remainingFraction.coerceIn(0f, 1f)
    val ringColor = when {
        frac > 0.5f -> TemproxColors.Success
        frac > 0.2f -> TemproxColors.Warning
        else -> TemproxColors.Danger
    }
    Box(modifier = Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = Color(0xFF1E1B2E).copy(alpha = 0.08f),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = ringColor,
                startAngle = -90f, sweepAngle = 360f * frac, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text("$secondsLeft", style = TemproxType.titleLg.copy(color = TemproxColors.Ink))
    }
}
