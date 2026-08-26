package cloud.bizflow.tempox.monetization

import android.util.Log
import cloud.bizflow.tempox.BuildConfig

object AdConstants {

    /**
     * TRAVA MESTRE DE SEGURANÇA (MASTER SAFETY SWITCH)
     *
     * [false] -> Mantém o app em MODO DE TESTE (Seguro para desenvolvimento, beta fechado e aprovação na Play Store).
     * [true]  -> Ativar APENAS quando o jogo for aprovado e publicado oficialmente na Play Store.
     */
    private const val IS_STORE_PUBLISHED = false

    // ------------------------------------------------------------------------
    // 1. IDs OFICIAIS DE TESTE DA GOOGLE (Ativos enquanto IS_STORE_PUBLISHED = false)
    // ------------------------------------------------------------------------
    private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    // ------------------------------------------------------------------------
    // 2. IDs REAIS DE PRODUÇÃO ADMOB (PRESERVADOS NO CÓDIGO)
    // ------------------------------------------------------------------------
    const val PROD_APP_ID = "ca-app-pub-5925121782414544~6622937999"
    private const val PROD_BANNER_ID = "ca-app-pub-5925121782414544/1710734119"
    private const val PROD_INTERSTITIAL_ID = "ca-app-pub-5925121782414544/1327590737"
    private const val PROD_REWARDED_ID = "ca-app-pub-5925121782414544/8838037793"

    // ------------------------------------------------------------------------
    // 3. LÓGICA DE DECISÃO DE AMBIENTE
    // ------------------------------------------------------------------------
    private val isProductionActive: Boolean
        get() {
            val active = IS_STORE_PUBLISHED && !BuildConfig.DEBUG
            if (active) {
                Log.i("AdMobConfig", "ADMOB STATUS: *** MODO PRODUÇÃO REAL ATIVO ***")
            } else {
                Log.w("AdMobConfig", "ADMOB STATUS: Rodando em MODO DE TESTE (Trava de Segurança Ativa)")
            }
            return active
        }

    // Propriedades consumidas pelas telas do aplicativo
    val BANNER_AD_UNIT_ID: String
        get() = if (isProductionActive) PROD_BANNER_ID else TEST_BANNER_ID

    val INTERSTITIAL_AD_UNIT_ID: String
        get() = if (isProductionActive) PROD_INTERSTITIAL_ID else TEST_INTERSTITIAL_ID

    val REWARDED_AD_UNIT_ID: String
        get() = if (isProductionActive) PROD_REWARDED_ID else TEST_REWARDED_ID
}
