package cloud.bizflow.tempox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Centralized color tokens for the "Floating Light Studio" theme.
 * Keep every surface/text decision here so future re-themes are a
 * single-file edit instead of a screen-by-screen sweep.
 */
object TempoxThemeColors {
    /** Ultra-clear center of the studio vignette. */
    val StudioCenter = Color(0xFFFFFFFF)

    /** Soft transition ring between center and vignette edge. */
    val StudioMid = Color(0xFFF1F5F9)

    /** Vignette edge/base tone framing the canvas. */
    val StudioEdge = Color(0xFFDDE4EC)

    /** Pure floating-card surface. */
    val CardSurface = Color(0xFFFFFFFF)

    /** Hairline card border. */
    val CardBorder = Color(0xFFE2E8F0)

    /** Primary text / titles — near-black slate for max contrast. */
    val TextPrimary = Color(0xFF0F172A)

    /** Subtitles, labels and secondary copy. */
    val TextSecondary = Color(0xFF475569)

    /** Elevation hierarchy: informative < selectable < primary CTA. */
    val ElevationInfo = 4.dp
    val ElevationCard = 8.dp
    val ElevationCta = 12.dp
}

/**
 * Native "Light Studio Vignette" backdrop: a pure-Compose radial gradient
 * that reads like a photo-studio sweep — bright center falling off to a cool
 * slate edge. No bitmaps, no assets; the brush is instantiated once per
 * composition via [remember] so recompositions never re-allocate it.
 */
@Composable
fun TempoxStudioBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    // Instanciação otimizada do gradiente radial de estúdio
    val studioBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                TempoxThemeColors.StudioCenter, // Centro ultra claro
                TempoxThemeColors.StudioMid, // Transição suave
                TempoxThemeColors.StudioEdge, // Vinheta das bordas/base
            ),
            radius = 1800f,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = studioBrush),
    ) {
        content()
    }
}
