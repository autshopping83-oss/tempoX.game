package com.tempoX.game.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Premium light animated background.
 *
 *  - Soft vertical gradient #EEF2FF -> #F8FAFC
 *  - Hairline grid (1dp, 5% ink)
 *  - 17 floating geometric shapes at 8-15% opacity in brand colors,
 *    drifting 6-20dp with <=5 deg rotation, 8s/16s ease-in-out loops.
 *
 * Everything renders inside a single draw phase driven by one shared
 * animation clock: no recomposition, no allocations per element per
 * frame beyond two tiny path transforms -> smooth 60 FPS on old devices.
 */
private enum class ShapeKind { TRIANGLE_FILLED, TRIANGLE_OUTLINE, ROUNDED_SQUARE, CIRCLE, DIAMOND, STAR4 }

private data class BgShape(
    val kind: ShapeKind,
    val color: Color,
    val sizeDp: Float,     // bounding radius-ish in dp
    val cx: Float,         // 0..1 fraction of width
    val cy: Float,         // 0..1 fraction of height
    val ampX: Float,       // drift amplitude dp (<=20)
    val ampY: Float,
    val periodSec: Float,  // 8 or 16 (divides the 16s master loop -> seamless)
    val phase: Float,      // 0..1 loop offset
    val rotDeg: Float,     // max rotation amplitude (<=5)
    val alpha: Float,      // 0.08..0.15
)

private fun buildShapes(): List<BgShape> {
    val palette = listOf(
        Color(0xFF6D3DF5), // roxo
        Color(0xFF3B82F6), // azul
        Color(0xFFFACC15), // amarelo
        Color(0xFFEC4899), // rosa
        Color(0xFF22C55E), // verde
    )
    val kinds = ShapeKind.entries
    val rng = Random(seed = 20260823L)

    return List(17) { i ->
        val kind = kinds[i % kinds.size]
        // Bias shapes toward edges/corners so they never fight cards or copy.
        val edgeX = if (rng.nextBoolean()) rng.nextFloat() * 0.16f else 0.84f + rng.nextFloat() * 0.16f
        val centerX = rng.nextBoolean()
        val cx = if (centerX && i % 3 == 0) 0.30f + rng.nextFloat() * 0.40f else edgeX
        val cy = 0.06f + rng.nextFloat() * 0.88f
        BgShape(
            kind = kind,
            color = palette[rng.nextInt(palette.size)],
            sizeDp = 10f + rng.nextFloat() * 12f,          // 10-22dp
            cx = cx.coerceIn(0.04f, 0.96f),
            cy = cy,
            ampX = 6f + rng.nextFloat() * 14f,             // 6-20dp
            ampY = 6f + rng.nextFloat() * 14f,
            periodSec = if (i % 2 == 0) 8f else 16f,       // both divide the 16s master loop
            phase = rng.nextFloat(),
            rotDeg = rng.nextFloat() * 5f,                 // <= 5 degrees
            alpha = 0.08f + rng.nextFloat() * 0.07f,       // 8-15%
        )
    }
}

/** Pre-built unit-space outlines centred on the origin (radius 1). */
private class ShapePaths {
    val triangle = Path().apply {
        moveTo(0f, -1f); lineTo(0.95f, 0.75f); lineTo(-0.95f, 0.75f); close()
    }
    val diamond = Path().apply {
        moveTo(0f, -1f); lineTo(1f, 0f); lineTo(0f, 1f); lineTo(-1f, 0f); close()
    }
    val star = Path().apply {
        // 4-pointed star: outer tips at r=1, inner waist at r=0.38
        val inner = 0.38f
        moveTo(0f, -1f)
        quadraticBezierTo(inner * 0.25f, -inner * 0.25f, 1f, 0f)
        quadraticBezierTo(inner * 0.25f, inner * 0.25f, 0f, 1f)
        quadraticBezierTo(-inner * 0.25f, inner * 0.25f, -1f, 0f)
        quadraticBezierTo(-inner * 0.25f, -inner * 0.25f, 0f, -1f)
        close()
    }
}

@Composable
fun AnimatedBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "temproxBg")
    // Master clock: one full loop every 16s; every shape period divides it.
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 16_000, easing = LinearEasing)),
        label = "masterT",
    )
    val shapes = remember { buildShapes() }
    val paths = remember { ShapePaths() }
    val gridColor = Color(0xFF1E1B2E).copy(alpha = 0.05f) // 5% hairline

    modifier.drawBehind {
        // --- soft light gradient -----------------------------------------
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFEEF2FF), Color(0xFFF8FAFC)),
                startY = 0f,
                endY = size.height,
            ),
        )

        // --- hairline grid -------------------------------------------------
        val step = 28.dp.toPx()
        var x = step
        while (x < size.width) {
            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
            x += step
        }
        var y = step
        while (y < size.height) {
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            y += step
        }

        // --- floating shapes ------------------------------------------------
        val tau = (2.0 * PI).toFloat()
        shapes.forEach { s ->
            val ph = (t * 16f / s.periodSec + s.phase) % 1f
            val wave = sin(tau * ph)                       // ease-in-out by nature
            val wave2 = 0.55f * sin(tau * ph * 2f + 1.3f)  // subtle figure-8 flavour
            val cx = s.cx * size.width + (s.ampX * wave).dp.toPx()
            val cy = s.cy * size.height + (s.ampY * wave2).dp.toPx()
            val rot = s.rotDeg * sin(tau * ph + 0.7f)
            val r = s.sizeDp.dp.toPx()

            translate(left = cx, top = cy) {
                rotate(degrees = rot) {
                    scale(scale = r, pivot = Offset.Zero) {
                        when (s.kind) {
                            ShapeKind.CIRCLE ->
                                drawCircle(s.color.copy(alpha = s.alpha), radius = 1f)
                            ShapeKind.ROUNDED_SQUARE -> {
                                val half = 0.85f
                                drawRoundRect(
                                    color = s.color.copy(alpha = s.alpha),
                                    topLeft = Offset(-half, -half),
                                    size = androidx.compose.ui.geometry.Size(half * 2f, half * 2f),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(0.35f, 0.35f),
                                )
                            }
                            ShapeKind.DIAMOND ->
                                drawPath(paths.diamond, s.color.copy(alpha = s.alpha))
                            ShapeKind.STAR4 ->
                                drawPath(paths.star, s.color.copy(alpha = s.alpha))
                            ShapeKind.TRIANGLE_FILLED ->
                                drawPath(paths.triangle, s.color.copy(alpha = s.alpha))
                            ShapeKind.TRIANGLE_OUTLINE ->
                                drawPath(
                                    paths.triangle,
                                    s.color.copy(alpha = s.alpha * 1.35f),
                                    style = androidx.compose.ui.graphics.Stroke(width = 0.22f),
                                )
                        }
                    }
                }
            }
        }
    }
}
