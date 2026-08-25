package cloud.bizflow.tempox.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Native Cyber-Arcade night backdrop for TEMPOX.
 *
 * Layers, painted back-to-front in a single [drawBehind] pass (GPU-only,
 * zero extra layout nodes, zero bitmaps):
 *  1. Vertical night gradient #080511 -> #140B27.
 *  2. Focal radial glow (#8B5CF6 @ 12%) anchored at the upper third so the
 *     logo/header pops — center/radius are derived from the live DrawScope
 *     size, keeping ultra-tall 20:9 screens from flattening the glow.
 *  3. Neon grid #6D28D9 at low opacity (strictly secondary to UI content).
 *
 * The grid pixel pitch is converted once per composition ([remember]) and the
 * optional [pulse] gently breathes the grid alpha for combo/time-pressure
 * moments — implemented as a single float animation driving a redraw, never
 * recomposing children.
 */
@Composable
fun TempoxCyberBackground(
    modifier: Modifier = Modifier,
    gridSize: Dp = 28.dp,
    pulse: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    // One density conversion per composition — never per frame.
    val gridPx = with(LocalDensity.current) { gridSize.toPx() }

    val transition = rememberInfiniteTransition(label = "cyberGridPulse")
    val breathed by transition.animateFloat(
        initialValue = GridAlphaBase,
        targetValue = GridAlphaPeak,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cyberGridAlpha",
    )
    // Idle grids hold the calm baseline; pulsing rides the breathing curve.
    val gridAlpha = if (pulse) breathed else GridAlphaBase

    val baseBrush = remember {
        Brush.verticalGradient(listOf(NightTop, NightBottom))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(baseBrush)
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(GlowColor.copy(alpha = GlowAlpha), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.28f),
                        radius = size.maxDimension * 0.75f,
                    ),
                )
                val stroke = 1.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(GridColor.copy(alpha = gridAlpha), Offset(x, 0f), Offset(x, size.height), stroke)
                    x += gridPx
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(GridColor.copy(alpha = gridAlpha), Offset(0f, y), Offset(size.width, y), stroke)
                    y += gridPx
                }
            },
        content = content,
    )
}

private val NightTop = Color(0xFF080511)
private val NightBottom = Color(0xFF140B27)
private val GlowColor = Color(0xFF8B5CF6)
private val GridColor = Color(0xFF6D28D9)
private const val GlowAlpha = 0.12f
private const val GridAlphaBase = 0.15f
private const val GridAlphaPeak = 0.30f

/** Contrast harness: a simulated TEMPOX card over the cyber backdrop. */
@Preview(name = "Cyber contrast", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
private fun CyberBackgroundPreview() {
    TempoxCyberBackground(pulse = false) {
        Box(Modifier.fillMaxSize())
    }
}
