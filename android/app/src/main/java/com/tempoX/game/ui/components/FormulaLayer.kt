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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class FormulaItem(
    val text: String,
    val xf: Float,
    val yf: Float,
    val sp: Float,
    val color: Color,
    val ampX: Float,
    val ampY: Float,
    val periodDiv: Int,   // divides 16 -> seamless loop
    val phase: Float,
)

/**
 * Faded chalkboard-math layer (integrals, proofs, E=mc^2 ...) drifting
 * slowly over the light gradient. Text layouts are measured ONCE and
 * cached; drawing happens in the draw phase only -> no recomposition,
 * negligible GPU cost (alpha <= 15%, tiny translations).
 */
@Composable
fun FormulaLayer(modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()

    val palette = listOf(
        Color(0xFF6D3DF5),
        Color(0xFF3B82F6),
        Color(0xFF334155),
    )
    val items = remember {
        val rng = Random(seed = 777L)
        listOf(
            "E = mc²", "∫₀^∞ e^(−x²) dx = √π⁄2",
            "∑ 1/n² = π²/6", "a² + b² = c²",
            "∇·E = ρ/ε₀", "∂²u/∂t² = c²∇²u",
            "π ≈ 3.14159…", "lim x→0 sin(x)/x = 1",
            "e^(iπ) + 1 = 0", "∮ F·dr = −dΦ/dt",
        ).mapIndexed { i, txt ->
            FormulaItem(
                text = txt,
                xf = if (rng.nextBoolean()) 0.04f + rng.nextFloat() * 0.22f else 0.62f + rng.nextFloat() * 0.30f,
                yf = 0.05f + rng.nextFloat() * 0.88f,
                sp = 13f + rng.nextFloat() * 9f,
                color = palette[rng.nextInt(palette.size)],
                ampX = 4f + rng.nextFloat() * 6f,
                ampY = 4f + rng.nextFloat() * 6f,
                periodDiv = if (i % 2 == 0) 1 else 2,
                phase = rng.nextFloat(),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "formulas")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16_000, easing = LinearEasing)),
        label = "formulaT",
    )

    val tau = (2.0 * PI).toFloat()

    // Measure once per composition; layouts reused every frame.
    val layouts: List<Pair<FormulaItem, TextLayoutResult>> = remember(items) {
        items.map { item ->
            item to measurer.measure(
                AnnotatedString(item.text),
                TextStyle(fontSize = item.sp.sp, fontWeight = FontWeight.SemiBold),
            )
        }
    }

    modifier.drawBehind {
        layouts.forEach { (item, layout) ->
            val ph = (t * item.periodDiv + item.phase) % 1f
            val dx = (item.ampX * sin(tau * ph)).dp.toPx()
            val dy = (item.ampY * sin(tau * ph + 1.1f)).dp.toPx()
            val x = item.xf * size.width + dx
            val y = item.yf * size.height + dy
            val clampedX = x.coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
            drawText(
                textLayoutResult = layout,
                color = item.color.copy(alpha = 0.13f), // 10-15% translucency
                topLeft = Offset(clampedX, y.coerceIn(0f, size.height - layout.size.height)),
            )
        }
    }
}
