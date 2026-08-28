package cloud.bizflow.tempox

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cloud.bizflow.tempox.audio.SoundManager
import cloud.bizflow.tempox.game.BillingManager
import cloud.bizflow.tempox.game.BillingRepository
import cloud.bizflow.tempox.game.EconomyRepository
import cloud.bizflow.tempox.game.GameMode
import cloud.bizflow.tempox.game.LangMode
import cloud.bizflow.tempox.game.LanguageManager
import cloud.bizflow.tempox.game.MatchSummary
import cloud.bizflow.tempox.game.MockAdManager
import cloud.bizflow.tempox.game.PlayerStats
import cloud.bizflow.tempox.game.StatsRepository
import cloud.bizflow.tempox.ui.screens.GameScreen
import cloud.bizflow.tempox.ui.screens.HomeScreen
import cloud.bizflow.tempox.ui.screens.ResultScreen
import cloud.bizflow.tempox.monetization.MonetizationManager
import cloud.bizflow.tempox.ui.screens.SplashScreen
import cloud.bizflow.tempox.ui.theme.TemproxTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var billingManager: BillingManager
    private var isRecreation = false

    override fun attachBaseContext(newBase: Context) {
        // Apply the saved locale to the base context. This is the stable, official
        // approach: it works both on cold start AND after recreate() (both pass
        // through attachBaseContext with a new instance). It does NOT touch the
        // Activity itself, so LocalContext.current stays the Activity and the
        // Compose identifiers/stringResource resolve to the localized context.
        val langMode = LanguageManager.load(newBase)
        if (langMode != LangMode.SYSTEM) {
            val locale = when (langMode) {
                LangMode.PT_BR -> java.util.Locale.forLanguageTag("pt-BR")
                LangMode.PT_PT -> java.util.Locale.forLanguageTag("pt-PT")
                LangMode.EN -> java.util.Locale.ENGLISH
                else -> null
            }
            if (locale != null) {
                val config = Configuration(newBase.resources.configuration)
                config.setLocale(locale)
                config.setLocales(android.os.LocaleList(locale))
                super.attachBaseContext(newBase.createConfigurationContext(config))
                return
            }
        }
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        isRecreation = savedInstanceState != null
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SoundManager.init(applicationContext)
        // Async AdMob bootstrap — never blocks the UI thread.
        MonetizationManager.initialize(applicationContext)
        // Real Google Play Billing — must be started early and destroyed in onDestroy.
        billingManager = BillingManager(applicationContext)
        billingManager.startConnection()
        setContent {
            TemproxTheme {
                AppRootHost(billing = billingManager, skipSplash = isRecreation)
            }
        }
    }

    override fun onDestroy() {
        billingManager.endConnection()
        super.onDestroy()
    }
}

/** Result bundle handed over when the 60s run ends. */
private data class FinishedMatch(
    val summary: MatchSummary,
    val isRecord: Boolean,
    val unlockedIds: List<String>,
)

@Composable
fun AppRootHost(billing: BillingRepository, skipSplash: Boolean = false) {
    val sysContext = LocalContext.current
    var langMode by remember { mutableStateOf(LanguageManager.load(sysContext)) }

    AppRoot(
        billing = billing,
        langMode = langMode,
        skipSplash = skipSplash,
        onLanguageChange = { next ->
            LanguageManager.save(sysContext, next)
            langMode = next
            SoundManager.play(SoundManager.Sfx.CLICK)
            (sysContext as? Activity)?.recreate()
        },
    )
}

@Composable
fun AppRoot(
    billing: BillingRepository,
    langMode: LangMode,
    skipSplash: Boolean = false,
    onLanguageChange: (LangMode) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val statsRepo = remember { StatsRepository(context) }
    val econRepo = remember { EconomyRepository(context) }
    remember { MockAdManager.billing = billing; true }
    val adFree by billing.isAdFreeUser.collectAsState()
    var economy by remember { mutableStateOf(econRepo.load()) }

    var showSplash by remember { mutableStateOf(!skipSplash) }
    LaunchedEffect(Unit) {
        billing.restorePurchases()
        if (!skipSplash) delay(2400)
        showSplash = false
        // Preload ads once the splash is done and Activity is available.
        if (activity != null && !adFree) MockAdManager.preloadAds(activity)
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
                coins = economy.coins,
                onSpendCoins = { amount ->
                    val ok = econRepo.trySpendCoins(amount)
                    if (ok) economy = econRepo.load()
                    ok
                },
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
                    onCoinsDoubled = { bonus ->
                        if (bonus > 0) {
                            econRepo.addCoins(bonus)
                            economy = econRepo.load()
                        }
                    },
                )
            }

            else -> HomeScreen(
                stats = statsRepo.load(),
                economy = economy,
                billing = billing,
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
