package cloud.bizflow.tempox.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** TEMPOX brand palette (identical to the original design system). */
object TemproxColors {
    val Primary = Color(0xFF6D3DF5)
    val PrimaryDark = Color(0xFF4F24D0)
    val Accent = Color(0xFFFFC93D)
    val Background = Color(0xFFF8FAFC)
    val Surface = Color.White
    val Ink = Color(0xFF1E1B2E)
    val Muted = Color(0xFF64748B)

    val Success = Color(0xFF10B981)
    val Danger = Color(0xFFEF4444)
    val Warning = Color(0xFFF59E0B)
    val Info = Color(0xFF3B82F6)
    val Green = Color(0xFF22C55E)
    val Pink = Color(0xFFEC4899)

    // Light-surface lines (web slate-200 / slate-50 equivalents)
    val BorderLight = Color(0xFFE2E8F0)
    val BorderSofter = Color(0xFFF1F5F9)

    // Dark hero surfaces (home / gameplay backdrops)
    val NightA = Color(0xFF151032)
    val NightB = Color(0xFF0B081C)
    val CardDark = Color(0xFF231D45)

    fun challengeColor(type: cloud.bizflow.tempox.game.ChallengeType): Color = when (type) {
        cloud.bizflow.tempox.game.ChallengeType.MEMORY -> Pink
        cloud.bizflow.tempox.game.ChallengeType.REFLEX -> Warning
        cloud.bizflow.tempox.game.ChallengeType.MATH -> Info
        cloud.bizflow.tempox.game.ChallengeType.ATTENTION -> Green
    }
}

object TemproxShapes {
    val Card = RoundedCornerShape(20.dp)
    val Button = RoundedCornerShape(16.dp)
    val Chip = RoundedCornerShape(999.dp)
}

/** Brand typography — heavy weights for the arcade feel. */
object TemproxType {
    val display = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
    val titleLg = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.3).sp)
    val title = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
    val bodyBold = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
    val body = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val caption = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    val micro = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
    val score = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
}

@Composable
fun TemproxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = TemproxColors.Primary,
            secondary = TemproxColors.Accent,
            error = TemproxColors.Danger,
            background = TemproxColors.Background,
            surface = TemproxColors.Surface,
        ),
        content = content,
    )
}
