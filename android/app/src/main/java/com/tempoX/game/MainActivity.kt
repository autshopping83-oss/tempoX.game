package com.tempoX.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tempoX.game.audio.SoundManager
import com.tempoX.game.game.GameEngine
import com.tempoX.game.game.LangMode
import com.tempoX.game.game.LanguageManager
import com.tempoX.game.game.MatchSummary
import com.tempoX.game.game.PlayerStats
import com.tempoX.game.game.StatsRepository
import com.tempoX.game.ui.screens.GameScreen
import com.tempoX.game.ui.screens.HomeScreen
import com.tempoX.game.ui.screens.ResultScreen
import com.tempoX.game.ui.screens.SplashScreen
import com.tempoX.game.ui.theme.TemproxTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        SoundManager.init(applicationContext)
        setContent {
            TemproxTheme {
                AppRootHost()
            }
        }
    }
}

/** Result bundle handed over when the 60s run ends. */
private data class FinishedMatch(
    val summary: MatchSummary,
    val isRecord: Boolean,
    val unlockedIds: List<String>,
)

@Composable
fun AppRootHost() {
    val sysContext = LocalContext.current
    var langMode by remember { mutableStateOf(LanguageManager.load(sysContext)) }
    val localized = remember(langMode) { LanguageManager.wrap(sysContext, langMode) }

    CompositionLocalProvider(LocalContext provides localized) {
        AppRoot(
            langMode = langMode,
            onLanguageChange = { next ->
                LanguageManager.save(sysContext, next)
                langMode = next
                SoundManager.play(SoundManager.Sfx.CLICK)
            },
        )
    }
}

@Composable
fun AppRoot(
    langMode: LangMode,
    onLanguageChange: (LangMode) -> Unit,
) {
    val context = LocalContext.current
    val statsRepo = remember { StatsRepository(context) }

    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1900)
        showSplash = false
    }

    var playing by remember { mutableStateOf(false) }
    var seedText by remember { mutableStateOf("") }
    var finished by remember { mutableStateOf<FinishedMatch?>(null) }

    Box(Modifier.fillMaxSize()) {
        when {
            showSplash -> SplashScreen()

            playing -> GameScreen(
                seedText = seedText,
                onFinish = { summary, engine ->
                    val before = statsRepo.load()
                    val isRecord = summary.score > before.highScore && summary.score > 0
                    val unlocked = statsRepo.commitMatch(summary, engine)
                    finished = FinishedMatch(summary, isRecord, unlocked)
                    playing = false
                },
                onQuit = {
                    playing = false
                },
            )

            finished != null -> {
                val f = finished!!
                ResultScreen(
                    summary = f.summary,
                    isRecord = f.isRecord,
                    doubledAlready = false,
                    unlockedIds = f.unlockedIds,
                    onPlayAgain = { finished = null; playing = true },
                    onMenu = { finished = null },
                )
            }

            else -> HomeScreen(
                stats = statsRepo.load(),
                language = langMode,
                onLanguageChange = onLanguageChange,
                onStartMatch = { seed ->
                    seedText = seed
                    finished = null
                    playing = true
                },
            )
        }
    }
}
