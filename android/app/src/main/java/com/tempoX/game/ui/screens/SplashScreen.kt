package com.tempoX.game.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tempoX.game.R
import com.tempoX.game.ui.components.TemproxLogo
import com.tempoX.game.ui.theme.TemproxColors
import com.tempoX.game.ui.theme.TemproxType
import androidx.compose.ui.unit.sp

/** Branded launch screen: pulsing X block + wordmark + tagline. */
@Composable
fun SplashScreen() {
    val transition = rememberInfiniteTransition(label = "splash")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "pulse",
    )
    val glow by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(TemproxColors.NightA, TemproxColors.NightB))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.scale(pulse)) {
                Box(
                    Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(listOf(TemproxColors.Primary, Color(0xFF8B5CF6)))
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("X", style = TemproxType.display.copy(fontSize = 46.sp, color = Color.White))
                }
            }
            Spacer(Modifier.height(18.dp))
            TemproxLogo(heightText = 34)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.tagline),
                style = TemproxType.micro.copy(color = Color(0xFF8B86A3), letterSpacing = 2.sp),
                modifier = Modifier.alpha(glow),
            )
        }
    }
}
