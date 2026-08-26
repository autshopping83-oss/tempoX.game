package cloud.bizflow.tempox.monetization

import cloud.bizflow.tempox.BuildConfig

/**
 * Centralizes every AdMob identifier so no screen ever hardcodes an ad unit.
 *
 * - **Debug builds** receive the official Google TEST IDs (always safe to load).
 * - **Release builds** receive the real production IDs from this app's AdMob
 *   account.  Violating this rule (e.g. loading production ads during dev)
 *   triggers permanent account suspension.
 */
object AdConstants {
    // IDs Oficiais de Teste da Google (Usados em DEBUG para evitar banimento)
    private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    // IDs Reais de Produção
    private const val PROD_BANNER_ID = "ca-app-pub-5925121782414544/1710734119"
    private const val PROD_INTERSTITIAL_ID = "ca-app-pub-5925121782414544/1327590737"
    private const val PROD_REWARDED_ID = "ca-app-pub-5925121782414544/8838037793"

    val BANNER_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER_ID else PROD_BANNER_ID

    val INTERSTITIAL_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_ID else PROD_INTERSTITIAL_ID

    val REWARDED_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_REWARDED_ID else PROD_REWARDED_ID
}
