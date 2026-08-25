package cloud.bizflow.tempox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.bizflow.tempox.audio.SoundManager
import cloud.bizflow.tempox.ui.theme.TemproxColors

/** Home tabs. */
enum class HomeTab { PLAY, STATS, TROPHIES }

/**
 * Floating pill bottom navigation — dark glassy bar for Cyber-Arcade theme.
 * Active tab gets the brand gradient; inactive tabs use muted grey text.
 */
@Composable
fun BottomNavigation(
    current: HomeTab,
    onSelect: (HomeTab) -> Unit,
    labels: Map<HomeTab, String>,
    icons: Map<HomeTab, String> = mapOf(HomeTab.PLAY to "🎮", HomeTab.STATS to "📊", HomeTab.TROPHIES to "🏆"),
    dark: Boolean = false,
) {
    val bgColor = if (dark) Color(0xE6140B27) else Color(0xF7FFFFFF)
    val borderColor = if (dark) Color(0xFF3B2D54) else TemproxColors.BorderLight
    val inactiveText = if (dark) Color(0xFF64748B) else Color(0xFF94A3B8)
    Row(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(28.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(28.dp))
            .height(68.dp)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeTab.entries.forEach { tab ->
            val selected = tab == current
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (selected) Brush.horizontalGradient(listOf(TemproxColors.Primary, Color(0xFF8B5CF6)))
                        else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { if (!selected) { SoundManager.play(SoundManager.Sfx.CLICK); onSelect(tab) } },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(icons[tab] ?: "", fontWeight = FontWeight.Bold)
                Text(
                    labels[tab] ?: "",
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (selected) Color.White else inactiveText,
                    ),
                )
            }
        }
    }
}
