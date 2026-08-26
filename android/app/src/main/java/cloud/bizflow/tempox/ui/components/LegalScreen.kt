package cloud.bizflow.tempox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.bizflow.tempox.R
import cloud.bizflow.tempox.audio.SoundManager
import cloud.bizflow.tempox.ui.theme.TemproxType

/**
 * Fullscreen native legal reader ("Floating Light Studio" theme).
 *
 * Layout contract:
 *  - Header (title + updated stamp) is pinned at the top.
 *  - Body scrolls independently in the middle.
 *  - The close button lives OUTSIDE the scroll area, pinned to the bottom,
 *    so the user never has to read the whole document to leave.
 */
@Composable
fun LegalScreen(
    title: String,
    updated: String,
    body: String,
    onDismiss: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xB30F172A))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(TempoxThemeColors.CardSurface)
            .border(1.dp, TempoxThemeColors.CardBorder, RoundedCornerShape(24.dp)),
    ) {
        // ── Pinned header ────────────────────────────────────────────────────
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                title,
                style = TemproxType.title.copy(color = TempoxThemeColors.TextPrimary),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                updated,
                style = TemproxType.micro.copy(color = TempoxThemeColors.TextSecondary),
            )
        }

        // ── Scrollable document body ────────────────────────────────────────
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                body,
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = TempoxThemeColors.TextPrimary,
                ),
            )
            Spacer(Modifier.height(20.dp))
        }

        // ── Pinned footer action ────────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .background(TempoxThemeColors.StudioMid.copy(alpha = 0.6f))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrimaryButton(
                text = stringResource(R.string.legal_button_close),
                onClick = {
                    SoundManager.play(SoundManager.Sfx.CLICK)
                    onDismiss()
                },
            )
        }
    }
}
