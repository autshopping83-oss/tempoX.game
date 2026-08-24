package cloud.bizflow.tempox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cloud.bizflow.tempox.audio.SoundManager
import cloud.bizflow.tempox.game.BillingRepository
import cloud.bizflow.tempox.game.EconomyRepository
import cloud.bizflow.tempox.game.GameEngine
import cloud.bizflow.tempox.game.GameMode
import cloud.bizflow.tempox.game.LangMode
import cloud.bizflow.tempox.game.LanguageManager
import cloud.bizflow.tempox.game.MatchSummary
import cloud.bizflow.tempox.game.MockAdManager
import cloud.bizflow.tempox.game.MockBillingRepositoryImpl
import cloud.bizflow.tempox.game.PlayerStats
import cloud.bizflow.tempox.game.StatsRepository
import cloud.bizflow.tempox.ui.screens.GameScreen
import cloud.bizflow.tempox.ui.screens.HomeScreen
import cloud.bizflow.tempox.ui.screens.ResultScreen
import cloud.bizflow.tempox.ui.screens.SplashScreen
import cloud.bizflow.tempox.ui.theme.TemproxTheme
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
    val econRepo = remember { EconomyRepository(context) }
    val billingRepo = remember { MockBillingRepositoryImpl(context) }
    remember { MockAdManager.billing = billingRepo as BillingRepository; true } // wire once
    val adFree by billingRepo.isAdFreeUser.collectAsState()
    var economy by remember { mutableStateOf(econRepo.load()) }

    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        billingRepo.restorePurchases() // Play compliance: revalidate on every launch
        delay(2400)
        showSplash = false
    }

    var playing by remember { mutableStateOf(false) }
    var matchMode by remember { mutableStateOf(GameMode.ARCADE) }
    var seedText by remember { mutableStateOf("") }
    var finished by remember { mutableStateOf<FinishedMatch?>(null) }

    Box(Modifier.fillMaxSize()) {
        when {
            showSplash -> SplashScreen()

            playing -> GameScreen(
                seedText = seedText,
                mode = matchMode,
                vipInstant = adFree,
                onFinish = { summary, engine ->
                    val before = statsRepo.load()
                    val isRecord = summary.score > before.highScore && summary.score > 0
                    val unlocked = statsRepo.commitMatch(summary, engine)
                    econRepo.addCoins(summary.coinsEarned)
                    economy = econRepo.load()
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
                    vipInstant = adFree,
                    onPlayAgain = { finished = null; playing = true },
                    onMenu = { finished = null },
                )
            }

            else -> HomeScreen(
                stats = statsRepo.load(),
                economy = economy,
                billing = billingRepo,
                language = langMode,
                onLanguageChange = onLanguageChange,
                onStartMatch = { mode, seed ->
                    matchMode = mode
                    seedText = seed
                    finished = null
                    playing = true
                },
                onUnlockWithCoins = { mode ->
                    econRepo.trySpendCoins(EconomyRepository.UNLOCK_COST).also { ok ->
                        if (ok) {
                            econRepo.unlockMode(mode)
                            economy = econRepo.load()
                        }
                    }
                },
                onUnlockWithAd = { mode ->
                    econRepo.unlockMode(mode)
                    economy = econRepo.load()
                },
            )
        }
    }
}
