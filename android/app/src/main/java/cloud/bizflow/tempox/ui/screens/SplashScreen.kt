package cloud.bizflow.tempox.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.bizflow.tempox.R
import cloud.bizflow.tempox.ui.theme.TemproxColors
import cloud.bizflow.tempox.ui.theme.TemproxType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Branded launch animation:
 *  1. TEMPOX logo pops in with a bouncy zoom + fade
 *  2. gentle idle pulse while the app warms up
 *  3. tagline "THINK FAST. REACT FASTER." slides up and glows in
 */
@Composable
fun SplashScreen() {
    // Entrance
    val logoAlpha = remember { Animatable(0f) }
    val logoZoom = remember { Animatable(0.55f) }
    val tagAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(420)) }
        launch {
            delay(380)
            tagAlpha.animateTo(1f, tween(520))
        }
        logoZoom.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    // Idle pulse (multiplies the entrance zoom)
    val transition = rememberInfiniteTransition(label = "splash")
    val pulse by transition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )
    val glow by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )

    Box(
        Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        cloud.bizflow.tempox.ui.components.AnimatedBackground(Modifier.fillMaxSize())
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center),
        ) {
            Image(
                painter = painterResource(R.drawable.temprox_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .height(200.dp)
                    .graphicsLayer {
                        scaleX = logoZoom.value * pulse
                        scaleY = logoZoom.value * pulse
                        alpha = logoAlpha.value
                        shadowElevation = 24f * logoAlpha.value
                    },
            )
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.tagline),
                style = TemproxType.micro.copy(color = TemproxColors.Muted, letterSpacing = 3.sp),
                modifier = Modifier
                    .offset(y = ((1f - tagAlpha.value) * 14).dp)
                    .alpha(tagAlpha.value * glow),
            )
        }
    }
}
